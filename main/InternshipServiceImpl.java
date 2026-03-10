package internship_project;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import internship_project.enums.*;

public class InternshipServiceImpl implements InternshipService {
	
	private final UserApp userApp;
	
    // master lists
    private final List<Internship> approvedInternships = new ArrayList<>();
    private final List<Internship> pendingInternships  = new ArrayList<>();
    private final List<Application> applications       = new ArrayList<>();

    // persistence handlers
    private InternshipCSV intStore;
    private ApplicationCSV appStore;

    public InternshipServiceImpl(UserApp userApp) {
        this.userApp = userApp;
    }

    @Override
    public void setPersistenceHandlers(InternshipCSV intStore, ApplicationCSV appStore) {
        this.intStore = intStore;
        this.appStore = appStore;
    }

    
    // ====== getters for CSV loaders ======
    public List<Internship> getApprovedInternships() { return approvedInternships; }
    public List<Internship> getPendingInternships() { return pendingInternships; }
    public List<Application> getApplications() { return applications; }
    
    public void addApprovedInternship(Internship i) { approvedInternships.add(i); }
    public void addPendingInternship(Internship i)  { pendingInternships.add(i); }
    public void addApplication(Application a)       { applications.add(a); }

    
    // Company Rep methods 
    
    @Override
    public Internship createInternship(CompanyRep rep,
                                       String title,
                                       String description,
                                       InternshipLevel level,
                                       String preferredMajor,
                                       LocalDate openingDate,
                                       LocalDate closingDate,
                                       int numberOfSlots) {

        if (rep == null) {
            throw new IllegalArgumentException("Only a logged-in Company Representative can create opportunities.");
        }

        // Max 5 per rep (both approved + pending)
        long owned = approvedInternships.stream()
                .filter(i -> rep.getUserID().equals(i.getCompanyRepId()))
                .count()
                + pendingInternships.stream()
                .filter(i -> rep.getUserID().equals(i.getCompanyRepId()))
                .count();

        if (owned >= 5) {
            throw new IllegalArgumentException("Limit reached: max 5 opportunities.");
        }

        if (openingDate == null || closingDate == null) {
            throw new IllegalArgumentException("Opening and closing dates must not be null.");
        }
        if (closingDate.isBefore(openingDate)) {
            throw new IllegalArgumentException("Closing date cannot be before opening date.");
        }

        if (numberOfSlots < 1 || numberOfSlots > 10) {
            throw new IllegalArgumentException("Number of slots must be between 1 and 10.");
        }

        Internship internship = new Internship(
                title,
                description,
                level,
                preferredMajor,
                closingDate,
                openingDate,
                rep.getCompanyName(),
                rep.getUserID(),
                numberOfSlots
        );
        internship.setStatus(InternshipStatus.PENDING);
        internship.setVisible(false);

        pendingInternships.add(internship);

        try {
            intStore.appendNew(internship);
        } catch (IOException e) {
            System.err.println("[WARN] Failed to save new internship to CSV: " + e.getMessage());
        }

        return internship;
    }

    @Override
    public List<Internship> getInternshipsForRep(CompanyRep rep) {
        String repId = rep.getUserID();
        List<Internship> out = new ArrayList<>();

        for (Internship x : approvedInternships) {
            if (repId.equals(x.getCompanyRepId())) out.add(x);
        }
        for (Internship x : pendingInternships) {
            if (repId.equals(x.getCompanyRepId())) out.add(x);
        }

        // Apply the rep's saved filters
        out = applyUserFilters(out, rep.getFilters());

        out.sort(Comparator.comparing(Internship::getInternshipTitle,
                String.CASE_INSENSITIVE_ORDER));
        return out;
    }
    
    public List<Internship> getPendingInternshipsForRep(CompanyRep rep) {
        return getInternshipsForRep(rep).stream()
    	        .filter(i -> getApplicationsForInternship(rep, i.getId()).stream()
    	                .anyMatch(a -> a.getStatus() == ApplicationStatus.PENDING))
    	        .sorted(Comparator.comparingInt(Internship::getId))
    	        .toList();
    }

