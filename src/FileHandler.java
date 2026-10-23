import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * FILE HANDLING: everything that touches the disk lives here.
 *
 *   readRooms()       data/rooms.csv      -> list of Room objects
 *   readRequests()    data/requests.csv   -> list of AllocationRequest objects
 *   readCheckouts()   data/checkouts.csv  -> list of student IDs
 *   writeReport()     output/*.txt        <- the generated report
 *
 * Blank lines, lines starting with '#', and the header line are skipped.
 */
public class FileHandler {

    private FileHandler() {
    }

    // ---------------------- rooms.csv: roomNumber,type ----------------------
    public static List<Room> readRooms(Path path) throws IOException {
        List<Room> rooms = new ArrayList<>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        for (int i = 0; i < lines.size(); i++) {
            String line = cleanLine(lines.get(i), i);
            if (isSkippable(line, "roomnumber")) {
                continue;
            }
            String[] parts = line.split(",", -1);
            RoomType type = parts.length == 2 ? RoomType.fromString(parts[1]) : null;
            if (type == null || parts[0].trim().isEmpty()) {
                System.err.println("Skipping bad room line " + (i + 1) + ": " + line);
                continue;
            }
            rooms.add(RoomFactory.create(type, parts[0].trim().toUpperCase()));
        }
        return rooms;
    }

    // -------- requests.csv: studentId,name,year,distanceKm,preferredRoom --------
    public static List<AllocationRequest> readRequests(Path path) throws IOException {
        List<AllocationRequest> requests = new ArrayList<>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        int sequence = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = cleanLine(lines.get(i), i);
            if (isSkippable(line, "studentid")) {
                continue;
            }
            sequence++;
            String[] parts = line.split(",", -1);

            if (parts.length != 5) {
                requests.add(new AllocationRequest(sequence, "LINE-" + (i + 1),
                        "Line " + (i + 1) + " is malformed: expected 5 columns, found "
                                + parts.length));
                continue;
            }

            String id = parts[0].trim().toUpperCase();
            String name = parts[1].trim();
            int year = parseInt(parts[2], 0);              // 0 is rejected by the validator
            double distance = parseDouble(parts[3], -1);   // -1 is rejected by the validator
            RoomType type = RoomType.fromString(parts[4]);

            requests.add(new AllocationRequest(new Student(id, name, year, distance),
                    type, parts[4].trim(), sequence));
        }
        return requests;
    }

    // ---------------- checkouts.csv: one student ID per line ----------------
    public static List<String> readCheckouts(Path path) throws IOException {
        List<String> ids = new ArrayList<>();
        if (!Files.exists(path)) {
            return ids;   // check-outs are optional
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = cleanLine(lines.get(i), i);
            if (isSkippable(line, "studentid")) {
                continue;
            }
            ids.add(line.toUpperCase());
        }
        return ids;
    }

    // ---------------------------- report output ----------------------------
    public static void writeReport(Path path, String content) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());   // create output/ if missing
        }
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------ helpers ------------------------------
    private static String cleanLine(String raw, int index) {
        String line = raw.trim();
        if (index == 0 && line.startsWith("\uFEFF")) {   // strip Excel's BOM marker
            line = line.substring(1).trim();
        }
        return line;
    }

    private static boolean isSkippable(String line, String headerStart) {
        return line.isEmpty()
                || line.startsWith("#")
                || line.toLowerCase().startsWith(headerStart);
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}