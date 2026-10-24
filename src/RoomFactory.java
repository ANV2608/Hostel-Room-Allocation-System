
/**
 * POLYMORPHISM in action: callers ask for a Room and get back the right
 * subclass. Everything after this point uses the Room type.
 */
public class RoomFactory {

    private RoomFactory() {
        // Utility class; no objects needed
    }

    public static Room create(RoomType type, String roomNumber, Gender gender) {
        switch (type) {
            case SINGLE:
                return new SingleRoom(roomNumber, gender);

            case DOUBLE:
                return new DoubleRoom(roomNumber, gender);

            case TRIPLE:
                return new TripleRoom(roomNumber, gender);

            default:
                throw new IllegalArgumentException(
                        "Unsupported room type: " + type
                );
        }
    }
}
