/** INHERITANCE: SingleRoom IS-A Room. It only supplies what is different. */
public class SingleRoom extends Room {

    public SingleRoom(String roomNumber) {
        super(roomNumber);
    }

    @Override
    public RoomType getType() {
        return RoomType.SINGLE;
    }

    @Override
    public int getCapacity() {
        return 1;
    }

    @Override
    public int getMonthlyRent() {
        return 6000;
    }

    @Override
    public String getFacilities() {
        return "attached bath, AC";
    }
}