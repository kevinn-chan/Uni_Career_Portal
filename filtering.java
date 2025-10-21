package Internship_project;

import java.time.LocalDate;

import Internship_project.enums.ApplicationStatus;
import Internship_project.enums.InternshipLevel;
import Internship_project.enums.InternshipStatus;

public class filtering {
	private InternshipStatus status = null;
	private String preferredMajors = null;
	private InternshipLevel level = null;
	private LocalDate closingDate = null;		
	private ApplicationStatus appStatus = null;
	private String companyName = null;
	private Boolean ifVisible = false;		//all set null as default
	
	public InternshipStatus getStatus() {
		return status;}
	
	public String getPreferredMajors() {
		return preferredMajors;}
	
	public InternshipLevel getLevel() {
		return level;}
	
	public LocalDate getClosingDate() {
		return closingDate;}
	
	public ApplicationStatus getAppStatus() {
		return appStatus;}
	
	public String getCompanyName() {
		return companyName;}
	
	public Boolean toggleVisibility() {
		return ifVisible;}
	
	public void filterStatus(InternshipStatus status) {
		this.status = status;
		System.out.println("Filtered. Internship Status: " + (status == null ? "NIL" : status));}
	
	public void filterPreferredMajors(String major) {
		this.preferredMajors = major;
		System.out.println("Filtered. Major: " + (major == null ? "NIL" : major));}
	
	public void filterLevel(InternshipLevel level) {
		this.level = level;
		System.out.println("Filtered. Internship Level: " + (level == null ? "NIL" : level));}	
	
	public void filterClosingDate(LocalDate date) {
		this.closingDate = date;
		System.out.println("Filtered. Closing Date: " + (date == null ? "NIL" : date));}
	
	public void filterAppStatus(ApplicationStatus appStatus) {
		this.appStatus = appStatus;
		System.out.println("Filtered. Application Status: " + (appStatus == null ? "NIL" : appStatus));}
	
	public void filterCompanyName(String companyName) {
		this.companyName = companyName;
		System.out.println("Filtered. Company Name: " + (companyName == null ? "NIL" : companyName));}
	
	public void filterVisibility(Boolean ifVisible) {
		this.ifVisible = ifVisible;
		String visibilityStatus;
		if(ifVisible == null) { 
			visibilityStatus = "All Options";}
		else if(ifVisible) {
			visibilityStatus = "Only Visible Options";}
		else {
			visibilityStatus = "Only Hidden Options";}
		System.out.println("Filtered. Visibility: " + visibilityStatus);}
		
	public void clearFilters() {
		this.status = null;
		this.preferredMajors = null;
		this.level = null;
		this.closingDate = null;
		this.appStatus = null;
		this.companyName = null;
		this.ifVisible = null;
		System.out.println("All filters cleared");}
		
			
}
		
	
