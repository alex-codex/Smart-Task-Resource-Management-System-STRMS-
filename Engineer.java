public class Engineer extends User {
    private String specialization;
    private double workloadHours;

    public Engineer(String userId, String name, String email, String role, String specialization, double workloadHours) {
        super(userId, name, email, role);
        this.specialization = specialization;
        this.workloadHours = workloadHours;
    }

    public Engineer() {
        super();
    }

    // Getters and Setters
    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public double getWorkloadHours() {
        return workloadHours;
    }

    public void setWorkloadHours(double workloadHours) {
        this.workloadHours = workloadHours;
    }

    @Override
    public String toString() {
        return "Engineer{" +
                "userId='" + getUserId() + '\'' +
                ", name='" + getName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", role='" + getRole() + '\'' +
                ", specialization='" + specialization + '\'' +
                ", workloadHours=" + workloadHours +
                '}';
    }
}
