import java.util.HashSet;
import java.util.Set;

/**
 * Decides whether a request is eligible for allocation.
 * validate() returns null when the request is fine, or a human readable
 * rejection reason when it is not.
 *
 * A HashSet remembers the student IDs already accepted, so a second request
 * from the same student is caught as a duplicate.
 */
public class RequestValidator {
    public static final int MIN_YEAR = 1;
    public static final int MAX_YEAR = 4;
    // Students living closer than this to the campus are not eligible for a hostel.
    public static final double MIN_DISTANCE_KM = 5.0;

    private final Set<String> acceptedIds = new HashSet<>();

    public String validate(AllocationRequest request) {
        if (request.getParseError() != null) {
            return request.getParseError();
        }

        Student s = request.getStudent();

        if (s.getId().isEmpty()) {
            return "Student ID is missing";
        }
        if (s.getName().isEmpty()) {
            return "Student name is missing";
        }
        if (s.getYear() < MIN_YEAR || s.getYear() > MAX_YEAR) {
            return "Year of study must be a number from " + MIN_YEAR + " to " + MAX_YEAR;
        }
        if (s.getDistanceKm() < 0) {
            return "Distance from home must be a non-negative number";
        }
        if (s.getDistanceKm() < MIN_DISTANCE_KM) {
            return "Lives within " + MIN_DISTANCE_KM + " km of campus (not eligible)";
        }
        if (request.getPreferredType() == null) {
            return "Unknown room type '" + request.getPreferredText() + "'";
        }
        if (acceptedIds.contains(s.getId())) {
            return "Duplicate request for student ID " + s.getId();
        }

        acceptedIds.add(s.getId());
        return null;
    }
}