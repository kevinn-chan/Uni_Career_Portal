package internship_project;

import java.util.*;
import java.time.LocalDate;

import internship_project.enums.*;

public class Main {
    private final UserApp userApp = new UserApp();

    // Use service instead of InternshipApp
    private InternshipService service;

    private AddRepCSV repStore;
    private InternshipCSV intStore;
    private ApplicationCSV appStore;

    public static void main(String[] args) { new Main().run(); }

    private void run() {
        // init persistance handles
        var repPath = java.nio.file.Paths.get("sample_company_representative_list.csv");
        var intPath = java.nio.file.Paths.get("internships.csv");
        var appPath = java.nio.file.Paths.get("applications.csv");

        this.repStore = new AddRepCSV(repPath, userApp);

        this.service = new InternshipServiceImpl(userApp);
        
        // pass service into CSV handlers
        this.intStore = new InternshipCSV(intPath, service);
        this.appStore = new ApplicationCSV(appPath, service);

        service.setPersistenceHandlers(intStore, appStore);

        // load data
        seedDataFromCsv();
        try { repStore.load(); } catch (Exception e) { System.out.println("[SEED] reps.csv load: " + e.getMessage()); }
        try { intStore.load(); } catch (Exception e) { System.out.println("[SEED] internships.csv load: " + e.getMessage()); }
        try { appStore.load(); } catch (Exception e) { System.out.println("[SEED] applications.csv load: " + e.getMessage()); }

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== Internship Placement System ===");
            System.out.println("1) Login   2) Register as Company Rep   3) Exit");
            System.out.print("Choice: ");
            String c = sc.nextLine().trim();
            switch (c) {
                case "1" -> {
                    System.out.print("User ID: "); String id = sc.nextLine();
                    System.out.print("Password: "); String pw = sc.nextLine();
                    if (userApp.login(id, pw)) routeMenu(sc);
                }
                case "2" -> registerRepFlow(sc);
                case "3" -> { System.out.println("Thank you."); sc.close(); return; }
                default   -> System.out.println("Invalid option.");
            }
        }
    }

    // rep registration
    private void registerRepFlow(Scanner sc) {
        System.out.println("\n-- Register as Company Representative --");
        System.out.print("CompanyRepID (login ID): "); String id = sc.nextLine().trim();
        System.out.print("Name: "); String name = sc.nextLine().trim();
        System.out.print("Company Name: "); String company = sc.nextLine().trim();
        System.out.print("Department: "); String dept = sc.nextLine().trim();
        System.out.print("Position: "); String pos = sc.nextLine().trim();
        System.out.print("Email: "); String email = sc.nextLine().trim();
        String pw = "password"; // or prompt

        try {
            CompanyRep rep = new CompanyRep(id, name, pw, company, dept, pos); // default PENDING
            userApp.addUser(rep);                 // in-memory
            repStore.appendNew(rep, email);       // persist
            System.out.println("Account created and pending approval by Career Center Staff.");
        } catch (IllegalArgumentException dup) {
            System.out.println("Registration failed: " + dup.getMessage());
        } catch (java.io.IOException io) {
            System.out.println("Registration saved in memory, but failed to persist CSV: " + io.getMessage());
        }
    }

    private void routeMenu(Scanner sc) {
        User u = userApp.getCurUser();
        printNotifications(u);
        while (userApp.checkifLoggedIn()) {
            u.showMenu();
            System.out.print("Enter choice: ");
            String choice = sc.nextLine().trim();

            if (u instanceof Student s)                 handleStudent(sc, s, choice);
            else if (u instanceof CompanyRep r)        handleRep(sc, r, choice);
            else if (u instanceof CareerCenterStaff s) handleStaff(sc, s, choice);
        }
    }

    // menus: student, rep and staff
    private void handleStudent(Scanner sc, Student s, String choice) {
        switch (choice) {
	        case "1" -> viewEligibleInternshipsUI(s);
	        case "2" -> applyForInternshipUI(sc, s);
	        case "3" -> viewMyApplicationsUI(s);
	        case "4" -> acceptPlacementUI(sc, s);
	        case "5" -> requestWithdrawalUI(sc, s);
	        case "6" -> handleFilters(sc, s);
	        case "7" -> changePasswordUI(sc, s);
	        case "8" -> userApp.logout();
	        default  -> System.out.println("Invalid.");
        }
    }

    private void handleRep(Scanner sc, CompanyRep r, String choice) {
        switch (choice) {
            case "1" -> createInternshipUI(sc, r);
            case "2" -> manageRepInternshipsUI(sc, r);
            case "3" -> manageRepApplicationsUI(sc, r);
            case "4" -> handleFilters(sc, r);
            case "5" -> changePasswordUI(sc, r);
            case "6" -> userApp.logout();
            default -> System.out.println("Invalid.");
        }
    }

    private void handleStaff(Scanner sc, CareerCenterStaff staff, String choice) {
        switch (choice) {
            case "1" -> manageRepAccountsUI(sc, staff);
            case "2" -> manageInternshipOpportunitiesUI(sc, staff);
            case "3" -> processWithdrawalsUI(sc);
            case "4" -> generateReportUI(sc);
            case "5" -> handleFilters(sc, staff);
            case "6" -> changePasswordUI(sc, staff);
            case "7" -> userApp.logout();
            default -> System.out.println("Invalid.");
        }
    }
    
    // the ui helpers/methods, routed by the menus
    private void changePasswordUI(Scanner sc, User u) {
        System.out.print("Old password: ");
        String oldP = sc.nextLine();
        System.out.print("New password: ");
        String newP = sc.nextLine();
        u.changePassword(oldP, newP);
    }
    
    // student helpers
    private void viewEligibleInternshipsUI(Student s) {
        var list = service.getEligibleInternships(s);
        if (list.isEmpty()) System.out.println("No internships available.");
        else printStudentTable(list, "Eligible Internships");
    }

    private void applyForInternshipUI(Scanner sc, Student s) {
        System.out.print("Enter internship ID to apply: ");
        int id = safeInt(sc);
        try {
            service.applyForInternship(s, id);
        } catch (IllegalArgumentException e) {
            System.out.println("Application failed: " + e.getMessage());
        }
    }

    private void viewMyApplicationsUI(Student s) {
        var apps = service.getApplicationsForStudent(s);
        if (apps.isEmpty()) System.out.println("No applications yet.");
        else {
            System.out.println("\n=== My Applications ===");
            for (Application a : apps) {
                System.out.println(
                    "Internship #" + a.getInternshipId()
                    + " >> " + a.getStatus()
                    + (a.isAccepted() ? " (ACCEPTED)" : "")
                    + " | Withdrawal: " + a.getWithdrawalStatus()
                );
            }
        }
    }

    private void acceptPlacementUI(Scanner sc, Student s) {
        System.out.print("Enter internship ID to accept: ");
        int id = safeInt(sc);
        System.out.println(service.acceptPlacement(s, id) ? "Accepted!" : "Cannot accept.");
    }
    
    private void requestWithdrawalUI(Scanner sc, Student s) {
        System.out.print("Enter internship ID to withdraw: ");
        int id = safeInt(sc);
        System.out.println(service.requestWithdrawal(s, id) ? "Request sent." : "Failed.");
    }
    // rep helpers
    private void createInternshipUI(Scanner sc, CompanyRep rep) {
        try {
            System.out.print("Internship Title: "); String title = sc.nextLine().trim();
            System.out.print("Description: "); String desc = sc.nextLine().trim();
            System.out.print("Internship Level (BASIC, INTERMEDIATE, ADVANCED): ");
            InternshipLevel level = InternshipLevel.valueOf(sc.nextLine().trim().toUpperCase());
            System.out.print("Preferred Major: "); String major = sc.nextLine().trim();
            System.out.print("Opening date (YYYY-MM-DD): ");
            LocalDate opening = LocalDate.parse(sc.nextLine().trim());
            System.out.print("Closing date (YYYY-MM-DD): ");
            LocalDate closing = LocalDate.parse(sc.nextLine().trim());
            System.out.print("Number of slots (1..10): ");
            int slots = Integer.parseInt(sc.nextLine().trim());

            service.createInternship(rep, title, desc, level, major, opening, closing, slots);
            System.out.println("Draft created. Pending CAO approval. Currently invisible to applicants.");
        } catch (Exception e) {
            System.out.println("Creation failed: " + e.getMessage());
        }
    }

    private void manageRepInternshipsUI(Scanner sc, CompanyRep rep) {
        var mine = service.getInternshipsForRep(rep);
        if (mine.isEmpty()) {
            System.out.println("You have no opportunities matching your filters.");
            return;
        }
        printStaffTable(mine, "Your opportunites (filtered)");

        System.out.println("\n1) Edit  2) Delete (only if not APPROVED)  3) Toggle Visibility  4) Back");
        System.out.print("Choice: ");
        String sub = sc.nextLine().trim();
        if ("4".equals(sub)) return;

        System.out.print("Enter internship ID: ");
        int iid = safeInt(sc);

        switch (sub) {
            case "1" -> { // Edit
                System.out.println("Edit field: 1) Title  2) Description  3) Level  4) Preferred major  5) Opening date  6) Closing date  7) Slots  8) Cancel");
                System.out.print("Choice: ");
                String ch = sc.nextLine().trim();

                String newTitle = null, newDesc = null, newMajor = null;
                InternshipLevel newLevel = null;
                LocalDate newOpen = null, newClose = null;
                Integer newSlots = null;

                try {
                    switch (ch) {
                        case "1" -> { System.out.print("New title: "); newTitle = sc.nextLine().trim(); }
                        case "2" -> { System.out.print("New description: "); newDesc = sc.nextLine().trim(); }
                        case "3" -> {
                            System.out.print("Level (BASIC, INTERMEDIATE, ADVANCED): ");
                            newLevel = InternshipLevel.valueOf(sc.nextLine().trim().toUpperCase());
                        }
                        case "4" -> { System.out.print("Preferred major: "); newMajor = sc.nextLine().trim(); }
                        case "5" -> {
                            System.out.print("Opening (YYYY-MM-DD): ");
                            newOpen = LocalDate.parse(sc.nextLine().trim());
                        }
                        case "6" -> {
                            System.out.print("Closing (YYYY-MM-DD): ");
                            newClose = LocalDate.parse(sc.nextLine().trim());
                        }
                        case "7" -> {
                            System.out.print("Slots (1..10): ");
                            newSlots = Integer.parseInt(sc.nextLine().trim());
                        }
                        case "8" -> { return; }
                        default -> { System.out.println("Invalid."); return; }
                    }

                    boolean ok = service.updateInternship(
                            rep, iid, newTitle, newDesc, newLevel,
                            newMajor, newOpen, newClose, newSlots
                    );
                    System.out.println(ok ? "Saved." : "Edit failed (not found or not editable).");
                } catch (Exception e) {
                    System.out.println("Edit failed: " + e.getMessage());
                }
            }
            case "2" -> { // Delete
                boolean ok = service.deletePendingInternship(rep, iid);
                System.out.println(ok ? "Deleted." : "Delete failed (not found / already approved?).");
            }
            case "3" -> { // Toggle visibility
                System.out.print("Turn visibility ON (Y) / OFF (N)? ");
                boolean to = "Y".equalsIgnoreCase(sc.nextLine().trim());
                boolean ok = service.toggleVisibility(rep, iid, to);
                System.out.println(ok ? ("Visibility set to " + (to ? "ON." : "OFF."))
                                      : "Failed to toggle (must be APPROVED & belong to you).");
            }
            default -> System.out.println("Invalid.");
        }
    }

    private void manageRepApplicationsUI(Scanner sc, CompanyRep rep) {
    	var mine = service.getPendingInternshipsForRep(rep);
        if (mine.isEmpty()) {
            System.out.println("No pending applications.");
            return;
        }
        printStaffTable(mine, "Opportunities with pending applications");

        System.out.print("Enter internship ID to review applications: ");
        int iid = safeInt(sc);

        var apps = service.getApplicationsForInternship(rep, iid);
        if (apps.isEmpty()) {
            System.out.println("No applications yet.");
            return;
        }

        for (Application a : apps) {
            String sid = a.getStudentId();
            String sInfo = sid;
            User u = userApp.getUserInfo(sid);
            if (u instanceof Student st) {
                sInfo = sid + " (" + st.getName() + ", " + st.getMajor() + ", Y" + st.getYearOfStudy() + ")";
            }
            System.out.println(sInfo + " → " + a.getStatus() + (a.isAccepted() ? " (ACCEPTED)" : ""));
        }

        System.out.print("Enter Student ID to decide (blank to cancel): ");
        String sid = sc.nextLine().trim();
        if (sid.isEmpty()) return;

        System.out.print("Approve (A) / Reject (R)? ");
        boolean approve = "A".equalsIgnoreCase(sc.nextLine().trim());

        boolean ok = service.decideApplication(rep, sid, iid, approve);
        System.out.println(ok
                ? (approve ? "Marked SUCCESSFUL." : "Marked UNSUCCESSFUL.")
                : "Decision failed.");
    }

    // staff helpers
    private void manageRepAccountsUI(Scanner sc, CareerCenterStaff staff) {
    	var pending = userApp.viewRepsPending();
        if (pending.isEmpty()) { System.out.println("No pending company representatives."); return; }
        pending.forEach(r -> System.out.println(r.getUserID() + " | " + r.getName() + " | " + r.getCompanyName()));
        System.out.print("Enter Rep ID to decide (blank to cancel): ");
        String rid = sc.nextLine().trim(); if (rid.isEmpty()) return;

        CompanyRep rep = (CompanyRep) userApp.getUserInfo(rid);
        if (rep == null) { System.out.println("Not found."); return; }

        System.out.print("Approve (A) / Reject (R)? ");
        String ar = sc.nextLine().trim().toUpperCase();
        if ("A".equals(ar)) {
            userApp.approveCompanyRep(rep, staff);
            try { repStore.updateStatus(rep.getUserID(), RepStatus.APPROVED); }
            catch (Exception e) { System.out.println("[WARN] reps.csv not updated: " + e.getMessage()); }
        } else if ("R".equals(ar)) {
            userApp.rejectCompanyRep(rep, staff);
            try { repStore.updateStatus(rep.getUserID(), RepStatus.REJECTED); }
            catch (Exception e) { System.out.println("[WARN] reps.csv not updated: " + e.getMessage()); }
        } else System.out.println("Invalid.");
    }
    
    private void manageInternshipOpportunitiesUI(Scanner sc, CareerCenterStaff staff) {
        Filtering f = staff.getFilters();
        var filteredPending = service.getPendingInternshipsForStaff(f);
        if (filteredPending.isEmpty()) {
            System.out.println("No pending opportunities match your filters.");
            return;
        }
        
        printStaffTable(filteredPending, "Pending opportunites (filtered)");

        System.out.print("Enter internship ID to review (blank to cancel): ");
        String inp = sc.nextLine().trim();
        if (inp.isEmpty()) return;
        int iid = safeInt(inp);
        if (iid < 0) { System.out.println("Invalid ID."); return; }

        System.out.print("Approve (A) / Reject (R)? ");
        String ar = sc.nextLine().trim().toUpperCase();

        boolean changed = false;
        if ("A".equals(ar)) {
            changed = service.approveInternship(iid);
            System.out.println(changed ? "Approved. Opportunity is now visible to eligible students." : "Approval failed.");
        } else if ("R".equals(ar)) {
            changed = service.rejectInternship(iid);
            System.out.println(changed ? "Rejected." : "Rejection failed.");
        } else {
            System.out.println("Invalid choice.");
        }
    }

    private void processWithdrawalsUI(Scanner sc) {
        var pend = service.listPendingWithdrawals();
        if (pend.isEmpty()) {
            System.out.println("No pending withdrawal requests.");
            return;
        }

        pend.forEach(a ->
                System.out.println("Student " + a.getStudentId() + " → Internship #" + a.getInternshipId())
        );

        System.out.print("Enter Student ID: ");
        String sid = sc.nextLine().trim();
        if (sid.isEmpty()) return;

        System.out.print("Enter Internship ID: ");
        int iid = safeInt(sc);

        System.out.print("Approve (A) / Reject (R)? ");
        boolean approve = "A".equalsIgnoreCase(sc.nextLine().trim());

        boolean ok = service.processWithdrawal(sid, iid, approve);
        System.out.println(ok
                ? (approve ? "Withdrawal approved." : "Withdrawal rejected.")
                : "No such pending request or operation failed.");
    }

    private void generateReportUI(Scanner sc) {
        System.out.println("== Report Filters (leave blank to skip) ==");
        System.out.print("Status [PENDING/APPROVED/REJECTED/FILLED]: ");
        String s = sc.nextLine().trim().toUpperCase();
        System.out.print("Preferred Major (exact): ");
        String mj = sc.nextLine().trim();
        System.out.print("Company Name (exact): ");
        String cn = sc.nextLine().trim();
        System.out.print("Level [BASIC/INTERMEDIATE/ADVANCED]: ");
        String lv = sc.nextLine().trim().toUpperCase();

        InternshipStatus status = null;
        InternshipLevel level = null;
        if (!s.isEmpty()) {
            try { status = InternshipStatus.valueOf(s); } catch (Exception ignored) {}
        }
        if (!lv.isEmpty()) {
            try { level = InternshipLevel.valueOf(lv); } catch (Exception ignored) {}
        }
        if (mj.isEmpty()) mj = null;
        if (cn.isEmpty()) cn = null;

        var out = service.generateReport(status, mj, cn, level);
        if (out.isEmpty()) {
            System.out.println("No results for selected filters.");
            return;
        }
        printStaffTable(out, "Report results");
    }

    // handles the filtering mechanism for all users
    private void handleFilters(Scanner sc, User u) {
        Filtering f = u.getFilters();
        while (true) {
            System.out.println("\n-- Set View Filters --");
            System.out.println("1. Filter by Status (Current: " + f.getStatus() + ")");
            System.out.println("2. Filter by Major (Current: " + f.getPreferredMajors() + ")");
            System.out.println("3. Filter by Level (Current: " + f.getLevel() + ")");
            System.out.println("4. Filter by Company (Current: " + f.getCompanyName() + ")");
            System.out.println("5. Clear All Filters");
            System.out.println("6. Back to main menu");
            System.out.print("Choice: ");
            String c = sc.nextLine().trim();
            switch (c) {
                case "1" -> {
                    System.out.print("Status [PENDING/APPROVED/REJECTED/FILLED] (blank to clear): ");
                    String s = sc.nextLine().trim().toUpperCase();
                    if (s.isEmpty()) f.filterStatus(null);
                    else try { f.filterStatus(InternshipStatus.valueOf(s)); } catch (Exception e) { System.out.println("Invalid status."); }
                }
                case "2" -> {
                    System.out.print("Preferred Major (blank to clear): ");
                    String s = sc.nextLine().trim();
                    f.filterPreferredMajors(s.isEmpty() ? null : s);
                }
                case "3" -> {
                    System.out.print("Level [BASIC/INTERMEDIATE/ADVANCED] (blank to clear): ");
                    String s = sc.nextLine().trim().toUpperCase();
                    if (s.isEmpty()) f.filterLevel(null);
                    else try { f.filterLevel(InternshipLevel.valueOf(s)); } catch (Exception e) { System.out.println("Invalid level."); }
                }
                case "4" -> {
                    System.out.print("Company Name (blank to clear): ");
                    String s = sc.nextLine().trim();
                    f.filterCompanyName(s.isEmpty() ? null : s);
                }
                case "5" -> f.clearFilters();
                case "6" -> { return; }
                default -> System.out.println("Invalid.");
            }
        }
    }

    // CSV seeding
    private void seedDataFromCsv() {
        var base = java.nio.file.Paths.get(".");
        try { loadStudents(base.resolve("sample_student_list.csv")); } catch (Exception e) { System.out.println("[SEED] students.csv: " + e.getMessage()); }
        try { loadStaff(base.resolve("sample_staff_list.csv")); }   catch (Exception e) { System.out.println("[SEED] staff.csv: " + e.getMessage()); }
    }

    private void loadStudents(java.nio.file.Path csv) throws java.io.IOException {
        try (var br = java.nio.file.Files.newBufferedReader(csv)) {
            String line; boolean first = true;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                var cols = CSVParser.parseLine(line);
                if (first && looksHeader(cols)) { first = false; continue; }
                first = false;
                // StudentID,Name,Major,Year,Email
                String id = get(cols,0), name = get(cols,1), major = get(cols,2);
                int year = parseInt(get(cols,3), 1);
                String pw = "password";
                userApp.addUser(new Student(id, name, pw, year, major));
            }
        }
    }

    private void loadStaff(java.nio.file.Path csv) throws java.io.IOException {
        try (var br = java.nio.file.Files.newBufferedReader(csv)) {
            String line; boolean first = true;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                var cols = CSVParser.parseLine(line);
                if (first && looksHeader(cols)) { first = false; continue; }
                first = false;
                // StaffID,Name,Role,Department,Email
                String id = get(cols,0), name = get(cols,1), dept = get(cols,3);
                String pw = "password";
                userApp.addUser(new CareerCenterStaff(id, name, pw, dept));
            }
        }
    }

    private static boolean looksHeader(java.util.List<String> c) {
        if (c.isEmpty()) return false;
        String s = c.get(0).toLowerCase();
        return s.contains("id") || s.contains("student") || s.contains("staff");
    }
    private static String get(java.util.List<String> c, int i) { return (i < c.size() && c.get(i) != null) ? c.get(i).trim() : ""; }
    private static int parseInt(String s, int d) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return d; } }

    // overload for safeInt(String) used in manageInternshipOpportunitiesUI
    private static int safeInt(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return -1; } }

    private static int safeInt(Scanner sc) { int v = -1; try { v = Integer.parseInt(sc.nextLine().trim()); } catch (Exception ignored) {} return v; }
    
    // notifications helper (additional feature for updates upon login)
    private void printNotifications(User u) {
        String msg = null;

        if (u instanceof Student s) {
            msg = service.getStudentNotification(s);
        } else if (u instanceof CompanyRep r) {
            msg = service.getRepNotification(r);
        } else if (u instanceof CareerCenterStaff staff) {
            msg = service.getStaffNotification(staff);
        }

        if (msg != null && !msg.isBlank()) {
            System.out.println(msg);
        }
    }
    
    // ui helpers for formatting tables
    public static void printStudentTable(List<Internship> list, String title) {
        System.out.println("\n" + title + ":\n");

        System.out.printf(
            " %-4s %-28s %-9s %-35s %-15s %-12s %-12s %-8s%n",
            "ID", "Title", "Company", "Major", "Level", "Open", "Close", "Slots"
        );

        System.out.println("=".repeat(130));

        for (Internship i : list) {
            System.out.printf(
                "#%-4d %-28s %-9s %-35s %-15s %-12s %-12s %d/%d%n",
                i.getId(),
                truncate(i.getInternshipTitle(), 28),
                i.getCompanyName(),
                i.getPreferredMajor(),
                i.getInternshipLevel(),
                i.getApplicationOpeningDate(),
                i.getApplicationClosingDate(),
                i.getConfirmedSlots(),
                i.getNumberOfSlots()
            );
        }
    }
    
    public static void printStaffTable(List<Internship> list, String title) {
        System.out.println("\n" + title + ":\n");

        System.out.printf(
            " %-4s %-28s %-9s %-35s %-15s %-10s %-6s %-12s %-12s %-8s%n",
            "ID", "Title", "Company", "Major", "Level", "Status", "Vis", "Open", "Close", "Slots"
        );

        System.out.println("=".repeat(150));

        for (Internship i : list) {
            System.out.printf(
                "#%-4d %-28s %-9s %-35s %-15s %-10s %-6s %-12s %-12s %d/%d%n",
                i.getId(),
                truncate(i.getInternshipTitle(), 28),
                i.getCompanyName(),
                i.getPreferredMajor(),
                i.getInternshipLevel(),
                i.getStatus(),
                i.isVisible(),
                i.getApplicationOpeningDate(),
                i.getApplicationClosingDate(),
                i.getConfirmedSlots(),
                i.getNumberOfSlots()
            );
        }
    }

    private static String truncate(String s, int len) {
        return (s.length() <= len) ? s : s.substring(0, len - 3) + "...";
    }
}
