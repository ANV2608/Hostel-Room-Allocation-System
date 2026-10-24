
import java.util.Comparator;

/**
 * Custom comparator that decides who gets a room first.
 * A request that compares as "smaller" is served earlier.
 *
 *   1. Higher year of study first          (4th year before 1st year)
 *   2. Larger distance from home first     (1200 km before 300 km)
 *   3. Earlier arrival first               (tie-breaker, keeps the order stable)
 *
 * To change the policy, flip the a/b order in a rule or reorder the rules.
 * Example: to favour first-years, swap b and a in the year comparison.
 */
public class PriorityComparator implements Comparator<AllocationRequest> {

    @Override
    public int compare(AllocationRequest a, AllocationRequest b) {
        // Rule 1: year of study, descending
        int result = Integer.compare(b.getStudent().getYear(), a.getStudent().getYear());
        if (result != 0) {
            return result;
        }

        // Rule 2: distance from home, descending
        result = Double.compare(b.getStudent().getDistanceKm(), a.getStudent().getDistanceKm());
        if (result != 0) {
            return result;
        }

        // Rule 3: whoever applied first
        return Integer.compare(a.getSequence(), b.getSequence());
    }
}