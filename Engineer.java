public class Engineer extends User {

    public Engineer(String id, String name, String email) {
        super(id , name, email);
    }

    @Override public boolean canCreateTask()      
    { return false; }
    @Override public boolean canDeleteTask()       
    { return false; }
    @Override public boolean canAssignTask()       
    { return false; }
    @Override public boolean canGenerateReport()   
    { return false; }
    @Override public boolean canUpdateTaskStatus() 
    { return true; }
    @Override public String  getRole()             
    { return "ENGINEER"; }
}
