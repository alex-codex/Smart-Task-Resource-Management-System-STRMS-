public class Admin extends User {

    public Admin(String id, String name, String email) {
        super(id, name, email);
    }

    public Admin(String id, String name, String email, String password) {
        super(id, name, email, password);
    }

    @Override
    public boolean canCreateTask() {
        return true;
    }

    @Override
    public boolean canDeleteTask() {
        return true;
    }

    @Override
    public boolean canAssignTask() {
        return true;
    }

    @Override
    public boolean canGenerateReport() {
        return true;
    }

    @Override
    public boolean canUpdateTaskStatus() {
        return true;
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }
}
