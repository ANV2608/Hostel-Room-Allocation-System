import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the text report (assigned / waitlisted / rejected) from the
 * current state of the HostelManager.
 */
public class ReportGenerator {

    private ReportGenerator() {
    }

    public static String build(String title, HostelManager manager) {
        StringBuilder sb = new StringBuilder();
        String bar = repeat('=', 90);
        String thin = repeat('-', 90);

        List<AllocationRequest> assigned = manager.getRequestsByStatus(RequestStatus.ASSIGNED);
        List<AllocationRequest> rejected = manager.getRequestsByStatus(RequestStatus.REJECTED);
        List<AllocationRequest> vacated = manager.getRequestsByStatus(RequestStatus.VACATED);

        // One waitlist exists for every room type and gender
        List<AllocationRequest> waitlisted = new ArrayList<>();
        for (RoomType type : RoomType.values()) {
            for (Gender gender : Gender.values()) {
                waitlisted.addAll(manager.getWaitlistSnapshot(type, gender));
            }
        }

        // Assigned students are listed room by room
        Collections.sort(assigned, new Comparator<AllocationRequest>() {
            @Override
            public int compare(AllocationRequest a, AllocationRequest b) {
                return a.getAssignedRoom().getRoomNumber()
                        .compareTo(b.getAssignedRoom().getRoomNumber());
            }
        });

        sb.append(bar).append('\n');
        sb.append(title).append('\n');
        sb.append(bar).append('\n');
        sb.append(String.format("Total requests: %d | Assigned: %d | Waitlisted: %d | Rejected: %d",
                manager.getTotalRequests(), assigned.size(), waitlisted.size(), rejected.size()));
        if (!vacated.isEmpty()) {
            sb.append(String.format(" | Checked out: %d", vacated.size()));
        }
        sb.append("\n\n");

        // ---- Room occupancy ----
        sb.append("ROOM OCCUPANCY\n").append(thin).append('\n');
        sb.append(String.format("%-8s %-8s %-8s %-9s %-8s %s%n",
                "Room", "Type", "Gender", "Beds", "Rent", "Occupants"));
        for (RoomType type : RoomType.values()) {
            for (Room room : manager.getRooms(type)) {
                StringBuilder names = new StringBuilder();
                for (Student s : room.getOccupants()) {
                    if (names.length() > 0) {
                        names.append(", ");
                    }
                    names.append(s.getId());
                }
                sb.append(String.format("%-8s %-8s %-8s %-9s Rs.%-5d %s%n",
                        room.getRoomNumber(), room.getType(), room.getGender(),
                        room.getOccupiedBeds() + "/" + room.getCapacity(),
                        room.getMonthlyRent(),
                        names.length() == 0 ? "(empty)" : names.toString()));
            }
        }
        sb.append('\n');

        // ---- Assigned ----
        sb.append("ASSIGNED (").append(assigned.size()).append(")\n").append(thin).append('\n');
        sb.append(String.format("%-8s %-18s %-8s %-5s %-10s %-6s %s%n",
                "ID", "Name", "Gender", "Year", "Dist (km)", "Room", "Type"));
        for (AllocationRequest r : assigned) {
            Student s = r.getStudent();
            sb.append(String.format("%-8s %-18s %-8s %-5d %-10.1f %-6s %s%n",
                    s.getId(), s.getName(), s.getGender(), s.getYear(), s.getDistanceKm(),
                    r.getAssignedRoom().getRoomNumber(), r.getAssignedRoom().getType()));
        }
        if (assigned.isEmpty()) {
            sb.append("(none)\n");
        }
        sb.append('\n');

        // ---- Waitlisted ----
        sb.append("WAITLISTED (").append(waitlisted.size()).append(")\n").append(thin).append('\n');
        sb.append(String.format("%-8s %-6s %-18s %-8s %-5s %-10s %s%n",
                "ID", "Queue", "Name", "Gender", "Year", "Dist (km)", "Waiting for"));
        for (RoomType type : RoomType.values()) {
            for (Gender gender : Gender.values()) {
                int position = 1;
                for (AllocationRequest r : manager.getWaitlistSnapshot(type, gender)) {
                    Student s = r.getStudent();
                    sb.append(String.format("%-8s #%-5d %-18s %-8s %-5d %-10.1f %s%n",
                            s.getId(), position++, s.getName(), s.getGender(), s.getYear(),
                            s.getDistanceKm(), type));
                }
            }
        }
        if (waitlisted.isEmpty()) {
            sb.append("(none)\n");
        }
        sb.append('\n');

        // ---- Rejected ----
        sb.append("REJECTED (").append(rejected.size()).append(")\n").append(thin).append('\n');
        sb.append(String.format("%-8s %-18s %s%n", "ID", "Name", "Reason"));
        for (AllocationRequest r : rejected) {
            sb.append(String.format("%-8s %-18s %s%n",
                    r.getStudent().getId(), r.getStudent().getName(), r.getRemark()));
        }
        if (rejected.isEmpty()) {
            sb.append("(none)\n");
        }
        sb.append('\n');

        // ---- Checked out ----
        if (!vacated.isEmpty()) {
            sb.append("CHECKED OUT (").append(vacated.size()).append(")\n").append(thin).append('\n');
            for (AllocationRequest r : vacated) {
                sb.append(String.format("%-8s %-18s %s%n",
                        r.getStudent().getId(), r.getStudent().getName(), r.getRemark()));
            }
            sb.append('\n');
        }

        // ---- Event log ----
        sb.append("EVENT LOG\n").append(thin).append('\n');
        int step = 1;
        for (String event : manager.getEventLog()) {
            sb.append(String.format("%3d. %s%n", step++, event));
        }
        sb.append(bar).append('\n');

        return sb.toString();
    }

    // String.repeat() needs Java 11, so this small helper keeps Java 8 working too
    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}