public class Main {
    public static void main(String[] args) {

        // --- Création des utilisateurs ---
        Admin admin = new Admin("alice", "Alice", "alice@eseo.fr", "admin123");
        Manager manager = new Manager("bob", "Bob", "bob@eseo.fr", "mgr123");
        Engineer engineer = new Engineer("charlie", "Charlie", "charlie@eseo.fr", "eng123");

        System.out.println("=== Utilisateurs créés ===");
        System.out.println(admin);
        System.out.println(manager);
        System.out.println(engineer);

        // --- Création d'une tâche ---
        Task task = new Task(
            "Implémenter le module auth",
            "Créer le système d'authentification JWT",
            PriorityLevel.HIGH,
            TaskCategory.FEATURE
        );

        System.out.println("\n=== Tâche créée ===");
        System.out.println(task);

        // --- Assignation et changement de statut ---
        task.assignTo(engineer);
        task.setStatus(TaskStatus.IN_PROGRESS);

        System.out.println("\n=== Après assignation ===");
        System.out.println("Assignée à : " + task.getAssignee());
        System.out.println("Statut : " + task.getStatus());

        // --- TaskManager ---
        TaskManager taskManager = new TaskManager();
        taskManager.addTask(task);

        Task task2 = new Task(
            "Rédiger les tests unitaires",
            "Couverture > 80%",
            PriorityLevel.MEDIUM,
            TaskCategory.TESTING
        );
        taskManager.addTask(task2);

        System.out.println("\n=== Liste des tâches ===");
        for (Task t : taskManager.getAllTasks()) {
            System.out.println("- " + t.getTitle() + " [" + t.getStatus() + "]");
        }

        // --- Notifications ---
        NotificationManager.send(NotificationType.EMAIL, "Une tâche a été créée pour l'ingénieur Charlie");

        System.out.println("\n=== Système initialisé avec succès ===");
    }
}