import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ABSTRACTION : Room says WHAT every room can do (capacity, rent, facilities)
 *               but leaves HOW it differs to the subclasses.
 * ENCAPSULATION: the occupant list is private. It can only change through
 *               addOccupant / removeOccupant, which enforce the capacity rule
 *               and the gender rule, so a room can never hold the wrong person.
 * POLYMORPHISM : describe() calls getCapacity(), getMonthlyRent() etc. and
 *               Java picks the right subclass version at run time.
 */
public abstract class Room {
    private final String roomNumber;
    private final Gender gender;   // the gender this room is for
    private final List<Student> occupants = new ArrayList<>();

    protected Room(String roomNumber, Gender gender) {
        this.roomNumber = roomNumber;
        this.gender = gender;
    }

    // ---- to be defined by each concrete room (INHERITANCE) ----
    public abstract RoomType getType();

    public abstract int getCapacity();

    public abstract int getMonthlyRent();

    public abstract String getFacilities();

    // ---- shared behaviour, written once, inherited by all rooms ----
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

    /** True if this student may be placed here: right gender, a free bed, not already inside. */
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