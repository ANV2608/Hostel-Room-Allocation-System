
public class TripleRoom extends Room {

    public TripleRoom(String roomNumber, Gender gender) {
        super(roomNumber, gender);
    }

    @Override
    public RoomType getType() {
        return RoomType.TRIPLE;
    }

    @Override
    public int getCapacity() {
        return 3;
    }

    @Override
    public int getMonthlyRent() {
        return 3000;
    }

    @Override
    public String getFacilities() {
        return "shared bath";
    }
}