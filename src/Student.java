
public class Student {
    private final String id;
    private final String name;
    private final Gender gender;     
    private final int year;          
    private final double distanceKm; 

    public Student(String id, String name, Gender gender, int year, double distanceKm) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.year = year;
        this.distanceKm = distanceKm;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Gender getGender() {
        return gender;
    }

    public int getYear() {
        return year;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    
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
        return id + " (" + name + ", " + gender + ", year " + year + ", " + distanceKm + " km)";
    }
}