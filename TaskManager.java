package strms.manager;

import strms.model.Task;
import strms.model.TaskHistoryEntry;
import strms.model.User;
import strms.model.Engineer;
import strms.enums.TaskStatus;
import strms.enums.PriorityLevel;
import strms.enums.NotificationType;
import strms.exception.*;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * TaskManager — Central controller of the STRMS system.
 *
 * Responsibilities:
 *  - Enforce all business rules (creation, deletion, assignment, state transitions).
 *  - Manage task dependencies and circular dependency detection (DFS).
 *  - Maintain task history for full traceability.
 *  - Handle persistence (save / load tasks to/from file).
 *  - Drive the priority queue for task scheduling.
 *  - Send notifications via NotificationManager.
 *
 * Data structures used:
 *  - HashMap<String, Task>       : fast O(1) lookup of any task by its unique ID.
 *  - HashMap<String, User>       : fast O(1) lookup of any user by their ID.
 *  - HashSet<Task>               : duplicate-free set of tasks currently IN_PROGRESS.
 *  - PriorityQueue<Task>         : always yields the highest-priority ready task first.
 *
 * All public mutating methods validate user permissions before executing the operation.
 */
public class TaskManager {

    // =========================================================================
    // Fields
    // =========================================================================

    /** Master registry — every task in the system, keyed by task ID. */
    private final HashMap<String, Task> tasks;

    /** Master registry — every registered user, keyed by user ID. */
    private final HashMap<String, User> users;

    /**
     * Set of tasks that are currently IN_PROGRESS.
     * HashSet guarantees no duplicates and O(1) membership checks.
     */
    private final HashSet<Task> inProgressTasks;

    /**
     * Priority queue of tasks whose dependencies are all DONE and that are
     * therefore ready to be picked up by an engineer.
     * Tasks implement Comparable<Task> so the queue orders them by priority
     * (CRITICAL > HIGH > MEDIUM > LOW).
     */
    private final PriorityQueue<Task> readyQueue;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Creates an empty TaskManager.
     * The readyQueue uses the natural ordering defined by Task#compareTo,
     * which places higher-priority tasks at the head of the queue.
     */
    public TaskManager() {
        this.tasks         = new HashMap<>();
        this.users         = new HashMap<>();
        this.inProgressTasks = new HashSet<>();
        this.readyQueue    = new PriorityQueue<>();
    }

    // =========================================================================
    // User management helpers
    // =========================================================================