    @Override
    public boolean updateInternship(CompanyRep rep,
                                    int internshipId,
                                    String newTitle,
                                    String newDescription,
                                    InternshipLevel newLevel,
                                    String newPreferredMajor,
                                    LocalDate newOpeningDate,
                                    LocalDate newClosingDate,
                                    Integer newNumberOfSlots) {

        Internship i = findApprovedById(internshipId);
        if (i == null) {
            i = findPendingById(internshipId);
        }
        if (i == null) return false;

        // must belong to this rep
        if (!rep.getUserID().equals(i.getCompanyRepId())) return false;

        // editing restricted once approved
        if (i.getStatus() == InternshipStatus.APPROVED) return false;

        // apply changes if provided
        if (newTitle != null) i.setInternshipTitle(newTitle.trim()); 
        if (newDescription != null) i.setDescription(newDescription.trim());
        if (newLevel != null) i.setInternshipLevel(newLevel);
        if (newPreferredMajor != null) i.setPreferredMajor(newPreferredMajor.trim());
        if (newOpeningDate != null) i.setApplicationOpeningDate(newOpeningDate);
        if (newClosingDate != null) i.setApplicationClosingDate(newClosingDate);
        if (newNumberOfSlots != null) {
            int s = newNumberOfSlots;
            if (s < 1 || s > 10) {
                throw new IllegalArgumentException("Slots must be between 1 and 10.");
            }
            i.setNumberOfSlots(s);
        }

        // validate dates after update
        if (i.getApplicationClosingDate().isBefore(i.getApplicationOpeningDate())) {
            throw new IllegalArgumentException("Closing date cannot be before opening date.");
        }

        try {
            intStore.updateAll();
        } catch (IOException e) {
            System.err.println("[WARN] Failed to save internships CSV: " + e.getMessage());
        }

        return true;
    }

    @Override
    public boolean deletePendingInternship(CompanyRep rep, int internshipId) {
        Internship i = findPendingById(internshipId);
        if (i == null) return false;

        // must belong to rep & cannot alr be approved
        if (!rep.getUserID().equals(i.getCompanyRepId())) return false;
        if (i.getStatus() == InternshipStatus.APPROVED) return false;
        
        boolean removed = pendingInternships.remove(i);
        if (!removed) return false;

        try {
            intStore.updateAll();
        } catch (IOException e) {
            System.err.println("[WARN] Failed to save internships CSV: " + e.getMessage());
        }

        return true;
    }

    @Override
    public boolean toggleVisibility(CompanyRep rep, int internshipId, boolean visible) {
        Internship i = findApprovedById(internshipId);
        if (i == null) return false;

        // must belong to rep
        if (!rep.getUserID().equals(i.getCompanyRepId())) return false;
        
        if (i.getStatus() != InternshipStatus.APPROVED) return false;
        
        i.setVisible(visible);

        try {
            intStore.updateAll();
        } catch (IOException e) {
            System.err.println("[WARN] Failed to save internships CSV: " + e.getMessage());
        }

        return true;
    }

    @Override
    public List<Application> getApplicationsForInternship(CompanyRep rep, int internshipId) {
        Internship i = findApprovedById(internshipId);
        if (i == null) return List.of();
        
        if (!rep.getUserID().equals(i.getCompanyRepId())) return List.of(); // not your internship
        

        List<Application> out = new ArrayList<>();
        for (Application a : applications) {
            if (a.getInternshipId() == internshipId) {
                out.add(a);
            }
        }
        out.sort(Comparator.comparing(Application::getStudentId));
        return out;
    }

