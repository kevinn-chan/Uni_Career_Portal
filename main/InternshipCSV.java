package internship_project;

import internship_project.enums.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// load or save for Internship obj, using safe rewrite all for updates
public class InternshipCSV {
    private final Path file;
    private final InternshipService intApp;

    // match order written
    private static final String[] HEADER = {
        "InternshipID", "Title", "Description", "Level", "Major", "ClosingDate",
        "OpeningDate", "Status", "CompanyName", "CompanyRepId",
        "NumberOfSlots", "ConfirmedSlots", "Visible"
    };

    public InternshipCSV(Path file, InternshipService service) {
        this.file = file;
        this.intApp = service;
    }

    // load internships from CSV into InternshipApp 
    public void load() throws IOException {
        if (!Files.exists(file)) return; // no file, nothing to load

        int maxId = 0;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line; boolean first = true;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                var cols = CSVParser.parseLine(line);
                if (first && looksHeader(cols)) { first = false; continue; }
                first = false;

                try {
                    int id = parseInt(get(cols,0), 0);
                    if (id == 0) continue; // skip bad data
                    maxId = Math.max(maxId, id);

                    String title = get(cols,1);
                    String desc = get(cols,2);
                    InternshipLevel level = InternshipLevel.valueOf(get(cols,3));
                    String major = get(cols,4);
                    LocalDate closing = LocalDate.parse(get(cols,5));
                    LocalDate opening = LocalDate.parse(get(cols,6));
                    InternshipStatus status = InternshipStatus.valueOf(get(cols,7));
                    String compName = get(cols,8);
                    String repId = get(cols,9);
                    int slots = parseInt(get(cols,10), 1);
                    int confirmed = parseInt(get(cols,11), 0);
                    boolean visible = Boolean.parseBoolean(get(cols,12));

                    // create object using the loader constructor
                    Internship i = new Internship(id, title, desc, level, major, closing, opening,
                                                  compName, repId, slots);
                    // manually set the state fields
                    i.setStatus(status);
                    i.setConfirmedSlots(confirmed);
                    i.setVisible(visible);

                    // add to the correct list in InternshipApp
                    if (status == InternshipStatus.PENDING || status == InternshipStatus.REJECTED) {
                        intApp.addPendingInternship(i);
                    } else {
                        intApp.addApprovedInternship(i);
                    }
                } catch (Exception e) {
                    System.out.println("[WARN] Skipping bad internship CSV line: " + e.getMessage());
                }
            }
        }
        // update the static ID tracker in Internship class
        Internship.setNextId(maxId + 1);
    }

    // append a single new internship  
    public void appendNew(Internship i) throws IOException {
        ensureHeader();
        String line = toCsvLine(i);
        Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);
    }

    // rewrites the entire file with the current in-memory state 
    public void updateAll() throws IOException {
        ensureHeader();
        List<String> out = new ArrayList<>();
        out.add(String.join(",", HEADER)); // start with header

        // add all lists
        for (Internship i : intApp.getPendingInternships()) out.add(toCsvLine(i));
        for (Internship i : intApp.getApprovedInternships()) out.add(toCsvLine(i));
        
        Files.write(file, out, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // converts one Internship object to a CSV line
    private String toCsvLine(Internship i) {
        return String.join(",",
            CSVParser.q(String.valueOf(i.getId())),
            CSVParser.q(i.getInternshipTitle()),
            CSVParser.q(i.getDescription()),
            CSVParser.q(i.getInternshipLevel().name()),
            CSVParser.q(i.getPreferredMajor()),
            CSVParser.q(i.getApplicationClosingDate().toString()),
            CSVParser.q(i.getApplicationOpeningDate().toString()),
            CSVParser.q(i.getStatus().name()),
            CSVParser.q(i.getCompanyName()),
            CSVParser.q(i.getCompanyRepId()),
            CSVParser.q(String.valueOf(i.getNumberOfSlots())),
            CSVParser.q(String.valueOf(i.getConfirmedSlots())),
            CSVParser.q(String.valueOf(i.isVisible()))
        );
    }
    
    // CSV helpers from AddRepCSV 
    private void ensureHeader() throws IOException {
        if (!Files.exists(file) || Files.size(file) == 0) {
            Files.createDirectories(file.getParent() == null ? Path.of(".") : file.getParent());
            Files.writeString(file, String.join(",", HEADER) + System.lineSeparator(), StandardCharsets.UTF_8);
        }
    }
    private static boolean looksHeader(List<String> c) { return !c.isEmpty() && c.get(0).toLowerCase().contains("internshipid"); }
    private static String get(List<String> c, int i) { return (i < c.size() && c.get(i)!=null) ? c.get(i).trim() : ""; }
    private static int parseInt(String s, int d) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return d; } }
}
