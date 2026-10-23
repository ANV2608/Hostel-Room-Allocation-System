import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Web front end for the hostel allocation system.
 *
 * Uses the JDK's built-in HTTP server (no extra libraries) to
 *   1. serve the web page from the web/ folder, and
 *   2. expose a small JSON API that calls the SAME classes the console
 *      program uses: HostelManager, FileHandler, ReportGenerator ...
 *
 * Start it from the project root:   java -cp out WebApp [port]
 * Then open http://localhost:8080
 */
public class WebApp {
    private static final String DATA_DIR = "data";
    private static final String WEB_DIR = "web";
    private static final String OUT_DIR = "output";

    private HostelManager manager = new HostelManager();
    private final List<AllocationRequest> pending = new ArrayList<>();
    private int nextSequence = 1;

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("'" + args[0] + "' is not a valid port, using 8080.");
            }
        }
        new WebApp().start(port);
    }

    private void start(int port) throws IOException {
        // Bound to the loopback address, so only this computer can reach it
        HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/api/", this::handleApi);
        server.createContext("/", this::handleStatic);
        server.start();
        System.out.println("Hostel Allocation web app is running.");
        System.out.println("Open http://localhost:" + port + " in your browser. Press Ctrl+C to stop.");
    }

    // ==================================================================
    // API routing
    // ==================================================================
    private synchronized void handleApi(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        try {
            if (path.equals("/api/state") && method.equals("GET")) {
                sendJson(ex, 200, "{\"ok\":true,\"state\":" + stateJson() + "}");
                return;
            }
            if (path.equals("/api/report") && method.equals("GET")) {
                String report = ReportGenerator.build("HOSTEL ALLOCATION REPORT - web session", manager);
                ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                ex.getResponseHeaders().set("Content-Disposition",
                        "attachment; filename=\"hostel_report.txt\"");
                send(ex, 200, report.getBytes(StandardCharsets.UTF_8));
                return;
            }

            if (!method.equals("POST")) {
                throw new ApiException(405, "Use POST for " + path);
            }
            // Browsers cannot add a custom header to a cross-site form post, so this
            // blocks other websites from driving the API while it runs on your PC.
            if (!"hostel-ui".equals(ex.getRequestHeaders().getFirst("X-Requested-With"))) {
                throw new ApiException(403, "Missing X-Requested-With header");
            }

            Map<String, String> form = parseForm(readBody(ex.getRequestBody()));
            String message;
            switch (path) {
                case "/api/room":
                    message = addRoom(form);
                    break;
                case "/api/request":
                    message = addRequest(form);
                    break;
                case "/api/process":
                    message = processPending();
                    break;
                case "/api/checkout":
                    message = checkout(form);
                    break;
                case "/api/load-sample":
                    message = loadSample();
                    break;
                case "/api/save-report":
                    message = saveReport();
                    break;
                case "/api/reset":
                    message = reset();
                    break;
                default:
                    throw new ApiException(404, "Unknown endpoint " + path);
            }
            sendJson(ex, 200, "{\"ok\":true,\"message\":" + q(message)
                    + ",\"state\":" + stateJson() + "}");

        } catch (ApiException e) {
            sendJson(ex, e.status, "{\"ok\":false,\"message\":" + q(e.getMessage()) + "}");
        } catch (RuntimeException e) {
            e.printStackTrace();
            sendJson(ex, 500, "{\"ok\":false,\"message\":" + q("Unexpected server error: " + e) + "}");
        }
    }

    // ==================================================================
    // Actions (each returns a message for the toast in the browser)
    // ==================================================================
    private String addRoom(Map<String, String> f) {
        String number = value(f, "roomNumber").toUpperCase();
        RoomType type = RoomType.fromString(value(f, "type"));

        if (number.isEmpty()) {
            throw new ApiException(400, "Room number is required");
        }
        if (type == null) {
            throw new ApiException(400, "Choose a valid room type");
        }
        for (RoomType t : RoomType.values()) {
            for (Room room : manager.getRooms(t)) {
                if (room.getRoomNumber().equals(number)) {
                    throw new ApiException(400, "Room " + number + " already exists");
                }
            }
        }
        List<AllocationRequest> promoted = manager.addRoom(RoomFactory.create(type, number));
        if (promoted.isEmpty()) {
            return "Room " + number + " (" + type + ") added";
        }
        return "Room " + number + " added. Promoted from waitlist: " + describePromotions(promoted);
    }

    // "S002 -> S101, S004 -> S102"
    private static String describePromotions(List<AllocationRequest> promoted) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < promoted.size(); i++) {
            AllocationRequest p = promoted.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(p.getStudent().getId()).append(" -> ")
                    .append(p.getAssignedRoom().getRoomNumber());
        }
        return sb.toString();
    }

    // Bad values are accepted here on purpose: the validator rejects them
    // later and the report shows the reason.
    private String addRequest(Map<String, String> f) {
        String id = value(f, "studentId").toUpperCase();
        String name = value(f, "name");
        int year = parseInt(value(f, "year"), 0);
        double distance = parseDouble(value(f, "distanceKm"), -1);
        String typeText = value(f, "preferredRoom");
        RoomType type = RoomType.fromString(typeText);

        pending.add(new AllocationRequest(new Student(id, name, year, distance),
                type, typeText, nextSequence++));
        return "Request for " + (id.isEmpty() ? "(no ID)" : id)
                + " queued (" + pending.size() + " pending)";
    }

    private String processPending() {
        if (pending.isEmpty()) {
            throw new ApiException(400, "No pending requests to process");
        }
        int count = pending.size();
        manager.processRequests(new ArrayList<>(pending));
        pending.clear();
        return "Processed " + count + " request(s)";
    }

    private String checkout(Map<String, String> f) {
        String id = value(f, "studentId").toUpperCase();
        if (id.isEmpty()) {
            throw new ApiException(400, "Student ID is required");
        }

        boolean holdsRoom = false;
        for (AllocationRequest r : manager.getRequestsByStatus(RequestStatus.ASSIGNED)) {
            if (r.getStudent().getId().equals(id)) {
                holdsRoom = true;
                break;
            }
        }
        if (!holdsRoom) {
            throw new ApiException(400, id + " does not currently hold a room");
        }

        List<AllocationRequest> promoted = manager.checkout(id);
        if (promoted.isEmpty()) {
            return id + " checked out (nobody was waiting for that room type)";
        }
        return id + " checked out. Promoted from waitlist: " + describePromotions(promoted);
    }

    private String loadSample() {
        try {
            HostelManager fresh = new HostelManager();
            int roomCount = 0;
            for (Room room : FileHandler.readRooms(Paths.get(DATA_DIR, "rooms.csv"))) {
                fresh.addRoom(room);
                roomCount++;
            }
            List<AllocationRequest> requests =
                    FileHandler.readRequests(Paths.get(DATA_DIR, "requests.csv"));

            manager = fresh;
            pending.clear();
            pending.addAll(requests);
            nextSequence = requests.size() + 1;
            return "Loaded " + roomCount + " rooms and " + requests.size()
                    + " pending requests. Click 'Run allocation'.";
        } catch (IOException e) {
            throw new ApiException(500, "Could not read the sample files from the '"
                    + DATA_DIR + "' folder: " + e.getMessage());
        }
    }

    private String saveReport() {
        try {
            Path file = Paths.get(OUT_DIR, "web_report.txt");
            FileHandler.writeReport(file,
                    ReportGenerator.build("HOSTEL ALLOCATION REPORT - web session", manager));
            return "Report saved to " + file;
        } catch (IOException e) {
            throw new ApiException(500, "Could not save the report: " + e.getMessage());
        }
    }

    private String reset() {
        manager = new HostelManager();
        pending.clear();
        nextSequence = 1;
        return "Everything cleared";
    }

    // ==================================================================
    // JSON output (hand-written so no library is needed)
    // ==================================================================
    private String stateJson() {
        List<AllocationRequest> assigned = manager.getRequestsByStatus(RequestStatus.ASSIGNED);
        List<AllocationRequest> rejected = manager.getRequestsByStatus(RequestStatus.REJECTED);
        List<AllocationRequest> vacated = manager.getRequestsByStatus(RequestStatus.VACATED);

        int totalBeds = 0;
        int freeBeds = 0;
        StringBuilder rooms = new StringBuilder("[");
        boolean firstRoom = true;
        for (RoomType type : RoomType.values()) {
            for (Room room : manager.getRooms(type)) {
                totalBeds += room.getCapacity();
                freeBeds += room.getVacantBeds();
                if (!firstRoom) {
                    rooms.append(',');
                }
                firstRoom = false;
                rooms.append(roomJson(room));
            }
        }
        rooms.append(']');

        StringBuilder waitlist = new StringBuilder("[");
        int waitCount = 0;
        for (RoomType type : RoomType.values()) {
            int rank = 1;
            for (AllocationRequest r : manager.getWaitlistSnapshot(type)) {
                if (waitCount++ > 0) {
                    waitlist.append(',');
                }
                waitlist.append(requestJson(r, rank++));
            }
        }
        waitlist.append(']');

        StringBuilder log = new StringBuilder("[");
        List<String> events = manager.getEventLog();
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) {
                log.append(',');
            }
            log.append(q(events.get(i)));
        }
        log.append(']');

        return "{\"summary\":{"
                + "\"pending\":" + pending.size()
                + ",\"processed\":" + manager.getTotalRequests()
                + ",\"assigned\":" + assigned.size()
                + ",\"waitlisted\":" + waitCount
                + ",\"rejected\":" + rejected.size()
                + ",\"checkedOut\":" + vacated.size()
                + ",\"totalBeds\":" + totalBeds
                + ",\"freeBeds\":" + freeBeds + "}"
                + ",\"rooms\":" + rooms
                + ",\"pending\":" + requestList(pending)
                + ",\"assigned\":" + requestList(assigned)
                + ",\"waitlist\":" + waitlist
                + ",\"rejected\":" + requestList(rejected)
                + ",\"vacated\":" + requestList(vacated)
                + ",\"log\":" + log + "}";
    }

    private String roomJson(Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"roomNumber\":").append(q(room.getRoomNumber()))
                .append(",\"type\":").append(q(room.getType().name()))
                .append(",\"capacity\":").append(room.getCapacity())
                .append(",\"occupied\":").append(room.getOccupiedBeds())
                .append(",\"rent\":").append(room.getMonthlyRent())
                .append(",\"facilities\":").append(q(room.getFacilities()))
                .append(",\"occupants\":[");
        List<Student> people = room.getOccupants();
        for (int i = 0; i < people.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"id\":").append(q(people.get(i).getId()))
                    .append(",\"name\":").append(q(people.get(i).getName())).append('}');
        }
        return sb.append("]}").toString();
    }

    private String requestList(List<AllocationRequest> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(requestJson(list.get(i), 0));
        }
        return sb.append(']').toString();
    }

    private String requestJson(AllocationRequest r, int rank) {
        Student s = r.getStudent();
        Room room = r.getAssignedRoom();
        String wants = r.getPreferredType() != null ? r.getPreferredType().name() : r.getPreferredText();
        return "{\"id\":" + q(s.getId())
                + ",\"name\":" + q(s.getName())
                + ",\"year\":" + s.getYear()
                + ",\"distanceKm\":" + s.getDistanceKm()
                + ",\"wants\":" + q(wants)
                + ",\"room\":" + (room == null ? "null" : q(room.getRoomNumber()))
                + ",\"roomType\":" + (room == null ? "null" : q(room.getType().name()))
                + ",\"remark\":" + q(r.getRemark())
                + ",\"rank\":" + rank + "}";
    }

    // Escapes a Java string into a quoted JSON string
    private static String q(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    // ==================================================================
    // Static files (the web page itself)
    // ==================================================================
    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }

        Path base = Paths.get(WEB_DIR).toAbsolutePath().normalize();
        Path file = base.resolve(path.substring(1)).normalize();

        // Refuse anything outside web/ (e.g. /../src/Main.java)
        if (!file.startsWith(base) || !Files.isRegularFile(file)) {
            String hint = Files.isDirectory(base)
                    ? "Not found: " + path
                    : "The 'web' folder was not found. Run the program from the project root folder.";
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            send(ex, 404, hint.getBytes(StandardCharsets.UTF_8));
            return;
        }

        ex.getResponseHeaders().set("Content-Type", contentType(file.toString()));
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        send(ex, 200, Files.readAllBytes(file));
    }

    private static String contentType(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (n.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (n.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (n.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        if (n.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (n.endsWith(".png")) {
            return "image/png";
        }
        return "application/octet-stream";
    }

    // ==================================================================
    // Small HTTP helpers
    // ==================================================================
    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        send(ex, status, json.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int status, byte[] body) throws IOException {
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    // Java 8 has no InputStream.readAllBytes(), so read in a loop (max 1 MB)
    private static String readBody(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
            if (out.size() > 1_000_000) {
                throw new ApiException(413, "Request body too large");
            }
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> map = new HashMap<>();
        if (body.isEmpty()) {
            return map;
        }
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String val = eq < 0 ? "" : pair.substring(eq + 1);
            map.put(decode(key), decode(val));
        }
        return map;
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);   // UTF-8 always exists
        }
    }

    private static String value(Map<String, String> form, String key) {
        String v = form.get(key);
        return v == null ? "" : v.trim();
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
            return (Double.isNaN(value) || Double.isInfinite(value)) ? fallback : value;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** An error that is sent back to the browser as {"ok":false,"message":...}. */
    private static class ApiException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final int status;

        ApiException(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}