import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Entry point. Run it from the project root folder (the one that contains
 * the data/ folder).
 *
 *   Phase 1: read rooms + requests -> filter -> rank -> allocate -> report
 *   Phase 2: read check-outs       -> free beds -> promote waitlist -> report
 *
 * Optional arguments:  java -cp out Main [dataFolder] [outputFolder]
 */
public class Main {

    public static void main(String[] args) {
        String dataDir = args.length > 0 ? args[0] : "data";
        String outDir = args.length > 1 ? args[1] : "output";

        Path roomsFile = Paths.get(dataDir, "rooms.csv");
        Path requestsFile = Paths.get(dataDir, "requests.csv");
        Path checkoutsFile = Paths.get(dataDir, "checkouts.csv");
        Path allocationReport = Paths.get(outDir, "allocation_report.txt");
        Path finalReport = Paths.get(outDir, "final_report.txt");

        try {
            // ---------- Phase 1: allocation ----------
            HostelManager manager = new HostelManager();

            for (Room room : FileHandler.readRooms(roomsFile)) {
                manager.addRoom(room);
            }

            List<AllocationRequest> requests = FileHandler.readRequests(requestsFile);
            manager.processRequests(requests);

            String report1 = ReportGenerator.build("HOSTEL ALLOCATION REPORT - after allocation", manager);
            System.out.println(report1);
            FileHandler.writeReport(allocationReport, report1);

            // ---------- Phase 2: check-outs and waitlist promotion ----------
            List<String> checkouts = FileHandler.readCheckouts(checkoutsFile);
            if (checkouts.isEmpty()) {
                System.out.println("No check-outs found in " + checkoutsFile + ", skipping phase 2.");
            } else {
                for (String studentId : checkouts) {
                    List<AllocationRequest> promoted = manager.checkout(studentId);
                    for (AllocationRequest p : promoted) {
                        System.out.println(">> " + p.getStudent().getName() + " (" + p.getStudent().getId()
                                + ") promoted from waitlist to room " + p.getAssignedRoom().getRoomNumber());
                    }
                }
                System.out.println();

                String report2 = ReportGenerator.build("HOSTEL ALLOCATION REPORT - after check-outs", manager);
                System.out.println(report2);
                FileHandler.writeReport(finalReport, report2);
            }

            System.out.println("Reports saved in the '" + outDir + "' folder.");

        } catch (IOException e) {
            System.err.println("Could not read or write a file: " + e.getMessage());
            System.err.println("Tip: run the program from the project root folder (the one containing 'data').");
        }
    }
}