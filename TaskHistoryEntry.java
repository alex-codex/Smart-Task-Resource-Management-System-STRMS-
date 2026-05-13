import java.time.LocalDate;

public class TaskHistoryEntry {
    private String taskId;
    private String action;
    private User user;
    private LocalDate date;

    public TaskHistoryEntry(String taskId, String action, User user, LocalDate date) {
        this.taskId = taskId;
        this.action = action;
        this.user = user;
        this.date = date;
    }

    // Getters
    public String getTaskId() {
        return taskId;
    }

    public String getAction() {
        return action;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "TaskHistoryEntry{" +
                "taskId='" + taskId + '\'' +
                ", action='" + action + '\'' +
                ", user=" + user +
                ", date=" + date +
                '}';
    }
}
