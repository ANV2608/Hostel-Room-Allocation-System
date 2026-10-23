import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ABSTRACTION : Room says WHAT every room can do (capacity, rent, facilities)
 *               but leaves HOW it differs to the subclasses.
 * ENCAPSULATION: the occupant list is private. It can only change through
 *               addOccupant / removeOccupant, which enforce the capacity rule.
 * POLYMORPHISM : describe() calls getCapacity(), getMonthlyRent() etc. and
 *               Java picks the right subclass version at run time.
 */
public abstract class Room {
    private final String roomNumber;
    private final List<Student> occupants = new ArrayList<>();

    protected Room(String roomNumber) {
        this.roomNumber = roomNumber;
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

    public boolean addOccupant(Student student) {
        if (!hasVacancy() || occupants.contains(student)) {
            return false;
        }
        return occupants.add(student);
    }

    public boolean removeOccupant(Student student) {
        return occupants.remove(student);
    }

    public String describe() {
        return String.format("%s [%s] %d/%d beds, Rs.%d/month, %s",
                roomNumber, getType(), getOccupiedBeds(), getCapacity(),
                getMonthlyRent(), getFacilities());
    }
}