    @Override
    public boolean decideApplication(CompanyRep rep, String studentId, int internshipId, boolean approve) {
        Internship i = findApprovedById(internshipId);
        if (i == null) return false;

        if (!rep.getUserID().equals(i.getCompanyRepId())) return false;
        
        Application target = null;
        for (Application a : applications) {
            if (a.getInternshipId() == internshipId && a.getStudentId().equals(studentId)) {
                target = a;
                break;
            }
        }
        if (target == null || target.getStatus() != ApplicationStatus.PENDING) return false;
        
        if (approve) {
            long offered = applications.stream()
                    .filter(a -> a.getInternshipId() == internshipId)
                    .filter(a -> a.getStatus() == ApplicationStatus.SUCCESSFUL)
                    .count();

            if (offered >= i.getNumberOfSlots()) return false;
            target.setStatus(ApplicationStatus.SUCCESSFUL);
        } else {
            target.setStatus(ApplicationStatus.UNSUCCESSFUL);
        }

        try {
            appStore.updateAll();
        } catch (IOException e) {
            System.err.println("[WARN] Failed to save applications CSV: " + e.getMessage());
        }

        return true;
    }
    
    @Override
    public String getRepNotification(CompanyRep rep) {
        String repId = rep.getUserID();

        long pendingAppsForRep = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.PENDING)
                .filter(a -> {
                    Internship i = findApprovedById(a.getInternshipId());
                    return i != null && repId.equals(i.getCompanyRepId());
                })
                .count();

