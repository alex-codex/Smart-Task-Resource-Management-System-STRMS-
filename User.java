public abstract class User{
    protected final String id;
    protected String name; 
    protected String email;

    public User( String id , String name , String email){
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public abstract boolean canCreateTask();
    public abstract boolean canDeleteTask();
    public abstract boolean canAssignTask();
    public abstract boolean canGenerateReport();
    public abstract boolean canUpdateTaskStatus();
    public abstract boolean canModify();
    public abstract String  getRole();

    public String getName(){
        return name;
    }

    public String getEmail(){
        return email;
    }

    public String getId(){
        return id;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setEmail(String email){
        this.email = email;
    }

    @Override
    public String toString() {
        return String.format ("User[%s | %s | %s | %s]", id, name, email, getRole());
    }

     @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return id.equals(((User) o).id);
    }

     @Override
    public int hashCode() { 
        return id.hashCode();
    }
    
}