
/**
 * The kinds of rooms the hostel offers.
 * An enum is used so that room types can never be misspelled in code.
 */
public enum RoomType {
    SINGLE,
    DOUBLE,
    TRIPLE;

    /**
     * Converts text from a file (e.g. " double ") into a RoomType.
     * Returns null when the text is not a valid room type, so the
     * validator can reject the request with a clear reason.
     */
    public static RoomType fromString(String text) {
        if (text == null) {
            return null;
        }
        try {
            return RoomType.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}