import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FILE HANDLING: everything that touches the disk lives here.
 *
 *   readRooms()       data/rooms.csv      -> list of Room objects
 *   readRequests()    data/requests.csv   -> list of AllocationRequest objects
 *   readCheckouts()   data/checkouts.csv  -> list of student IDs
 *   writeReport()     output/*.txt        <- the generated report
 *
 * File formats:
 *   rooms.csv      roomNumber,type,gender
 *   requests.csv   studentId,name,gender,year,distanceKm,preferredRoom
 *   checkouts.csv  one student ID per line
 *
 * How the CSV files are read:
 *   - every file is read once into rows (readRows), then each reader turns
 *     the rows into objects
 *   - blank lines and lines starting with '#' are ignored
 *   - the first real line is skipped if it is a header (e.g. "studentId,...")
 *   - fields may be wrapped in double quotes, so  "Sharma, Aarav"  stays one
 *     field, and  ""  inside quotes means a literal quote character
 *   - one bad line never stops the program: bad rooms are skipped with a
 *     warning, bad requests become REJECTED requests with the reason
 *
 * Every error that reaches the caller is an IOException with a message that
 * can be shown to the user as it is.
 */
public class FileHandler {

    private static final int ROOM_COLUMNS = 3;      // roomNumber,type,gender
    private static final int REQUEST_COLUMNS = 6;   // studentId,name,gender,year,distanceKm,preferredRoom

    private FileHandler() {
        // utility class, no objects needed
    }

