package internship_project;

import java.util.ArrayList;
import java.util.List;

public class CSVParser {
	
	// parse one CSV line with quote support
    public static List<String> parseLine(String line) {
        List<String> out = new ArrayList<>();
        if (line == null) return out;
        StringBuilder cur = new StringBuilder();
        boolean inQ = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQ && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else inQ = !inQ;
            } else if (ch == ',' && !inQ) {
                out.add(cur.toString().trim()); cur.setLength(0);
            } else cur.append(ch);
        }
        out.add(cur.toString().trim());
        return out;
    }

    // quote a field for CSV
    public static String q(String s) {
        if (s == null) s = "";
        boolean need = s.contains(",") || s.contains("\"") || s.contains("\n") || s.startsWith(" ") || s.endsWith(" ");
        if (s.contains("\"")) s = s.replace("\"", "\"\"");
        return need ? "\"" + s + "\"" : s;
    }
	
}
