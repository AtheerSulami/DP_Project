package phase3;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * PermitWriter – IO Stream layer for SilentCheck.
 *
 * Generates a human-readable booking permit as a .txt file and saves it
 * to a local "permits/" directory. Uses BufferedWriter for efficiency.
 *
 * Covers: IO Streams requirement (FileWriter, BufferedWriter, PrintWriter).
 */
public class PermitWriter {

    private static final String PERMITS_DIR = "permits";
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm:ss");
    private static final DateTimeFormatter FILE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private PermitWriter() {}

    /**
     * Generates a booking permit file and returns its path.
     *
     * @param bookingId  The DB-assigned booking ID
     * @param faculty    The faculty member who made the booking
     * @param room       The room that was booked
     * @return           The absolute path of the created permit file
     * @throws IOException if the file cannot be written
     */
    public static String generatePermit(int bookingId, Faculty faculty, Room room)
            throws IOException {

        // Ensure permits/ directory exists
        Path dir = Paths.get(PERMITS_DIR);
        if (!Files.exists(dir)) Files.createDirectories(dir);

        // Build a unique filename
        String timestamp = LocalDateTime.now().format(FILE_FMT);
        String fileName  = String.format("permit_%d_%s.txt", bookingId, timestamp);
        Path   filePath  = dir.resolve(fileName);

        // Write the permit using BufferedWriter over FileWriter
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            writeLine(bw, "╔══════════════════════════════════════════════════════════╗");
            writeLine(bw, "║              SILENTCHECK – BOOKING PERMIT                ║");
            writeLine(bw, "║         King Abdulaziz University – Faculty of CEIT      ║");
            writeLine(bw, "╚══════════════════════════════════════════════════════════╝");
            writeLine(bw, "");
            writeLine(bw, "  Permit No.  : #" + bookingId);
            writeLine(bw, "  Issued On   : " + LocalDateTime.now().format(DISPLAY_FMT));
            writeLine(bw, "");
            writeLine(bw, "  ── Faculty Details ─────────────────────────────────────");
            writeLine(bw, "  Name        : " + faculty.getFullName());
            writeLine(bw, "  Email       : " + faculty.getEmail());
            writeLine(bw, "");
            writeLine(bw, "  ── Room Details ─────────────────────────────────────────");
            writeLine(bw, "  Room        : " + room.getName());
            writeLine(bw, "  Floor       : " + floorLabel(room.getFloor()));
            writeLine(bw, "  Capacity    : " + room.getCapacity());
            writeLine(bw, "  Equipment   : " + room.getEquipment());
            writeLine(bw, "  Status      : Confirmed ✓");
            writeLine(bw, "");
            writeLine(bw, "  ─────────────────────────────────────────────────────────");
            writeLine(bw, "  Please present this permit to Security/Admin if requested.");
            writeLine(bw, "  This permit is valid for the booked session only.");
            writeLine(bw, "  ─────────────────────────────────────────────────────────");
            writeLine(bw, "");
            writeLine(bw, "                   [ SilentCheck System ]");
        }

        System.out.println("[IO] Permit saved → " + filePath.toAbsolutePath());
        return filePath.toAbsolutePath().toString();
    }

    /** Reads an existing permit file and returns its content as a String. */
    public static String readPermit(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    // ── helpers ───────────────────────────────────────────────────────────────
    private static void writeLine(BufferedWriter bw, String line) throws IOException {
        bw.write(line);
        bw.newLine();
    }

    private static String floorLabel(int floor) {
        return switch (floor) {
            case 0  -> "Ground Floor";
            case 1  -> "First Floor";
            case 2  -> "Second Floor";
            default -> "Unknown Floor";
        };
    }
}
