package internship_project;

public class CareerCenterStaff extends User{
	
	private final String staffdepartment;

	public CareerCenterStaff(String userID, String name, String password, String staffdepartment) {
		super(userID, name, password);
		this.staffdepartment= staffdepartment;}
	
	public String getStaffdepartment() {
		return staffdepartment;}
	
	public void showMenu() {
		System.out.println("\nCareer Center Staff Menu:"); 
		System.out.println("-".repeat(40));
		System.out.println("Hello " + super.getName() + " (" + getStaffdepartment() + ")!");
		System.out.println("-".repeat(40));
		System.out.println("What would you like to do?");
		System.out.println("1. Manage Company Representatives Accounts");
		System.out.println("2. Manage Internship Opportunities");
		System.out.println("3. Manage Withdrawal Requests");
		System.out.println("4. Generate Comprehensive Reports");
		System.out.println("5. Set View Filters");
		System.out.println("6. Change Password");
		System.out.println("7. Logout");
		System.out.println("-".repeat(40));
	}
}
