
public class DoubleRoom extends Room {

    public DoubleRoom(String roomNumber, Gender gender) {
        super(roomNumber, gender);
    }

    @Override
    public RoomType getType() {
        return RoomType.DOUBLE;
    }

    @Override
    public int getCapacity() {
        return 2;
    }

    @Override
    public int getMonthlyRent() {
        return 4500;
    }

    @Override
    public String getFacilities() {
        return "attached bath";
    }
}