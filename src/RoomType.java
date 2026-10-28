
public enum RoomType {
    SINGLE,
    DOUBLE,
    TRIPLE;

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