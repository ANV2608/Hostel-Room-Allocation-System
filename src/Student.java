/**
 * ENCAPSULATION: all fields are private and final. The outside world can
 * only read them through getters, so a Student can never be changed
 * accidentally after it has been created.
 */
public class Student {
    private final String id;
    private final String name;
    private final int year;          // year of study (1-4)
    private final double distanceKm; // distance of home from the campus

    public Student(String id, String name, int year, double distanceKm) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.distanceKm = distanceKm;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getYear() {
        return year;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    // Two students are the same person if their IDs match.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Student)) {
            return false;
        }
        return id.equals(((Student) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " (" + name + ", year " + year + ", " + distanceKm + " km)";
    }
}