    /**
     * Registers a user in the system so that the TaskManager can later look up
     * their role when validating permissions.
     *
     * @param user the user to register (must not be null)
     */
    public void registerUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null.");
        }
        users.put(user.getId(), user);
    }

    /**
     * Returns the user registered under the given ID, or null if not found.
     *
     * @param userId the user's unique identifier
     * @return the User, or null
     */
    public User findUser(String userId) {
        return users.get(userId);
    }

    // =========================================================================
    // Core task operations
    // =========================================================================

    /**
     * Adds a new task to the system.
     *
     * Rules enforced:
     *  - The requesting user must have the canCreateTask() permission.
     *  - The task ID must be unique; duplicates are rejected.
     *  - On success the task is placed in the readyQueue (it has no dependencies yet).
     *  - A history entry "Task created by <user>" is appended.
     *
     * @param task    the task to add
     * @param creator the user requesting the creation
     * @throws InvalidRoleException    if the user lacks creation permission
     * @throws DuplicateTaskException  if a task with the same ID already exists
     */
    public void addTask(Task task, User creator)
            throws InvalidRoleException, DuplicateTaskException {

        // 1. Permission check
        if (!creator.canCreateTask()) {
            throw new InvalidRoleException(
                "User '" + creator.getName() + "' (role: " + creator.getRole()
                + ") does not have permission to create tasks.");
        }

        // 2. Duplicate ID check
        if (tasks.containsKey(task.getId())) {
            throw new DuplicateTaskException(
                "A task with ID '" + task.getId() + "' already exists in the system.");
        }

        // 3. Persist in the master map
        tasks.put(task.getId(), task);

        // 4. A brand-new task has no dependencies → immediately ready
        readyQueue.offer(task);

        // 5. History
        task.addHistoryEntry(new TaskHistoryEntry(
            "Task created.",
            creator,
            LocalDateTime.now()
        ));

        // 6. Notification
        NotificationManager.send(
            NotificationType.CONSOLE,
            "Task [" + task.getId() + "] '" + task.getTitle()
            + "' created by " + creator.getName() + "."
        );
    }

    /**
     * Removes a task from the system.
     *
     * Rules enforced:
     *  - The requesting user must have the canDeleteTask() permission.
     *  - The task must exist.
     *  - Removes the task from all internal structures.
     *  - All other tasks that listed this task as a dependency are updated
     *    (the reference is cleaned up).
     *
     * @param taskId  the ID of the task to delete
     * @param deleter the user requesting the deletion
     * @throws InvalidRoleException  if the user lacks deletion permission
     * @throws TaskNotFoundException if no task with the given ID exists
     */
    public void deleteTask(String taskId, User deleter)
            throws InvalidRoleException, TaskNotFoundException {

        // 1. Permission check
        if (!deleter.canDeleteTask()) {
            throw new InvalidRoleException(
                "User '" + deleter.getName() + "' does not have permission to delete tasks.");
        }

        // 2. Existence check
        Task task = requireTask(taskId);

        // 3. Remove from all structures
        tasks.remove(taskId);
        inProgressTasks.remove(task);
        readyQueue.remove(task);

        // 4. Clean up reverse references in other tasks
        for (Task other : tasks.values()) {
            other.removeDependency(task);
        }

        // 5. Notification
        NotificationManager.send(
            NotificationType.CONSOLE,
            "Task [" + taskId + "] deleted by " + deleter.getName() + "."
        );
    }

    /**
     * Assigns a task to an engineer.
     *
     * Rules enforced:
     *  - The requesting user must have the canAssignTask() permission.
     *  - Both the task and the target engineer must exist.
     *  - The task must not already be assigned.
     *  - If all dependencies are DONE the task transitions to IN_PROGRESS and
     *    is moved from the ready queue to the in-progress set.
     *  - If dependencies are not satisfied the task remains BLOCKED.
     *
     * @param taskId     the ID of the task to assign
     * @param engineerId the ID of the engineer who will execute the task
     * @param assigner   the user requesting the assignment
     * @throws InvalidRoleException           if the assigner lacks permission
     * @throws TaskNotFoundException          if the task or engineer does not exist
     * @throws DependencyNotCompletedException if dependencies are not satisfied
     *                                         (task becomes BLOCKED, exception informs the caller)
     */
    public void assignTask(String taskId, String engineerId, User assigner)
            throws InvalidRoleException, TaskNotFoundException,
                   DependencyNotCompletedException {

        // 1. Permission check
        if (!assigner.canAssignTask()) {
            throw new InvalidRoleException(
                "User '" + assigner.getName() + "' does not have permission to assign tasks.");
        }

        // 2. Retrieve task and engineer
        Task task         = requireTask(taskId);
        User engineerUser = users.get(engineerId);
        if (engineerUser == null) {
            throw new TaskNotFoundException(
                "Engineer with ID '" + engineerId + "' not found.");
        }
        if (!(engineerUser instanceof Engineer)) {
            throw new InvalidRoleException(
                "User '" + engineerUser.getName() + "' is not an Engineer and cannot be assigned tasks.");
        }
        Engineer engineer = (Engineer) engineerUser;

        // 3. Already assigned?
        if (task.getAssignedEngineer() != null) {
            throw new InvalidTaskStateException(
                "Task [" + taskId + "] is already assigned to '"
                + task.getAssignedEngineer().getName() + "'.");
        }

        // 4. Assign
        task.setAssignedEngineer(engineer);

        // 5. Check if dependencies allow immediate activation
        if (areDependenciesDone(task)) {
            activateTask(task);
        } else {
            // Keep / set BLOCKED
            task.updateStatus(TaskStatus.BLOCKED);
            task.addHistoryEntry(new TaskHistoryEntry(
                "Task assigned to " + engineer.getName()
                + " but remains BLOCKED — unresolved dependencies.",
                assigner,
                LocalDateTime.now()
            ));
            throw new DependencyNotCompletedException(
                "Task [" + taskId + "] has unresolved dependencies and is now BLOCKED.");
        }

        // 6. History (happy path)
        task.addHistoryEntry(new TaskHistoryEntry(
            "Task assigned to " + engineer.getName() + " and set to IN_PROGRESS.",
            assigner,
            LocalDateTime.now()
        ));

        // 7. Notification
        NotificationManager.send(
            NotificationType.CONSOLE,
            "Task [" + taskId + "] assigned to " + engineer.getName() + "."
        );
    }

    /**
     * Marks a task as DONE.
     *
     * Rules enforced:
     *  - Only the assigned engineer may complete a task.
     *  - The task must exist and be IN_PROGRESS.
     *  - On completion the task is removed from inProgressTasks and readyQueue.
     *  - Any task whose only missing dependency was this task is automatically
     *    unblocked and added to the ready queue.
     *
     * @param taskId   the ID of the task to complete
     * @param engineer the engineer requesting completion
     * @throws InvalidRoleException    if the caller is not the assigned engineer
     * @throws TaskNotFoundException   if the task does not exist
     * @throws InvalidTaskStateException if the task is not IN_PROGRESS
     */
    public void completeTask(String taskId, User engineer)
            throws InvalidRoleException, TaskNotFoundException,
                   InvalidTaskStateException {

        // 1. Retrieve task
        Task task = requireTask(taskId);

        // 2. Check the caller is the assigned engineer
        if (task.getAssignedEngineer() == null
                || !task.getAssignedEngineer().getId().equals(engineer.getId())) {
            throw new InvalidRoleException(
                "Only the assigned engineer can complete task [" + taskId + "].");
        }

        // 3. Must be IN_PROGRESS
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new InvalidTaskStateException(
                "Task [" + taskId + "] is not IN_PROGRESS and cannot be completed. "
                + "Current status: " + task.getStatus());
        }

        // 4. Transition
        task.markAsDone();
        inProgressTasks.remove(task);
        readyQueue.remove(task);

        // 5. History
        task.addHistoryEntry(new TaskHistoryEntry(
            "Task completed and marked as DONE.",
            engineer,
            LocalDateTime.now()
        ));

        // 6. Notification
        NotificationManager.send(
            NotificationType.CONSOLE,
            "Task [" + taskId + "] '" + task.getTitle() + "' marked DONE by "
            + engineer.getName() + "."
        );

        // 7. Unblock dependent tasks whose prerequisites are now all DONE
        unlockDependentTasks(task);
    }

    /**
     * Updates one or more attributes of an existing task.
     *
     * Rules enforced:
     *  - The user must have canUpdateTask() permission.
     *  - The task must exist.
     *  - If the new status implies a forbidden transition an exception is thrown.
     *  - All changes are logged in the task history.
     *
     * @param taskId     the ID of the task to update
     * @param newStatus  the desired new status (null = no change)
     * @param newTitle   the new title (null = no change)
     * @param newDesc    the new description (null = no change)
     * @param newPrio    the new priority (null = no change)
     * @param updater    the user requesting the update
     * @throws InvalidRoleException      if the user lacks permission
     * @throws TaskNotFoundException     if the task does not exist
     * @throws InvalidTaskStateException if the status transition is forbidden
     */
    public void updateTask(String taskId,
                           TaskStatus   newStatus,
                           String       newTitle,
                           String       newDesc,
                           PriorityLevel newPrio,
                           User          updater)
            throws InvalidRoleException, TaskNotFoundException,
                   InvalidTaskStateException {

        if (!updater.canUpdateTask()) {
            throw new InvalidRoleException(
                "User '" + updater.getName() + "' does not have permission to update tasks.");
        }

        Task task = requireTask(taskId);

        StringBuilder log = new StringBuilder("Task updated by " + updater.getName() + ": ");

        // Status transition validation
        if (newStatus != null && newStatus != task.getStatus()) {
            validateStatusTransition(task.getStatus(), newStatus, taskId);
            task.updateStatus(newStatus);
            log.append("status ").append(task.getStatus()).append(" → ").append(newStatus).append("; ");

            // Keep in-progress set consistent
            if (newStatus == TaskStatus.IN_PROGRESS) {
                inProgressTasks.add(task);
            } else {
                inProgressTasks.remove(task);
            }
        }

        if (newTitle != null && !newTitle.isBlank()) {
            task.setTitle(newTitle);
            log.append("title updated; ");
        }

        if (newDesc != null && !newDesc.isBlank()) {
            task.updateDescription(newDesc);
            log.append("description updated; ");
        }

        if (newPrio != null && newPrio != task.getPriority()) {
            task.changePriority(newPrio);
            log.append("priority → ").append(newPrio).append("; ");
            // Re-enqueue so the priority queue reflects the new priority
            if (readyQueue.remove(task)) {
                readyQueue.offer(task);
            }
        }

        task.addHistoryEntry(new TaskHistoryEntry(log.toString(), updater, LocalDateTime.now()));
    }

    // =========================================================================
    // Dependency management
    // =========================================================================

    /**
     * Adds a dependency: {@code dependentTask} depends on {@code prerequisiteTask}.
     *
     * Rules enforced:
     *  - Both tasks must exist.
     *  - A task cannot depend on itself.
     *  - The new dependency must not introduce a cycle (checked via DFS).
     *  - If the prerequisite is not yet DONE the dependent task is set BLOCKED.
     *  - The dependency is recorded in history.
     *
     * @param dependentTaskId   the task that will depend on the other
     * @param prerequisiteTaskId the task that must be completed first
     * @param requester          the user requesting the operation
     * @throws TaskNotFoundException        if either task ID is unknown
     * @throws CircularDependencyException  if adding the dependency would create a cycle
     * @throws InvalidRoleException         if the user lacks permission
     */
    public void addDependency(String dependentTaskId,
                              String prerequisiteTaskId,
                              User   requester)
            throws TaskNotFoundException, CircularDependencyException,
                   InvalidRoleException {

        if (!requester.canManageDependencies()) {
            throw new InvalidRoleException(
                "User '" + requester.getName() + "' cannot manage task dependencies.");
        }

        Task dependent   = requireTask(dependentTaskId);
        Task prerequisite = requireTask(prerequisiteTaskId);

        // Self-dependency guard
        if (dependentTaskId.equals(prerequisiteTaskId)) {
            throw new CircularDependencyException(
                "A task cannot depend on itself (ID: " + dependentTaskId + ").");
        }

        // Circular dependency detection — DFS starting from prerequisite
        // We check: if we can reach 'dependent' from 'prerequisite' through
        // existing edges, then adding this edge would close a cycle.
        if (detectCircularDependency(prerequisite, dependent, new HashSet<>())) {
            // Log the violation
            dependent.addHistoryEntry(new TaskHistoryEntry(
                "Attempted to add dependency on [" + prerequisiteTaskId
                + "] — rejected: would create a circular dependency.",
                requester,
                LocalDateTime.now()
            ));
            throw new CircularDependencyException(
                "Adding dependency [" + dependentTaskId + "] → [" + prerequisiteTaskId
                + "] would create a circular dependency. Operation rejected.");
        }

        // Safe to add
        dependent.addDependency(prerequisite);

        // If the prerequisite is not done, block the dependent task
        if (prerequisite.getStatus() != TaskStatus.DONE) {
            dependent.updateStatus(TaskStatus.BLOCKED);
            readyQueue.remove(dependent);
        }

        // History
        dependent.addHistoryEntry(new TaskHistoryEntry(
            "Dependency added: this task now depends on [" + prerequisiteTaskId + "].",
            requester,
            LocalDateTime.now()
        ));

        NotificationManager.send(
            NotificationType.CONSOLE,
            "Dependency added: [" + dependentTaskId + "] now depends on ["
            + prerequisiteTaskId + "]."
        );
    }

    /**
     * Removes a dependency between two tasks.
     *
     * After removal, if all remaining dependencies of the dependent task are
     * DONE (or it now has none), the task is re-evaluated and potentially unblocked.
     *
     * @param dependentTaskId   the task that currently depends on the other
     * @param prerequisiteTaskId the dependency to remove
     * @param requester          the user requesting the operation
     * @throws TaskNotFoundException if either task ID is unknown
     * @throws InvalidRoleException  if the user lacks permission
     */
    public void removeDependency(String dependentTaskId,
                                 String prerequisiteTaskId,
                                 User   requester)
            throws TaskNotFoundException, InvalidRoleException {

        if (!requester.canManageDependencies()) {
            throw new InvalidRoleException(
                "User '" + requester.getName() + "' cannot manage task dependencies.");
        }

        Task dependent    = requireTask(dependentTaskId);
        Task prerequisite = requireTask(prerequisiteTaskId);

        dependent.removeDependency(prerequisite);

        // Re-evaluate: maybe the task can now proceed
        if (areDependenciesDone(dependent) && dependent.getStatus() == TaskStatus.BLOCKED) {
            if (dependent.getAssignedEngineer() != null) {
                activateTask(dependent);
            } else {
                // Unblocked but not yet assigned — make it TODO so it can be queued
                dependent.updateStatus(TaskStatus.TODO);
                readyQueue.offer(dependent);
            }
        }

        dependent.addHistoryEntry(new TaskHistoryEntry(
            "Dependency on [" + prerequisiteTaskId + "] removed.",
            requester,
            LocalDateTime.now()
        ));
    }

    /**
     * Depth-First Search to detect whether {@code target} is reachable from
     * {@code current} by following existing dependency edges.
     *
     * <p>This is called <em>before</em> adding a new edge (current → newDep).
     * If {@code target} is reachable from {@code current}, adding the edge
     * would close a cycle.</p>
     *
     * @param current  node to start DFS from
     * @param target   node we are looking for
     * @param visited  set of already-visited task IDs (prevents revisiting)
     * @return {@code true} if a path from current to target exists
     */
    private boolean detectCircularDependency(Task current,
                                              Task target,
                                              HashSet<String> visited) {
        if (current.getId().equals(target.getId())) {
            return true; // Cycle found
        }
        visited.add(current.getId());

        for (Task dep : current.getDependencies()) {
            if (!visited.contains(dep.getId())) {
                if (detectCircularDependency(dep, target, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    // =========================================================================
    // Query & display methods
    // =========================================================================

    /**
     * Finds a task by its ID.
     *
     * @param taskId the unique task identifier
     * @return the Task if found
     * @throws TaskNotFoundException if the ID does not exist
     */
    public Task findTask(String taskId) throws TaskNotFoundException {
        return requireTask(taskId);
    }

    /**
     * Returns an unmodifiable view of all tasks in the system.
     *
     * @return collection of all tasks
     */
    public Collection<Task> getAllTasks() {
        return Collections.unmodifiableCollection(tasks.values());
    }

    /**
     * Prints all tasks that are currently IN_PROGRESS to the console.
     */
    public void printInProgressTasks() {
        System.out.println("=== Tasks IN PROGRESS ===");
        if (inProgressTasks.isEmpty()) {
            System.out.println("  (none)");
        } else {
            for (Task t : inProgressTasks) {
                t.displayTask();
            }
        }
        System.out.println("=========================");
    }

    /**
     * Peeks at the next highest-priority task in the ready queue without removing it.
     *
     * @return the next Task to process, or null if the queue is empty
     */
    public Task peekNextTask() {
        return readyQueue.peek();
    }

    /**
     * Polls (removes and returns) the next highest-priority task from the ready queue.
     *
     * @return the next Task to process, or null if the queue is empty
     */
    public Task pollNextTask() {
        return readyQueue.poll();
    }

    /**
     * Lists all prerequisite tasks of the specified task.
     *
     * @param taskId the task whose dependencies to list
     * @throws TaskNotFoundException if the task does not exist
     */
    public void listDependencies(String taskId) throws TaskNotFoundException {
        Task task = requireTask(taskId);
        List<Task> deps = task.getDependencies();
        System.out.println("=== Dependencies of [" + taskId + "] ===");
        if (deps.isEmpty()) {
            System.out.println("  (no dependencies)");
        } else {
            for (Task dep : deps) {
                System.out.printf("  [%s] %s — %s%n",
                    dep.getId(), dep.getTitle(), dep.getStatus());
            }
        }
    }

    // =========================================================================
    // Persistence
    // =========================================================================

    /**
     * Saves all tasks to the specified file.
     *
     * Each task is serialised as a single line in a simple CSV-like format:
     * {@code id|title|status|priority|category|deadline|assignedEngineerId}
     *
     * Dependencies are written in a separate section after a "---DEPS---" separator.
     * History entries are written after a "---HISTORY---" separator.
     *
     * @param filePath path to the output file
     * @throws FilePersistenceException if writing fails
     */
    public void saveTasksToFile(String filePath) throws FilePersistenceException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

            // --- Tasks section ---
            writer.write("---TASKS---");
            writer.newLine();
            for (Task task : tasks.values()) {
                writer.write(task.toFileString());
                writer.newLine();
            }

            // --- Dependencies section ---
            writer.write("---DEPS---");
            writer.newLine();
            for (Task task : tasks.values()) {
                for (Task dep : task.getDependencies()) {
                    writer.write(task.getId() + "|" + dep.getId());
                    writer.newLine();
                }
            }

            // --- History section ---
            writer.write("---HISTORY---");
            writer.newLine();
            for (Task task : tasks.values()) {
                for (TaskHistoryEntry entry : task.getHistory()) {
                    writer.write(task.getId() + "|" + entry.toFileString());
                    writer.newLine();
                }
            }

        } catch (IOException e) {
            throw new FilePersistenceException(
                "Failed to save tasks to '" + filePath + "': " + e.getMessage(), e);
        }
    }

    /**
     * Loads tasks from the specified file, restoring dependencies and history.
     *
     * <p>The file format is the same as produced by {@link #saveTasksToFile}.</p>
     *
     * @param filePath path to the input file
     * @throws FilePersistenceException if reading or parsing fails
     */
    public void loadTasksFromFile(String filePath) throws FilePersistenceException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            tasks.clear();
            inProgressTasks.clear();
            readyQueue.clear();

            String section = "";
            String line;

            while ((line = reader.readLine()) != null) {
                switch (line) {
                    case "---TASKS---":
                    case "---DEPS---":
                    case "---HISTORY---":
                        section = line;
                        continue;
                    default:
                        break;
                }
                if (line.isBlank()) continue;

                if ("---TASKS---".equals(section)) {
                    Task task = Task.fromFileString(line, users);
                    tasks.put(task.getId(), task);
                    // Restore into correct runtime structure
                    switch (task.getStatus()) {
                        case IN_PROGRESS:
                            inProgressTasks.add(task);
                            break;
                        case TODO:
                            readyQueue.offer(task);
                            break;
                        default:
                            break;
                    }

                } else if ("---DEPS---".equals(section)) {
                    String[] parts = line.split("\\|", 2);
                    if (parts.length == 2) {
                        Task dependent    = tasks.get(parts[0]);
                        Task prerequisite = tasks.get(parts[1]);
                        if (dependent != null && prerequisite != null) {
                            dependent.addDependency(prerequisite);
                        }
                    }

                } else if ("---HISTORY---".equals(section)) {
                    String[] parts = line.split("\\|", 2);
                    if (parts.length == 2) {
                        Task task = tasks.get(parts[0]);
                        if (task != null) {
                            task.addHistoryEntry(
                                TaskHistoryEntry.fromFileString(parts[1], users));
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new FilePersistenceException(
                "Failed to load tasks from '" + filePath + "': " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Looks up a task by ID and throws {@link TaskNotFoundException} if absent.
     *
     * @param taskId the task's unique identifier
     * @return the Task
     * @throws TaskNotFoundException if the ID is not registered
     */
    private Task requireTask(String taskId) throws TaskNotFoundException {
        Task task = tasks.get(taskId);
        if (task == null) {
            throw new TaskNotFoundException(
                "Task with ID '" + taskId + "' not found in the system.");
        }
        return task;
    }

    /**
     * Returns {@code true} if all dependencies of {@code task} are in DONE state
     * (or the task has no dependencies).
     *
     * @param task the task to check
     * @return true if all prerequisites are DONE
     */
    private boolean areDependenciesDone(Task task) {
        for (Task dep : task.getDependencies()) {
            if (dep.getStatus() != TaskStatus.DONE) {
                return false;
            }
        }
        return true;
    }

    /**
     * Transitions a task to IN_PROGRESS and updates internal structures.
     * Assumes caller has already verified that dependencies are satisfied.
     *
     * @param task the task to activate
     */
    private void activateTask(Task task) {
        task.updateStatus(TaskStatus.IN_PROGRESS);
        inProgressTasks.add(task);
        readyQueue.remove(task);
    }

    /**
     * After a task is completed, iterates over all tasks and re-evaluates those
     * whose dependencies included the just-completed task. Tasks that now have
     * all dependencies satisfied are moved to the ready queue (if not yet assigned)
     * or activated (if already assigned).
     *
     * @param completedTask the task that was just marked DONE
     */
    private void unlockDependentTasks(Task completedTask) {
        for (Task candidate : tasks.values()) {
            if (candidate.getStatus() != TaskStatus.BLOCKED) continue;
            if (!candidate.getDependencies().contains(completedTask)) continue;

            if (areDependenciesDone(candidate)) {
                if (candidate.getAssignedEngineer() != null) {
                    activateTask(candidate);
                    candidate.addHistoryEntry(new TaskHistoryEntry(
                        "All dependencies completed. Task automatically set to IN_PROGRESS.",
                        null,   // system-generated
                        LocalDateTime.now()
                    ));
                    NotificationManager.send(
                        NotificationType.CONSOLE,
                        "Task [" + candidate.getId() + "] unblocked and now IN_PROGRESS."
                    );
                } else {
                    candidate.updateStatus(TaskStatus.TODO);
                    readyQueue.offer(candidate);
                    NotificationManager.send(
                        NotificationType.CONSOLE,
                        "Task [" + candidate.getId() + "] unblocked — awaiting assignment."
                    );
                }
            }
        }
    }

    /**
     * Validates that a status transition is permitted by the state machine.
     *
     * Allowed transitions (as per spec):
     *  TODO        → BLOCKED, IN_PROGRESS
     *  BLOCKED     → IN_PROGRESS, TODO
     *  IN_PROGRESS → DONE
     *  DONE        → (terminal — no further transitions)
     *
     * @param current the current status of the task
     * @param next    the desired new status
     * @param taskId  used in the exception message
     * @throws InvalidTaskStateException if the transition is forbidden
     */
    private void validateStatusTransition(TaskStatus current,
                                           TaskStatus next,
                                           String     taskId)
            throws InvalidTaskStateException {

        boolean allowed = switch (current) {
            case TODO        -> next == TaskStatus.BLOCKED || next == TaskStatus.IN_PROGRESS;
            case BLOCKED     -> next == TaskStatus.IN_PROGRESS || next == TaskStatus.TODO;
            case IN_PROGRESS -> next == TaskStatus.DONE;
            case DONE        -> false; // terminal state
        };

        if (!allowed) {
            throw new InvalidTaskStateException(
                "Invalid state transition for task [" + taskId + "]: "
                + current + " → " + next + " is not permitted.");
        }
    }
}
