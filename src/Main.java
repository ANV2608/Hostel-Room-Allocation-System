
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class Main {

    private static final Scanner sc = new Scanner(System.in);
    private static final HostelManager manager = new HostelManager();
    private static final Set<String> studentIds = new HashSet<>();

    private static int sequence = 1;

    public static void main(String[] args) {
        loadRoomsAtStartup();

        boolean running = true;

        while (running) {
            System.out.println("\n====================================");
            System.out.println("     HOSTEL ROOM ALLOCATION");
            System.out.println("====================================");
            System.out.println("1. Add a room");
            System.out.println("2. Add student and allocate room");
            System.out.println("3. Check out a student");
            System.out.println("4. View rooms and occupancy");
            System.out.println("5. View allocation report");
            System.out.println("6. Save report");
            System.out.println("0. Exit");
            System.out.println("------------------------------------");

            int choice = readInt("Enter your choice: ");

            try {
                switch (choice) {
                    case 1:
                        addRoom();
                        break;
                    case 2:
                        addStudent();
                        break;
                    case 3:
                        checkoutStudent();
                        break;
                    case 4:
                        showRooms();
                        break;
                    case 5:
                        showReport();
                        break;
                    case 6:
                        saveReport();
                        break;
                    case 0:
                        running = false;
                        System.out.println("Exiting Hostel Room Allocation System.");
                        break;
                    default:
                        System.out.println("Invalid choice. Select 0 to 6.");
                }
            } catch (RuntimeException e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }

        sc.close();
    }

    private static void loadRoomsAtStartup() {
        Path file = Paths.get("data", "rooms.csv");

        if (!Files.exists(file)) {
            System.out.println("No rooms.csv found. Add rooms from the menu.");
            return;
        }

        try {
            List<Room> rooms = FileHandler.readRooms(file);

            for (Room room : rooms) {
                manager.addRoom(room);
            }

            System.out.println("Loaded " + rooms.size() + " room(s).");
        } catch (IOException e) {
            System.out.println("Could not load rooms: " + e.getMessage());
        }
    }

    private static void addRoom() {
        System.out.println("\n--- Add New Room ---");

        String number = readText("Room number: ").toUpperCase();

        if (number.isEmpty()) {
            System.out.println("Room number cannot be empty.");
            return;
        }

        if (findRoom(number) != null) {
            System.out.println("This room number already exists.");
            return;
        }

        RoomType type = readRoomType();
        Gender gender = readGender();

        Room room = RoomFactory.create(type, number, gender);
        List<AllocationRequest> promoted = manager.addRoom(room);

        System.out.println("\nRoom " + number + " added successfully.");
        printPromotions(promoted);
        showRooms();
    }

    private static void addStudent() {
        System.out.println("\n--- Register Student ---");

        String id = readText("Student ID: ").toUpperCase();

        if (id.isEmpty()) {
            System.out.println("Student ID cannot be empty.");
            return;
        }

        if (studentIds.contains(id)) {
            System.out.println("This student ID has already been entered.");
            return;
        }

        String name = readText("Student name: ");

        if (name.isEmpty()) {
            System.out.println("Student name cannot be empty.");
            return;
        }

        Gender gender = readGender();

        int year;
        do {
            year = readInt("Academic year (1-4): ");
            if (year < 1 || year > 4) {
                System.out.println("Enter a year between 1 and 4.");
            }
        } while (year < 1 || year > 4);

        double distance;
        do {
            distance = readDouble("Distance from campus in km: ");
            if (distance < 0) {
                System.out.println("Distance cannot be negative.");
            }
        } while (distance < 0);

        RoomType preferredRoom = readRoomType();

        Student student =
                new Student(id, name, gender, year, distance);

        AllocationRequest request = new AllocationRequest(
                student,
                preferredRoom,
                preferredRoom.name(),
                sequence++
        );

        
        manager.processRequests(Collections.singletonList(request));
        studentIds.add(id);

        System.out.println("\nStudent request processed.");
        System.out.println("Student: " + name + " (" + id + ")");
        System.out.println("Preferred room: " + preferredRoom);

       
        showReport();
        showRooms();
    }

    private static void checkoutStudent() {
        System.out.println("\n--- Student Check-Out ---");

        String id = readText("Student ID: ").toUpperCase();

        if (id.isEmpty()) {
            System.out.println("Student ID cannot be empty.");
            return;
        }

        boolean assigned = false;

        for (AllocationRequest request :
                manager.getRequestsByStatus(RequestStatus.ASSIGNED)) {

            if (request.getStudent().getId().equals(id)) {
                assigned = true;
                break;
            }
        }

        if (!assigned) {
            System.out.println(
                    "This student does not currently have an assigned room."
            );
            return;
        }

        List<AllocationRequest> promoted = manager.checkout(id);

        System.out.println("\nStudent " + id + " checked out successfully.");
        System.out.println("The room's available-bed count has been updated.");

        printPromotions(promoted);
        showRooms();
    }



    private static void showRooms() {
        System.out.println("\n========== ROOM STATUS ==========");

        int roomCount = 0;
        int totalBeds = 0;
        int freeBeds = 0;

        for (RoomType type : RoomType.values()) {
            for (Room room : manager.getRooms(type)) {
                roomCount++;

                int capacity = room.getCapacity();
                int vacant = room.getVacantBeds();
                int occupied = capacity - vacant;

                totalBeds += capacity;
                freeBeds += vacant;

                System.out.println("\nRoom: " + room.getRoomNumber());
                System.out.println("Type: " + room.getType());
                System.out.println("Gender: " + room.getGender().wing());
                System.out.println("Capacity: " + capacity);
                System.out.println("Occupied beds: " + occupied);
                System.out.println("Free beds: " + vacant);

                System.out.println("Students:");

                if (room.getOccupants().isEmpty()) {
                    System.out.println("  No students assigned.");
                } else {
                    for (Student student : room.getOccupants()) {
                        System.out.println(
                                "  " + student.getName()
                                + " (" + student.getId() + ")"
                        );
                    }
                }
            }
        }

        System.out.println("\n---------------------------------");
        System.out.println("Total rooms: " + roomCount);
        System.out.println("Total beds: " + totalBeds);
        System.out.println("Available beds: " + freeBeds);
        System.out.println("Occupied beds: " + (totalBeds - freeBeds));
        System.out.println("=================================");
    }

    private static void showReport() {
        String report = ReportGenerator.build(
                "HOSTEL ROOM ALLOCATION REPORT",
                manager
        );

        System.out.println("\n" + report);
    }

    private static void saveReport() {
        try {
            Path output = Paths.get("output", "console_report.txt");

            String report = ReportGenerator.build(
                    "HOSTEL ROOM ALLOCATION REPORT",
                    manager
            );

            FileHandler.writeReport(output, report);

            System.out.println("Report saved to: " + output);
        } catch (IOException e) {
            System.out.println("Could not save report: " + e.getMessage());
        }
    }


    private static Room findRoom(String number) {
        for (RoomType type : RoomType.values()) {
            for (Room room : manager.getRooms(type)) {
                if (room.getRoomNumber().equalsIgnoreCase(number)) {
                    return room;
                }
            }
        }
        return null;
    }

    private static RoomType readRoomType() {
        while (true) {
            System.out.println("Room types: SINGLE, DOUBLE, TRIPLE");

            String value = readText("Preferred room type: ");
            RoomType type = RoomType.fromString(value);

            if (type != null) {
                return type;
            }

            System.out.println("Invalid room type. Try again.");
        }
    }

    private static Gender readGender() {
        while (true) {
            System.out.println("Gender: MALE or FEMALE");

            Gender gender = Gender.fromString(readText("Enter gender: "));

            if (gender != null) {
                return gender;
            }

            System.out.println("Invalid gender. Try again.");
        }
    }

    private static void printPromotions(
            List<AllocationRequest> promoted) {

        if (promoted == null || promoted.isEmpty()) {
            return;
        }

        System.out.println("\nWaitlist promotions:");

        for (AllocationRequest request : promoted) {
            System.out.println(
                    request.getStudent().getName()
                    + " (" + request.getStudent().getId() + ")"
                    + " -> Room "
                    + request.getAssignedRoom().getRoomNumber()
            );
        }
    }

    private static String readText(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readText(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            try {
                double value = Double.parseDouble(readText(prompt));

                if (Double.isFinite(value)) {
                    return value;
                }

                System.out.println("Enter a finite number.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}
