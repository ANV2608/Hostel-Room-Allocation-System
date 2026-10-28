import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Room {
    private final String roomNumber;
    private final Gender gender;  
    private final List<Student> occupants = new ArrayList<>();

    protected Room(String roomNumber, Gender gender) {
        this.roomNumber = roomNumber;
        this.gender = gender;
    }

    
    public abstract RoomType getType();

    public abstract int getCapacity();

    public abstract int getMonthlyRent();

    public abstract String getFacilities();

    
    public String getRoomNumber() {
        return roomNumber;
    }

    public Gender getGender() {
        return gender;
    }

    public List<Student> getOccupants() {
        return Collections.unmodifiableList(occupants);
    }

    public int getOccupiedBeds() {
        return occupants.size();
    }

    public int getVacantBeds() {
        return getCapacity() - occupants.size();
    }

    public boolean hasVacancy() {
        return getVacantBeds() > 0;
    }

    
    public boolean canAccept(Student student) {
        return student.getGender() == gender
                && hasVacancy()
                && !occupants.contains(student);
    }

    public boolean addOccupant(Student student) {
        if (!canAccept(student)) {
            return false;
        }
        return occupants.add(student);
    }

    public boolean removeOccupant(Student student) {
        return occupants.remove(student);
    }

    public String describe() {
        return String.format("%s [%s, %s] %d/%d beds, Rs.%d/month, %s",
                roomNumber, getType(), gender, getOccupiedBeds(), getCapacity(),
                getMonthlyRent(), getFacilities());
    }
}