/**
 * POLYMORPHISM in action: callers ask for a Room and get back the right
 * subclass. Everything after this point just uses the Room type and never
 * needs to know which concrete class it is dealing with.
 */
public class RoomFactory {

    private RoomFactory() {
        // utility class, no objects needed
    }

    public static Room create(RoomType type, String roomNumber) {
        switch (type) {
            case SINGLE:
                return new SingleRoom(roomNumber);
            case DOUBLE:
                return new DoubleRoom(roomNumber);
            case TRIPLE:
                return new TripleRoom(roomNumber);
            default:
                throw new IllegalArgumentException("Unsupported room type: " + type);
        }
    }
}