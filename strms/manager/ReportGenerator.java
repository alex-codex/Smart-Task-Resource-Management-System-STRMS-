package strms.manager;

import strms.interfaces.Reportable;
import strms.manager.TaskManager;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Génère des rapports textuels sur les tâches et utilisateurs.
 * Implémente l'interface Reportable.
 */
public class ReportGenerator implements Reportable {

    private final TaskManager taskManager;

    public ReportGenerator(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public String generateReport() {
        StringBuilder sb = new StringBuilder();

        sb.append("════════════════════════════════════════\n");
        sb.append("         RAPPORT\n");
        sb.append("════════════════════════════════════════\n");
        sb.append("Date : ").append(LocalDateTime.now()).append("\n\n");

        Map<String, Task> tasks = taskManager.getAllTasks();

        sb.append("Nombre total de tâches : ")
          .append(tasks.size())
          .append("\n\n");

        for (Task task : tasks.values()) {
            sb.append("─ ")
              .append(task.getTaskId()).append(" | ")
              .append(task.getTitle()).append(" | ")
              .append(task.getStatus()).append(" | ")
              .append(task.getPriorityLevel())
              .append("\n");
        }

        sb.append("\n════════════════════════════════════════\n");

        return sb.toString();
    }

    /**
     * Affiche le rapport dans la console.
     */
    public void printReport() {
        System.out.println(generateReport());
    }
}