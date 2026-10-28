
public class AllocationRequest {

    private final Student student;
    private final RoomType preferredType;
    private final String preferredText;
    private final int sequence;
    private final String parseError;

    private RequestStatus status = RequestStatus.PENDING;
    private Room assignedRoom;
    private String remark = "";


    public AllocationRequest(Student student, RoomType preferredType,
                             String preferredText, int sequence) {
        this.student = student;
        this.preferredType = preferredType;
        this.preferredText = preferredText;
        this.sequence = sequence;
        this.parseError = null;
    }

    public AllocationRequest(int sequence, String placeholderId,
                             String parseError) {
        this.student = new Student(
                placeholderId,
                "(unreadable line)",
                null,
                0,
                -1.0
        );

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
        this.remark = "No " + preferredType
                + " room available for " + student.getGender();
    }

    public void markRejected(String reason) {
        this.status = RequestStatus.REJECTED;
        this.assignedRoom = null;
        this.remark = reason;
    }

    public void markVacated() {
        this.status = RequestStatus.VACATED;
        this.remark = "Checked out of room "
                + assignedRoom.getRoomNumber();
        this.assignedRoom = null;
    }
}
