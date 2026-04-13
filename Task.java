import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Task {
    private String taskId;
    private String Title;
    private String description;
    private TaskStatus status;
    private TaskCategory category;
    private LocalDate deadline;
    private PriorityLevel priorityLevel;
    private Engineer assignedEngineer;
    private List<Task> dependencies;
    private List<TaskHistoryEntry> history;

    
    public Task() {
        this.dependencies = new ArrayList<>();
        this.history = new ArrayList<>();
    }

    
    public Task(String taskId, String Title, String description, TaskStatus status, TaskCategory category, LocalDate deadline, PriorityLevel priorityLevel, Engineer assignedEngineer) {
        this.taskId = taskId;
        this.Title = Title;
        this.description = description;
        this.status = status;
        this.category = category;
        this.deadline = deadline;
        this.priorityLevel = priorityLevel;
        this.assignedEngineer = assignedEngineer;
        this.dependencies = new ArrayList<>();
        this.history = new ArrayList<>();
    }

    public void updateStatus(TaskStatus status, User user) {
        this.status = status;
        addHistoryEntry("Status updated to " + status, user);
    }

    public void markAsDone(User user) {
        this.status = TaskStatus.DONE;
        addHistoryEntry("Task marked as done", user);
    }

    public void addHistoryEntry(String action, User user) {
        TaskHistoryEntry entry = new TaskHistoryEntry(taskId, action, user, LocalDate.now());
        this.history.add(entry);
    }

    public void addDependency(Task dependency) {
        if (!this.dependencies.contains(dependency)) {
            this.dependencies.add(dependency);
        }
    }
    
    public void removeDependency(Task dependency) {
        this.dependencies.remove(dependency);
    }

    public boolean areDependenciesDone() {
        for (Task dependency : dependencies) {
            if (dependency.getStatus() != TaskStatus.DONE) {
                return false;
            }
        }
        return true;
    }

    public String displayTask() {
        return "Task{" +
                "taskId='" + taskId + '\'' +
                ", Title='" + Title + '\'' +
                ", status=" + status +
                ", category=" + category +
                ", deadline=" + deadline +
                ", priorityLevel=" + priorityLevel +
                "}";
    }

    public int compareTo(Task other) {
        
        int priorityComparison = other.priorityLevel.compareTo(this.priorityLevel);
        if (priorityComparison != 0) {
            return priorityComparison;
        }
    
        return this.deadline.compareTo(other.deadline);
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        this.Title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskCategory getCategory() {
        return category;
    }

    public void setCategory(TaskCategory category) {
        this.category = category;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public PriorityLevel getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(PriorityLevel priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public Engineer getAssignedEngineer() {
        return assignedEngineer;
    }

    public void setAssignedEngineer(Engineer assignedEngineer) {
        this.assignedEngineer = assignedEngineer;
    }

    public List<Task> getDependencies() {
        return dependencies;
    }

    public List<TaskHistoryEntry> getHistory() {
        return history;
    }
}