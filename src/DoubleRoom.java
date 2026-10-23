/** INHERITANCE: DoubleRoom IS-A Room that sleeps two students. */
public class DoubleRoom extends Room {

    public DoubleRoom(String roomNumber) {
        super(roomNumber);
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