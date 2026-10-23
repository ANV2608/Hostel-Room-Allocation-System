/**
 * One student's request for a room, plus what has happened to it so far.
 * Status, assigned room and remark can only be changed through the mark...()
 * methods, which keeps the three fields consistent with each other.
 */
public class AllocationRequest {
    private final Student student;
    private final RoomType preferredType;   // null if the text in the file was invalid
    private final String preferredText;     // raw text, used in rejection messages
    private final int sequence;             // arrival order, used as the final tie-breaker
    private final String parseError;        // set when the line in the file was malformed

    private RequestStatus status = RequestStatus.PENDING;
    private Room assignedRoom;
    private String remark = "";

    // A normally parsed request
    public AllocationRequest(Student student, RoomType preferredType,
                             String preferredText, int sequence) {
        this.student = student;
        this.preferredType = preferredType;
        this.preferredText = preferredText;
        this.sequence = sequence;
        this.parseError = null;
    }

    // A line that could not be understood at all (e.g. wrong number of columns)
    public AllocationRequest(int sequence, String placeholderId, String parseError) {
        this.student = new Student(placeholderId, "(unreadable line)", 0, -1);
        this.preferredType = null;
        this.preferredText = "";
        this.sequence = sequence;
        this.parseError = parseError;
    }

    public Student getStudent() {
        return student;
    }

    public RoomType getPreferredType() {
        return preferredType;
    }

    public String getPreferredText() {
        return preferredText;
    }

    public int getSequence() {
        return sequence;
    }

    public String getParseError() {
        return parseError;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Room getAssignedRoom() {
        return assignedRoom;
    }

    public String getRemark() {
        return remark;
    }

    public void markAssigned(Room room) {
        this.status = RequestStatus.ASSIGNED;
        this.assignedRoom = room;
        this.remark = "Assigned to room " + room.getRoomNumber();
    }

    public void markWaitlisted() {
        this.status = RequestStatus.WAITLISTED;
        this.assignedRoom = null;
        this.remark = "No " + preferredType + " room available";
    }

    public void markRejected(String reason) {
        this.status = RequestStatus.REJECTED;
        this.assignedRoom = null;
        this.remark = reason;
    }

    public void markVacated() {
        this.status = RequestStatus.VACATED;
        this.remark = "Checked out of room " + assignedRoom.getRoomNumber();
    }
}