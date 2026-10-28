
import java.util.Comparator;

public class PriorityComparator implements Comparator<AllocationRequest> {

    @Override
    public int compare(AllocationRequest a, AllocationRequest b) {
        
        int result = Integer.compare(b.getStudent().getYear(), a.getStudent().getYear());
        if (result != 0) {
            return result;
        }

        
        result = Double.compare(b.getStudent().getDistanceKm(), a.getStudent().getDistanceKm());
        if (result != 0) {
            return result;
        }

       
        return Integer.compare(a.getSequence(), b.getSequence());
    }
}