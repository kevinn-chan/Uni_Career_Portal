package internship_project;

import java.time.LocalDate;
import java.util.List;

import internship_project.enums.*;

// pure business logic for internships & applications

public interface InternshipService {

    // COMPANY REP:
    // for the additional feature of notifs upon login
	String getRepNotification(CompanyRep rep);

    // for reps to create an internship (menu item 1)
    Internship createInternship(CompanyRep rep,
                                String title,
                                String description,
                                InternshipLevel level,
                                String preferredMajor,
                                LocalDate openingDate,
                                LocalDate closingDate,
                                int numberOfSlots);

    // create list of all internships made by rep (+ filters)
    List<Internship> getInternshipsForRep(CompanyRep rep);
    
    // create list of internships with pending applications (for menu item 3)
	List<Internship> getPendingInternshipsForRep(CompanyRep rep);

    // for reps to edit internship info (menu item 2)
    boolean updateInternship(CompanyRep rep,
                             int internshipId,
                             String newTitle,
                             String newDescription,
                             InternshipLevel newLevel,
                             String newPreferredMajor,
                             LocalDate newOpeningDate,
                             LocalDate newClosingDate,
                             Integer newNumberOfSlots);

    // for reps to delete internships ONLY IF ITS PENDING (menu item 2)
    boolean deletePendingInternship(CompanyRep rep, int internshipId);

    // for reps to toggle internship visibility ONLY IF ITS APPROVED (menu item 2)
    boolean toggleVisibility(CompanyRep rep, int internshipId, boolean visible);

    // list all applications for a specific internship (menu item 3)
    List<Application> getApplicationsForInternship(CompanyRep rep, int internshipId);

    // for reps to choose application outcome (menu item 3)
    boolean decideApplication(CompanyRep rep, String studentId, int internshipId, boolean approve);

	
    // CAREER CENTRE STAFF:
    // for the additional feature of notifs upon login
	String getStaffNotification(CareerCenterStaff staff);
	
    // list internships PENDING approval from staff
    List<Internship> getPendingInternshipsForStaff(Filtering staffFilters);

    // to approve internship by ID: move from pending to approved list, status set APPROVED, vis set true
    boolean approveInternship(int internshipId);

    // to reject internship by ID: status set REJECTED
    boolean rejectInternship(int internshipId);

    // generate report of internships matching criteria, blank to keep field empty
    List<Internship> generateReport(InternshipStatus status,
                                    String preferredMajor,
                                    String companyName,
                                    InternshipLevel level);

    // list applications with PENDING withdrawal req, sort by studentID
    List<Application> listPendingWithdrawals();

    // for staff to choose withdrawal outcome: update status and slot count
    boolean processWithdrawal(String studentId, int internshipId, boolean approve);
	

    // STUDENT:
    // for the additional feature of notifs upon login
    String getStudentNotification(Student s);
    
    // list internships that student is eligible to see based on vis, date, major, InternshipLevel, slots and filters
    List<Internship> getEligibleInternships(Student student);

    // apply for internship by id: max 3, no duplicates
    void applyForInternship(Student student, int internshipId);

    // list of all the students applications
    List<Application> getApplicationsForStudent(Student student);

    // for student to accept internship they have a SUCCESSFUL offer for
    // student cannot hv accepted other offers, and this will withdraw all other apps
    // also will increased confiredSlots, set InternshipStatus to FILLED if full
    boolean acceptPlacement(Student student, int internshipId);

    // submit withdrawal for existing application
    boolean requestWithdrawal(Student student, int internshipId);
    
    
    // HELPERS:

    Internship findApprovedById(int id);
    Internship findPendingById(int id);
    
    void setPersistenceHandlers(InternshipCSV intStore, ApplicationCSV appStore);

    List<Internship> getApprovedInternships();
    List<Internship> getPendingInternships();
    List<Application> getApplications();

    void addApprovedInternship(Internship i);
    void addPendingInternship(Internship i);
    void addApplication(Application a);

}
