package strms.model;

import strms.manager.TaskManager;
import java.time.LocalDate;
import java.util.Map;

/**
 * Tableau de bord des statistiques des tâches.
 */
public class Dashboard {

    private final TaskManager taskManager;

    public Dashboard(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public void display() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║                 TABLEAU DE BORD              ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        Map<String, Task> tasks = taskManager.getAllTasks();

        int total = tasks.size();
        int todo = countByStatus(tasks, TaskStatus.TODO);
        int blocked = countByStatus(tasks, TaskStatus.BLOCKED);
        int inProgress = countByStatus(tasks, TaskStatus.IN_PROGRESS);
        int done = countByStatus(tasks, TaskStatus.DONE);

        System.out.println("Total des tâches   : " + total);
        System.out.println("TODO              : " + todo);
        System.out.println("BLOCKED           : " + blocked);
        System.out.println("IN_PROGRESS       : " + inProgress);
        System.out.println("DONE              : " + done);

        long overdue = tasks.values().stream()
                .filter(task ->
                        task.getDeadline() != null
                                && task.getDeadline().isBefore(LocalDate.now())
                                && task.getStatus() != TaskStatus.DONE
                )
                .count();

        System.out.println("Tâches en retard   : " + overdue);

        System.out.println("\n--- Tâches par ingénieur ---");

        for (User user : taskManager.getAllUsers().values()) {
            long count = tasks.values().stream()
                    .filter(task ->
                            task.getAssignedEngineer() != null
                                    && task.getAssignedEngineer().getUserId().equals(user.getUserId())
                    )
                    .count();

            if (count > 0) {
                System.out.println(user.getName() + " : " + count + " tâche(s)");
            }
        }

        System.out.println("══════════════════════════════════════════════");
    }

    private int countByStatus(Map<String, Task> tasks, TaskStatus status) {
        return (int) tasks.values()
                .stream()
                .filter(task -> task.getStatus() == status)
                .count();
    }
}