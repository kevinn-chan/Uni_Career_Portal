package internship_project;

public abstract class User {
	
	private String userID;	// unique identifier of user
	private String name;	// full name of user	
	private String password;	//user's password
	private Filtering filters;
	
	public User(String userID, String name, String password) {
		this.userID = userID;
		this.name = name;
		this.password = password;
		this.filters = new Filtering();}		//each user gets their own filter options
	
	public String getUserID() {
		return userID;}		// gets users unique ID
	
	public String getName() {
		return name;}		// gets users name
	
	/*private String getPassword() {
		return password;}*/	// gets users password, BUT SHOULD NOT BE PUBLIC MIGHT REMOVE
	
	public Filtering getFilters() {
		return filters;}
	
	public boolean changePassword(String oldPassword, String newPassword) {
		if (!(this.password.equals(oldPassword))) {
			System.out.println("Incorrect password entered.");
			return false;}
		
		if (newPassword.isEmpty()) {
				System.out.println("New password cannot be empty.");
				return false;}
			
		if (this.password.equals(oldPassword)) {
			this.password = newPassword;
			System.out.println("Password successfully changed.");
			return true;}		//method to change password
		
		return false;}		//default


	public boolean checkPassword(String password) {
		return this.password.equals(password);} 	//if password is correct, used for encapsulation

	public abstract void showMenu();		// to be defined in all subclasses
}
