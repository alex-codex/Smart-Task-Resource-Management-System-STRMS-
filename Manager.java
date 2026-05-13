public class Manager extends User {

    public Manager(String id, String name, String email) {
        super(id, name, email);
    }

    public Manager(String id, String name, String email, String password) {
        super(id, name, email, password);
    }

    @Override
    public boolean canCreateTask() {
        return false;
    }

    @Override
    public boolean canDeleteTask() {
        return false;
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
        return false;
    }

    @Override
    public String getRole() {
        return "MANAGER";
    }
}
