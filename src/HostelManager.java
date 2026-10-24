import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * The brain of the system.
 *
 * Collections used:
 *   HashMap<RoomType, List<Room>>                 rooms grouped by type
 *   HashMap<Gender, HashMap<RoomType, PriorityQueue>>
 *                                                 one waitlist for every gender and
 *                                                 room type, always ordered by
 *                                                 PriorityComparator
 *   HashMap<String, AllocationRequest>            quick lookup by student ID (for check-outs)
 *   ArrayList                                     all requests and the event log
 *
 * Gender rule: a student is only ever placed in a room of the same gender,
 * and waits in the waitlist of their own gender. A free bed in a girls' room
 * is never offered to a boy, and the other way round.
 *
 * Workflow of processRequests():
 *   1. validate each request  -> invalid ones become REJECTED
 *   2. sort the eligible ones with PriorityComparator
 *   3. try to give each a room of the preferred type and own gender,
 *      otherwise WAITLIST
 *
 * Workflow of checkout():
 *   1. free the student's bed
 *   2. promote the highest-priority waitlisted student of the same
 *      room type and gender
 */
public class HostelManager {
    private final Map<RoomType, List<Room>> roomsByType = new HashMap<>();
    private final Map<Gender, Map<RoomType, PriorityQueue<AllocationRequest>>> waitlists = new HashMap<>();
    private final Map<String, AllocationRequest> requestsById = new HashMap<>();
    private final List<AllocationRequest> allRequests = new ArrayList<>();
    private final List<String> eventLog = new ArrayList<>();
    private final Comparator<AllocationRequest> priority = new PriorityComparator();
    // Kept as a field so duplicate IDs are caught even across several batches
    private final RequestValidator validator = new RequestValidator();

    public HostelManager() {
        for (RoomType type : RoomType.values()) {
            roomsByType.put(type, new ArrayList<Room>());
        }
        for (Gender gender : Gender.values()) {
            Map<RoomType, PriorityQueue<AllocationRequest>> byType = new HashMap<>();
            for (RoomType type : RoomType.values()) {
                byType.put(type, new PriorityQueue<AllocationRequest>(priority));
            }
            waitlists.put(gender, byType);
        }
    }

    private PriorityQueue<AllocationRequest> waitlist(Gender gender, RoomType type) {
        return waitlists.get(gender).get(type);
    }

    // ------------------------------------------------------------------
    // Setup
    // ------------------------------------------------------------------
    // A new room is new free beds, so anyone of the same gender waiting for
    // that room type is promoted straight away. Returns who was promoted.
    public List<AllocationRequest> addRoom(Room room) {
        roomsByType.get(room.getType()).add(room);
        List<AllocationRequest> promoted = new ArrayList<>();
        promoteFromWaitlist(room.getGender(), room.getType(), promoted);
        return promoted;
    }

    // ------------------------------------------------------------------
    // Step 1-3: filter, rank, allocate
    // ------------------------------------------------------------------
    public void processRequests(List<AllocationRequest> requests) {
        List<AllocationRequest> eligible = new ArrayList<>();

        // Filter: keep the eligible, reject the rest
        for (AllocationRequest request : requests) {
            allRequests.add(request);
            String reason = validator.validate(request);
            if (reason == null) {
                eligible.add(request);
                requestsById.put(request.getStudent().getId(), request);
            } else {
                request.markRejected(reason);
                log("REJECTED   " + request.getStudent().getId() + " - " + reason);
            }
        }

        // Rank: custom comparator decides the order
        Collections.sort(eligible, priority);

        // Allocate: best-ranked student picks first
        for (AllocationRequest request : eligible) {
            Student student = request.getStudent();
            if (tryAssign(request)) {
                log("ASSIGNED   " + student.getId() + " -> room "
                        + request.getAssignedRoom().getRoomNumber()
                        + " (" + request.getPreferredType() + ", " + student.getGender() + ")");
            } else {
                request.markWaitlisted();
                waitlist(student.getGender(), request.getPreferredType()).add(request);
                log("WAITLISTED " + student.getId() + " - no "
                        + request.getPreferredType() + " room free for " + student.getGender());
            }
        }
    }

    // Looks for a free bed of the preferred type in a room for the student's gender
    private boolean tryAssign(AllocationRequest request) {
        for (Room room : roomsByType.get(request.getPreferredType())) {
            if (room.canAccept(request.getStudent())) {
                room.addOccupant(request.getStudent());
                request.markAssigned(room);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Check-out and automatic waitlist promotion
    // ------------------------------------------------------------------
    public List<AllocationRequest> checkout(String studentId) {
        List<AllocationRequest> promoted = new ArrayList<>();
        String id = studentId.trim().toUpperCase();
        AllocationRequest request = requestsById.get(id);

        if (request == null || request.getStatus() != RequestStatus.ASSIGNED) {
            log("CHECKOUT IGNORED " + id + " - student does not currently hold a room");
            return promoted;
        }

        Room room = request.getAssignedRoom();
        room.removeOccupant(request.getStudent());
        request.markVacated();
        log("CHECKED OUT " + id + " left room " + room.getRoomNumber());

        promoteFromWaitlist(room.getGender(), room.getType(), promoted);
        return promoted;
    }

    // Keeps promoting while there is both a free bed and someone waiting
    private void promoteFromWaitlist(Gender gender, RoomType type, List<AllocationRequest> promoted) {
        PriorityQueue<AllocationRequest> queue = waitlist(gender, type);
        while (!queue.isEmpty()) {
            AllocationRequest next = queue.peek();   // highest priority waiting
            if (!tryAssign(next)) {
                break;                               // no bed free, stop
            }
            queue.poll();                            // remove from waitlist
            promoted.add(next);
            log("PROMOTED   " + next.getStudent().getId() + " from waitlist -> room "
                    + next.getAssignedRoom().getRoomNumber() + " (" + type + ", " + gender + ")");
        }
    }

    // ------------------------------------------------------------------
    // Read-only views used by the report
    // ------------------------------------------------------------------
    public List<AllocationRequest> getRequestsByStatus(RequestStatus status) {
        List<AllocationRequest> result = new ArrayList<>();
        for (AllocationRequest request : allRequests) {
            if (request.getStatus() == status) {
                result.add(request);
            }
        }
        return result;
    }

    public List<AllocationRequest> getWaitlistSnapshot(RoomType type, Gender gender) {
        List<AllocationRequest> snapshot = new ArrayList<>(waitlist(gender, type));
        Collections.sort(snapshot, priority);   // PriorityQueue iteration is not sorted
        return snapshot;
    }

    public List<Room> getRooms(RoomType type) {
        return Collections.unmodifiableList(roomsByType.get(type));
    }

    public int getTotalRequests() {
        return allRequests.size();
    }

    public List<String> getEventLog() {
        return Collections.unmodifiableList(eventLog);
    }

    private void log(String message) {
        eventLog.add(message);
    }
}