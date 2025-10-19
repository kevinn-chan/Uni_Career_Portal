package Internship_project;

import java.util.List;

import Internship_project.enums.RepStatus;

import java.util.ArrayList;


public class UserApp {
	
	private List<User> allUsers;		//allUsers is masterlist to store all user objects
	
	private User curUser; 		//default null when noone logged in, curuser for user logged in
	
	public UserApp(List<User> allUsers) {
		this.allUsers = allUsers;
		this.curUser = null;}
	
	public User getCurUser() {
		return this.curUser;}
	
	public boolean login(String userID, String password) {		//get input userID and password
		for(User user : allUsers) {								//for each user in masterlist
			if(user.getUserID().equals(userID) && user.checkPassword(password)) {		//correct userID and password
				if(user instanceof CompanyRep) {
					if(!(((CompanyRep) user).getStatus() == RepStatus.APPROVED)) {		//if companyrep not approved
						System.out.println("Login Failed! Your account has not been approved.");
						return false;}
					else {	
						this.curUser = user;
						System.out.println("Login Successful! Welcome" + user.getName() + ".");
						return true;}}
				else { 
					this.curUser = user;
					System.out.println("Login Successful! Welcome" + user.getName() + ".");
					return true;}				
				}
			else { 
				System.out.println("Login Failed! Incorrect UserID or Password.");
				return false;}
			}
				
		}
	
	public void logout() {
		if(this.curUser != null) {
			this.curUser = null; 	//reset back to default
			System.out.println("Logout Successful!");}
		else {
			System.out.println("Logout Unsuccessful.");}}
	
	public boolean isLoggedIn() {
		return this.curUser != null;}
		
	public void approveCompanyRep(CompanyRep rep, CareerCenterStaff staff) {
		if (!(rep.getStatus() == RepStatus.APPROVED)){
			rep.setStatus(RepStatus.APPROVED);
			System.out.println("Company representative account for " + rep.getName() + " approved by Career Center Staff: " + staff.getName());}
		else {
			System.out.println("Company representative account for " + rep.getName() + " is already approved");}}
	
	public void rejectCompanyRep(CompanyRep rep, CareerCenterStaff staff) {
		if (rep.getStatus() == RepStatus.PENDING) {
			rep.setStatus(RepStatus.REJECTED);
			System.out.println("Company representative account for " + rep.getName() + " rejected by Career Center Staff: " + staff.getName());}
		else {
			System.out.println("Company representative account for " + rep.getName() + " is not pending approval");}}

	
		
			
			
			
			
		}

		
	


