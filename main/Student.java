package internship_project;

public class Student extends User {
    
    private final int yearOfStudy;
    private final String major;
    private int numApplied;
    
    public Student(String userID, String name, String password,
                int yearOfStudy, String major) {
        super(userID, name, password);
        this.yearOfStudy = yearOfStudy;
        this.major = major;
        this.numApplied = 0;
    }
    
    public int getYearOfStudy() {
        return yearOfStudy; }

    public String getMajor() {
        return major; }

    public int getNumApplied() {
        return numApplied; }

    public void setNumApplied(int numApplied) {
        this.numApplied = numApplied;
    }

    public String getInternshipLevel() { // calculates and returns the student's internship level eligibility
        String internshipLevel;
        if (this.yearOfStudy < 3) { // year 1 or 2 
            internshipLevel = "BASIC";
        } else { // year 3 or higher
            internshipLevel = "ALL"; // eligible for all internship levels
        }
        return internshipLevel;
    } 
    public void showMenu() {
        System.out.println("\nStudent Menu: ");
        System.out.println("-".repeat(40));
        System.out.println("Hello " + super.getName() +
                        ", Year " + getYearOfStudy() +
                        " " + getMajor() + " student!");
        System.out.println("-".repeat(40));
        System.out.println("What would you like to do?");
        System.out.println("1. View Internship Opportunities");
        System.out.println("2. Apply for Internship");
        System.out.println("3. View My Applications");
        System.out.println("4. Accept Internship Placement");
        System.out.println("5. Request Withdrawal");
        System.out.println("6. Set View Filters");
        System.out.println("7. Change Password");
        System.out.println("8. Logout");
        System.out.println("-".repeat(40));
    }
}