        if (pendingAppsForRep == 0) {
            return "\nUpdates: No new applications for your internships.";
        }
        return "\nUpdates: You have " + pendingAppsForRep + " pending application(s) to review.";
    }
    
    //  Career Centre Staff methods 
    
	 @Override
	 public List<Internship> getPendingInternshipsForStaff(Filtering staffFilters) {
	     List<Internship> base = new ArrayList<>(pendingInternships);
	     List<Internship> filtered = applyUserFilters(base, staffFilters);
	     filtered.sort(Comparator.comparing(Internship::getInternshipTitle,
	                                        String.CASE_INSENSITIVE_ORDER));
	     return filtered;
	 }
	
	 @Override
	 public boolean approveInternship(int internshipId) {
	     Internship target = findPendingById(internshipId);
	     if (target == null) return false;
	     
	     // remove from pending list
	     boolean removed = pendingInternships.remove(target);
	     if (!removed) return false;
	     
	     // update status & visibility, then add to approved list
	     target.setStatus(InternshipStatus.APPROVED);
	     target.setVisible(true);
	     approvedInternships.add(target);
	
	     try {
	         intStore.updateAll();
	     } catch (IOException e) {
	         System.err.println("[WARN] Failed to save internships CSV: " + e.getMessage());
	         // still return true because in-memory state is consistent
	     }
	
	     return true;
	 }
	
	 @Override
	 public boolean rejectInternship(int internshipId) {
	     Internship target = findPendingById(internshipId);
	     if (target == null) return false;
	     
	     boolean removed = pendingInternships.remove(target);
	     if (!removed) return false;
	     
	     target.setStatus(InternshipStatus.REJECTED);
	
	     try {
	         intStore.updateAll();
	     } catch (IOException e) {
	         System.err.println("[WARN] Failed to save internships CSV: " + e.getMessage());
	     }
	
	     return true;
	 }
	
	 @Override
	 public List<Internship> generateReport(InternshipStatus status, String preferredMajor,
	                                        String companyName, InternshipLevel level) {
		 
	     List<Internship> all = new ArrayList<>(pendingInternships);
	     all.addAll(approvedInternships);
	
	     if (all.isEmpty()) return List.of();
	     
	     return all.stream()
	             .filter(i -> status == null || i.getStatus() == status)
	             .filter(i -> preferredMajor == null
	                       || (i.getPreferredMajor() != null
	                           && i.getPreferredMajor().equalsIgnoreCase(preferredMajor)))
	             .filter(i -> companyName == null
	                       || (i.getCompanyName() != null
	                           && i.getCompanyName().equalsIgnoreCase(companyName)))
	             .filter(i -> level == null || i.getInternshipLevel() == level)
	             .sorted(Comparator.comparing(Internship::getInternshipTitle,
	                                          String.CASE_INSENSITIVE_ORDER))
	             .toList();
	 }
	
	 @Override
	 public List<Application> listPendingWithdrawals() {
	     List<Application> out = new ArrayList<>();
	     for (Application a : applications) {
	         if (a.getWithdrawalStatus() == WithdrawalRequest.PENDING) out.add(a);
	     }
	     out.sort(Comparator.comparing(Application::getStudentId));
	     return out;
	 }
	
	 @Override
	 public boolean processWithdrawal(String studentId, int internshipId, boolean approve) {
	     Application a = null;
	     for (Application x : applications) {
	         if (x.getStudentId().equalsIgnoreCase(studentId) && x.getInternshipId() == internshipId) {
	             a = x;
	             break;
	         }
	     }
	     if (a == null || a.getWithdrawalStatus() != WithdrawalRequest.PENDING) return false;
	     
	     boolean internshipChanged = false;
	
	     if (approve) {
	         a.setWithdrawalStatus(WithdrawalRequest.APPROVED);
	         a.setStatus(ApplicationStatus.WITHDRAWN);
	
	         Internship i = findApprovedById(internshipId);
	         if (i != null) {
	             // if this was an accepted placement, free a confirmed slot
	             if (a.isAccepted()) {
	                 int confirmed = Math.max(0, i.getConfirmedSlots() - 1);
	                 i.setConfirmedSlots(confirmed);
	                 internshipChanged = true;
	             }
	
	             // if internship was FILLED but now has free slots, reopen to APPROVED
	             if (i.getStatus() == InternshipStatus.FILLED
	                     && i.getConfirmedSlots() < i.getNumberOfSlots()) {
	                 i.setStatus(InternshipStatus.APPROVED);
	                 internshipChanged = true;
	             }
	         }
	     } else {
	         a.setWithdrawalStatus(WithdrawalRequest.REJECTED);
	     }
	     
	     if (approve) {
	    	    Logger.log(studentId, "WITHDRAWAL_ACCEPTED", "Internship " + internshipId);
	    	} else {
	    	    Logger.log(studentId, "WITHDRAWAL_REJECTED", "Internship " + internshipId);
	    	}
	
	     try {
	         appStore.updateAll();
	     } catch (IOException e) {
	         System.err.println("[WARN] Failed to save applications CSV: " + e.getMessage());
	     }
	     
	     if (internshipChanged) {
	         try {
	             intStore.updateAll();
	         } catch (IOException e) {
	             System.err.println("[WARN] Failed to save internships CSV: " + e.getMessage());
	         }
	     }
	
	     return true;
	 }
	 
	 @Override
	 public String getStaffNotification(CareerCenterStaff staff) {
	     int pendingReps = userApp.viewRepsPending().size();

	     long pendingInts = pendingInternships.stream()
	             .filter(i -> i.getStatus() == InternshipStatus.PENDING)
	             .count();

	     int pendingWithdrawals = listPendingWithdrawals().size();

	     var parts = new ArrayList<String>();
	     parts.add(pendingReps + " company representative(s) pending approval");
	     parts.add(pendingInts + " internship opportunity/ies pending approval");
	     parts.add(pendingWithdrawals + " withdrawal request(s) pending");

	     return "\nUpdates:\n" + String.join("\n", parts);
	 }
    
    //  Student methods 

	@Override
	public List<Internship> getEligibleInternships(Student s) {
	    LocalDate today = LocalDate.now();

	    List<Internship> baseList = approvedInternships.stream()
	            .filter(Internship::isVisible)
	            .filter(i -> !today.isBefore(i.getApplicationOpeningDate())
	                      && !today.isAfter(i.getApplicationClosingDate()))
	            .filter(i -> i.getPreferredMajor() != null
	                      && i.getPreferredMajor().equalsIgnoreCase(s.getMajor()))
	            .filter(i -> {
	                int y = s.getYearOfStudy();
	                InternshipLevel lvl = i.getInternshipLevel();
	                if (y <= 2) return lvl == InternshipLevel.BASIC; // Y1–2 only BASIC
	                return true; // Y3–4 all levels
	            })
	            .filter(i -> i.getConfirmedSlots() < i.getNumberOfSlots())
	            .toList();

	    // apply student's saved filters
	    List<Internship> filteredList = applyUserFilters(baseList, s.getFilters());

	    return filteredList.stream()
	            .sorted(Comparator.comparing(Internship::getInternshipTitle,
	                                         String.CASE_INSENSITIVE_ORDER))
	            .toList();
	}

	@Override
	public void applyForInternship(Student s, int internshipId) throws IllegalArgumentException {
		boolean alreadyAccepted = applications.stream()
	            .anyMatch(a -> a.getStudentId().equals(s.getUserID()) && a.isAccepted());
	    if (alreadyAccepted) {
	        throw new IllegalArgumentException("You have already accepted an internship placement and cannot apply for more.");
	    }
	    
	    var eligible = getEligibleInternships(s);

	    Internship chosen = eligible.stream()
	            .filter(i -> i.getId() == internshipId)
	            .findFirst()
	            .orElseThrow(() -> new IllegalArgumentException("Invalid or ineligible internship ID."));

	    boolean alreadyApplied = applications.stream()
	            .anyMatch(a -> a.getStudentId().equals(s.getUserID())
	                        && a.getInternshipId() == internshipId);
	    
	    if (alreadyApplied) {
	        throw new IllegalArgumentException("You have already applied for this internship.");
	    }

	    long active = applications.stream()
	            .filter(a -> a.getStudentId().equals(s.getUserID()))
	            .filter(a -> a.getStatus() == ApplicationStatus.PENDING
	                      || a.getStatus() == ApplicationStatus.SUCCESSFUL)
	            .count();
	    
	    if (active >= 3) {
	        throw new IllegalArgumentException("You already have 3 active applications.");
	    }

	    Application newApp = new Application(s.getUserID(), internshipId);
	    applications.add(newApp);
	    
	    Logger.log(s.getUserID(), "APPLIED", "Internship " + internshipId);

	    try {
	        appStore.updateAll();
	    } catch (IOException e) {
	        System.err.println("[WARN] Failed to save applications CSV: " + e.getMessage());
	    }
	    
	    System.out.println("Application successfully submitted for: " + chosen.getInternshipTitle());
	}

	@Override
	public List<Application> getApplicationsForStudent(Student s) {
	    List<Application> out = new ArrayList<>();
	    for (Application a : applications) {
	        if (a.getStudentId().equals(s.getUserID())) out.add(a);
	    }
	    // sort by internship ID
	    out.sort(Comparator.comparing(Application::getInternshipId));
	    return out;
	}

	@Override
	public boolean acceptPlacement(Student s, int internshipId) {
	    
	    boolean alreadyAccepted = applications.stream()
	            .anyMatch(a -> a.getStudentId().equals(s.getUserID()) && a.isAccepted());
	    if (alreadyAccepted) return false;
	    
	    Application target = null;
	    for (Application a : applications) {
	        if (a.getStudentId().equals(s.getUserID()) && a.getInternshipId() == internshipId) {
	            target = a;
	            break;
	        }
	    }
	    if (target == null || target.getStatus() != ApplicationStatus.SUCCESSFUL) return false;
	    
	    target.setAccepted(true);
	    boolean changedInternship = false;

	    Internship i = findApprovedById(internshipId);
	    if (i != null) {
	        i.setConfirmedSlots(i.getConfirmedSlots() + 1);
	        i.setNumberOfSlots(i.getNumberOfSlots() - 1);
	        if (i.getConfirmedSlots() >= i.getNumberOfSlots()) i.setStatus(InternshipStatus.FILLED);
	        changedInternship = true;
	    }

	    // auto-withdraw other apps for this student
	    applications.stream()
	            .filter(a -> a.getStudentId().equals(s.getUserID()) && a.getInternshipId() != internshipId)
	            .forEach(a -> a.setStatus(ApplicationStatus.WITHDRAWN));
	    
	    Logger.log(s.getUserID(), "ACCEPTED", "Internship " + internshipId);

	    try {
	        appStore.updateAll();
	    } catch (IOException e) {
	        System.err.println("[WARN] Failed to save applications CSV: " + e.getMessage());
	    }
	    if (changedInternship) {
	        try {
	            intStore.updateAll();
	        } catch (IOException e) {
	            System.err.println("[WARN] Failed to save internships CSV: " + e.getMessage());
	        }
	    }

	    return true;
	}

	@Override
	public boolean requestWithdrawal(Student s, int internshipId) {
	    Application a = null;
	    for (Application x : applications) {
	        if (x.getStudentId().equals(s.getUserID())
	                && x.getInternshipId() == internshipId) {
	            a = x;
	            break;
	        }
	    }
	    if (a == null) return false;
	    if (a.getWithdrawalStatus() == WithdrawalRequest.PENDING) return false;

	    a.setWithdrawalStatus(WithdrawalRequest.PENDING);

	    try {
	        appStore.updateAll();
	    } catch (IOException e) {
	        System.err.println("[WARN] Failed to save applications CSV: " + e.getMessage());
	    }

	    return true;
	}
	
	@Override
	public String getStudentNotification(Student s) {
		List<Internship> eligible = getEligibleInternships(s);

	    long successfulOffers = applications.stream()
	            .filter(a -> a.getStudentId().equals(s.getUserID()))
	            .filter(a -> a.getStatus() == ApplicationStatus.SUCCESSFUL && !a.isAccepted())
	            .count();

	    long pendingWithdrawals = applications.stream()
	            .filter(a -> a.getStudentId().equals(s.getUserID()))
	            .filter(a -> a.getWithdrawalStatus() == WithdrawalRequest.PENDING)
	            .count();
	    
	    List<Internship> expiringSoon = eligible.stream()
	    	    .filter(i -> !LocalDate.now().isAfter(i.getApplicationClosingDate()))
	    	    .filter(i -> LocalDate.now().until(i.getApplicationClosingDate()).getDays() <= 3)
	    	    .toList();
	    
	    int expiringCount = expiringSoon.size();
	    List<String> expiringIds = expiringSoon.stream()
	            .map(i -> "#" + i.getId())
	            .toList();
	    
	    List<Internship> limitedSlots = eligible.stream()
	    	    .filter(a -> a.getNumberOfSlots() == 1)
	    	    .toList();
	    int slotCount = limitedSlots.size();
	    List<String> slotIds = limitedSlots.stream()
	            .map(i -> "#" + i.getId())
	            .toList();

	    StringBuilder sb = new StringBuilder();
	    sb.append("\nUpdates: ");

	    var parts = new ArrayList<String>();
	    parts.add(eligible.size() + " eligible internship(s)");

	    if (successfulOffers > 0) parts.add(successfulOffers + " successful application(s) awaiting acceptance");
	    if (pendingWithdrawals > 0)  parts.add(pendingWithdrawals + " withdrawal request(s) pending staff decision");
	    if (expiringCount > 0) parts.add(expiringCount + " internship(s) closing within 3 days: "+ String.join(", ", expiringIds));
	    if (slotCount > 0) parts.add(slotCount + " internship(s) with 1 slot left: "+ String.join(", ", slotIds));

	    if (parts.isEmpty()) {
	        sb.append("\nNo updates.");
	    } else {
	        sb.append("\n" + String.join("\n", parts));
	    }

	    return sb.toString();
	}
	
	// filtering 
    private List<Internship> applyUserFilters(List<Internship> inputList, Filtering f) {
        if (f == null) return inputList; // safety check
        
        return inputList.stream()
            .filter(i -> f.getStatus() == null || i.getStatus() == f.getStatus())
            .filter(i -> f.getPreferredMajors() == null || i.getPreferredMajor().equalsIgnoreCase(f.getPreferredMajors()))
            .filter(i -> f.getLevel() == null || i.getInternshipLevel() == f.getLevel())
            .filter(i -> f.getCompanyName() == null || i.getCompanyName().equalsIgnoreCase(f.getCompanyName()))
            .collect(Collectors.toList());
    }
    
    // helpers and utils  
    public Internship findApprovedById(int id) {
        for (Internship i : approvedInternships) if (i.getId() == id) return i;
        return null;
    }
    public Internship findPendingById(int id) {
        for (Internship i : pendingInternships) if (i.getId() == id) return i;
        return null;
    }
}