    // ==================================================================
    // rooms.csv : roomNumber,type,gender
    // ==================================================================
    public static List<Room> readRooms(Path path) throws IOException {
        List<Room> rooms = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Row row : readRows(path, "roomNumber")) {
            if (row.error != null || row.fields.size() != ROOM_COLUMNS) {
                warn(path, row, row.error != null ? row.error
                        : "expected " + ROOM_COLUMNS + " columns (roomNumber,type,gender), found "
                                + row.fields.size());
                continue;
            }

            String number = row.fields.get(0).toUpperCase();
            RoomType type = RoomType.fromString(row.fields.get(1));
            Gender gender = Gender.fromString(row.fields.get(2));

            if (number.isEmpty()) {
                warn(path, row, "room number is missing");
            } else if (type == null) {
                warn(path, row, "unknown room type '" + row.fields.get(1) + "'");
            } else if (gender == null) {
                warn(path, row, "unknown gender '" + row.fields.get(2) + "' (use MALE or FEMALE)");
            } else if (!seen.add(number)) {
                warn(path, row, "room " + number + " is listed twice, keeping the first");
            } else {
                rooms.add(RoomFactory.create(type, number, gender));
            }
        }
        return rooms;
    }

    // ==================================================================
    // requests.csv : studentId,name,gender,year,distanceKm,preferredRoom
    // ==================================================================
    public static List<AllocationRequest> readRequests(Path path) throws IOException {
        List<AllocationRequest> requests = new ArrayList<>();
        int sequence = 0;   // arrival order, the final tie-breaker when ranking

        for (Row row : readRows(path, "studentId")) {
            sequence++;

            // A line we cannot split into 6 fields still becomes a request, so the
            // report can list it as REJECTED with the reason instead of hiding it.
            if (row.error != null) {
                requests.add(new AllocationRequest(sequence, "LINE-" + row.lineNumber,
                        "Line " + row.lineNumber + " is malformed: " + row.error));
                continue;
            }
            if (row.fields.size() != REQUEST_COLUMNS) {
                requests.add(new AllocationRequest(sequence, "LINE-" + row.lineNumber,
                        "Line " + row.lineNumber + " is malformed: expected " + REQUEST_COLUMNS
                                + " columns, found " + row.fields.size()));
                continue;
            }

            String id = row.fields.get(0).toUpperCase();
            String name = row.fields.get(1);
            Gender gender = Gender.fromString(row.fields.get(2));   // null fails validation
            int year = parseInt(row.fields.get(3), 0);              // 0 fails validation
            double distance = parseDouble(row.fields.get(4), -1);   // -1 fails validation
            String typeText = row.fields.get(5);

            requests.add(new AllocationRequest(new Student(id, name, gender, year, distance),
                    RoomType.fromString(typeText), typeText, sequence));
        }
        return requests;
    }

    // ==================================================================
    // checkouts.csv : one student ID per line (the file is optional)
    // ==================================================================
    public static List<String> readCheckouts(Path path) throws IOException {
        List<String> ids = new ArrayList<>();
        if (!Files.exists(path)) {
            return ids;
        }
        for (Row row : readRows(path, "studentId")) {
            if (row.error != null) {
                warn(path, row, row.error);
                continue;
            }
            String id = row.fields.get(0).toUpperCase();   // extra columns are ignored
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids;
    }

    // ==================================================================
    // report output
    // ==================================================================
    /**
     * Writes the report, creating the folder if needed. The text goes to a
     * temporary file first and is then moved into place, so a crash half way
     * can never leave a half-written report behind.
     */
    public static void writeReport(Path path, String content) throws IOException {
        Path folder = path.toAbsolutePath().getParent();
        Files.createDirectories(folder);

        Path temp = Files.createTempFile(folder, "report", ".tmp");
        try {
            Files.write(temp, content.getBytes(StandardCharsets.UTF_8));
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);   // only does something if the move failed
        }
    }

    // ==================================================================
    // Reading and splitting lines
    // ==================================================================

    /** One usable line of a CSV file: its number, its fields, or why it could not be split. */
    private static final class Row {
        final int lineNumber;          // 1-based, as an editor shows it
        final List<String> fields;     // empty when error is set
        final String error;            // null when the line was split fine

        Row(int lineNumber, List<String> fields, String error) {
            this.lineNumber = lineNumber;
            this.fields = fields;
            this.error = error;
        }
    }

    /** Reads a CSV file and returns only its real data lines (no blanks, comments or header). */
    private static List<Row> readRows(Path path, String headerFirstColumn) throws IOException {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            throw new IOException("File not found: " + path, e);
        } catch (CharacterCodingException e) {
            throw new IOException(path + " is not saved as UTF-8. In Excel use "
                    + "'Save As' > 'CSV UTF-8', or in VS Code use 'Save with Encoding' > UTF-8.", e);
        }

        List<Row> rows = new ArrayList<>();
        boolean firstLineSeen = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (i == 0 && line.startsWith("﻿")) {   // invisible marker that Excel adds
                line = line.substring(1).trim();
            }
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            List<String> fields = splitCsv(line);

            // Only the first real line can be a header
            if (!firstLineSeen) {
                firstLineSeen = true;
                if (fields != null && fields.get(0).equalsIgnoreCase(headerFirstColumn)) {
                    continue;
                }
            }

            if (fields == null) {
                rows.add(new Row(i + 1, new ArrayList<String>(), "a quoted value is never closed"));
            } else {
                rows.add(new Row(i + 1, fields, null));
            }
        }
        return rows;
    }

    /**
     * Splits one CSV line into trimmed fields.
     * Returns null if a quote is opened and never closed.
     * A quote only starts a quoted value at the beginning of a field, so a
     * name like  O"Brien  is left alone.
     */
    private static List<String> splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');   // "" is a literal quote
                        i++;
                    } else {
                        inQuotes = false;      // closing quote
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"' && current.toString().trim().isEmpty()) {
                current.setLength(0);          // opening quote, drop the spaces before it
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (inQuotes) {
            return null;
        }
        fields.add(current.toString().trim());
        return fields;
    }

    // ==================================================================
    // Small helpers
    // ==================================================================
    private static void warn(Path path, Row row, String message) {
        System.err.println("Warning: " + path.getFileName() + " line " + row.lineNumber
                + " skipped: " + message);
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
            double value = Double.parseDouble(text.trim());
            // "NaN" and "Infinity" parse successfully but are not real distances
            return (Double.isNaN(value) || Double.isInfinite(value)) ? fallback : value;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}