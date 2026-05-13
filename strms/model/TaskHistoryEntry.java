package strms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class TaskHistoryEntry {
    private String taskId;
    private String action;
    private User user;
    private LocalDate date;
    private LocalDateTime dateTime;

    public TaskHistoryEntry(String taskId, String action, User user, LocalDate date) {
        this.taskId = taskId;
        this.action = action;
        this.user = user;
        this.date = date;
        this.dateTime = date.atStartOfDay();
    }

    public TaskHistoryEntry(String action, User user, LocalDateTime dateTime) {
        this.taskId = "";
        this.action = action;
        this.user = user;
        this.dateTime = dateTime;
        this.date = dateTime.toLocalDate();
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

    public LocalDateTime getDateTime() {
        return dateTime;
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

    public String toFileString() {
        String userName = user != null ? user.getName() : "UNKNOWN";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return action + "|" + userName + "|" + dateTime.format(formatter);
    }

    public static TaskHistoryEntry fromFileString(String fileString, HashMap<String, User> users) {
        String[] parts = fileString.split("\\|", 3);
        if (parts.length < 3) {
            return null;
        }
        String action = parts[0];
        String userName = parts[1];
        LocalDateTime dateTime = LocalDateTime.parse(parts[2], java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        User user = users.get(userName);
        return new TaskHistoryEntry(action, user, dateTime);
    }
}
