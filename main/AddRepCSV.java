package internship_project;
import internship_project.enums.RepStatus;

public class AddRepCSV {
	private final Path file;
    private final UserApp userApp;

    // header
    private static final String[] HEADER = {
        "CompanyRepID","Name","CompanyName","Department","Position","Email","Status"
    };

    public AddRepCSV(Path file, UserApp userApp) {
        this.file = file;
        this.userApp = userApp;
    }

    // load existing company reps (if any) from CSV into UserApp 
    public void load() throws IOException {
        if (!Files.exists(file)) return; 
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line; boolean first = true;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                var cols = CSVParser.parseLine(line);
                if (first && looksHeader(cols)) { first = false; continue; }
                first = false;

                String id   = get(cols,0);
                String name = get(cols,1);
                String comp = get(cols,2);
                String dept = get(cols,3);
                String pos  = get(cols,4);
                String st   = get(cols,6);

                CompanyRep rep = new CompanyRep(id, name, "password", comp, dept, pos);
                try { rep.setStatus(RepStatus.valueOf(st.toUpperCase())); } catch(Exception e){ rep.setStatus(RepStatus.PENDING); }
                userApp.addUser(rep);
            }
        }
    }

    // show new company rep account registration (pending status)
    public void appendNew(CompanyRep rep, String email) throws IOException {
        ensureHeader();
        String line = String.join(",",
        		CSVParser.q(rep.getUserID()),
        		CSVParser.q(rep.getName()),
        		CSVParser.q(rep.getCompanyName()),
        		CSVParser.q(rep.getDepartment()),
        		CSVParser.q(rep.getPosition()),
                CSVParser.q(email == null ? "" : email),
                CSVParser.q(rep.getStatus().name())
        );
        Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);
    }

    // update status for an existing rep, rewrite the file
    public void updateStatus(String repId, RepStatus newStatus) throws IOException {
        List<String> out = new ArrayList<>();
        boolean found = false;

        // read existing reps (or start with header)
        if (Files.exists(file)) {
            try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line; boolean first = true;
                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) continue;
                    var cols = CSVParser.parseLine(line);
                    if (first && looksHeader(cols)) {
                        out.add(String.join(",", HEADER));
                        first = false;
                        continue;
                    }
                    first = false;

                    if (get(cols,0).equalsIgnoreCase(repId)) {
                        cols.set(6, newStatus.name());
                        found = true;
                    }
                    out.add(join(cols));
                }
            }
        }
        if (!found) throw new IllegalArgumentException("Rep ID not found in CSV: " + repId);

        Files.write(file, out, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    private void ensureHeader() throws IOException {
        if (!Files.exists(file) || Files.size(file) == 0) {
            Files.createDirectories(file.getParent() == null ? Path.of(".") : file.getParent());
            Files.writeString(file, String.join(",", HEADER) + System.lineSeparator(), StandardCharsets.UTF_8);
        }
    }

    private static boolean looksHeader(List<String> c) {
        return !c.isEmpty() && c.get(0).toLowerCase().contains("companyrepid");
    }
    private static String get(List<String> c, int i) { return (i < c.size() && c.get(i)!=null) ? c.get(i).trim() : ""; }
    private static String join(List<String> cols) {
        List<String> q = new ArrayList<>(cols.size());
        for (String s : cols) q.add(CSVParser.q(s));
        return String.join(",", q);
    }
