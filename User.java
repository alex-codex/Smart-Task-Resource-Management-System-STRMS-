public abstract class User {
    protected String id;
    protected String name;
    protected String email;
    protected String password;

    public User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = "";
    }

    public User(String id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // Abstract permission methods
    public abstract boolean canCreateTask();
    public abstract boolean canDeleteTask();
    public abstract boolean canAssignTask();
    public abstract boolean canGenerateReport();
    public abstract boolean canUpdateTaskStatus();
    public abstract String getRole();

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }

    @Override
    public String toString() {
        return getRole() + " [" + id + "] " + name + " (" + email + ")";
    }
}
