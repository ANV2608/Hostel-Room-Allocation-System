
public enum Gender {
    MALE,
    FEMALE;

 
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

    public String wing() {
        return this == MALE ? "Boys" : "Girls";
    }
}