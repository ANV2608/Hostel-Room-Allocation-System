/**
 * Gender of a student, and of a room.
 * A room belongs to one gender only (a boys' room or a girls' room), and a
 * student can only be placed in a room of the same gender.
 */
public enum Gender {
    MALE,
    FEMALE;

    /**
     * Converts text from a file (e.g. " f ", "Male") into a Gender.
     * Accepts M, F, MALE and FEMALE in any case.
     * Returns null for anything else, so the validator can reject the
     * request with a clear reason.
     */
    public static Gender fromString(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim().toUpperCase();
        if (t.equals("M") || t.equals("MALE")) {
            return MALE;
        }
        if (t.equals("F") || t.equals("FEMALE")) {
            return FEMALE;
        }
        return null;
    }

    /** "Boys" or "Girls", used in headings. */
    public String wing() {
        return this == MALE ? "Boys" : "Girls";
    }
}