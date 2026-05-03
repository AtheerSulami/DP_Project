package phase3;

import java.sql.*;

/**
 * DatabaseManager – handles all MySQL connectivity for SilentCheck.
 *
 * FACTORY PATTERN changes:
 *   authenticate()  → uses FacultyFactory.fromResultSet(rs)
 *   getRoomByName() → uses RoomFactory.fromResultSet(rs)
 *   Both previously built objects inline with new Faculty(...) / new Room(...)
 */
public class DatabaseManager {

    private static final String URL     = "jdbc:mysql://localhost:3306/silentcheck?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "silentcheck";
    private static final String DB_PASS = "1234";

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, DB_USER, DB_PASS);
            System.out.println("[DB] Connected to MySQL successfully.");
            initSchema();
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found. Add mysql-connector-j.jar to classpath.", e);
        }
    }

    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (instance == null || instance.connection.isClosed()) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() { return connection; }

    private void initSchema() throws SQLException {
        String createFaculty =
            "CREATE TABLE IF NOT EXISTS faculty (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(100) UNIQUE NOT NULL, " +
            "password_hash VARCHAR(255) NOT NULL, full_name VARCHAR(100) NOT NULL DEFAULT 'Faculty Member', " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        String createRooms =
            "CREATE TABLE IF NOT EXISTS rooms (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50) NOT NULL, floor INT NOT NULL, " +
            "capacity VARCHAR(50), equipment VARCHAR(200), " +
            "status ENUM('Available','Occupied','Restricted') DEFAULT 'Available')";

        String createBookings =
            "CREATE TABLE IF NOT EXISTS bookings (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, faculty_id INT NOT NULL, room_id INT NOT NULL, " +
            "booked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, permit_file VARCHAR(255), " +
            "FOREIGN KEY (faculty_id) REFERENCES faculty(id), FOREIGN KEY (room_id) REFERENCES rooms(id))";

        try (Statement st = connection.createStatement()) {
            st.execute(createFaculty);
            st.execute(createRooms);
            st.execute(createBookings);
        }
        seedDefaultData();
    }

    private void seedDefaultData() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM rooms")) {
            rs.next();
            if (rs.getInt(1) == 0) {
                String ins = "INSERT INTO rooms (name, floor, capacity, equipment, status) VALUES (?,?,?,?,?)";
                Object[][] rooms = {
                    {"Lab G-01",0,"30 PCs","Projector, Whiteboard","Available"},
                    {"Room G-02",0,"40 Chairs","Smart Board","Available"},
                    {"Lab G-05",0,"25 PCs","Projector","Occupied"},
                    {"Room G-08",0,"35 Chairs","Whiteboard","Available"},
                    {"Lab G-10",0,"30 PCs","Projector, Sound System","Available"},
                    {"Room G-12",0,"45 Chairs","Smart Board, Projector","Available"},
                    {"Room 101",1,"50 Chairs","Smart Board, Projector","Available"},
                    {"Lab 102",1,"20 PCs","Projector","Available"},
                    {"Room 103",1,"60 Chairs","Smart Board","Occupied"},
                    {"Room 104",1,"55 Chairs","Projector, Sound System","Available"},
                    {"Lab 105",1,"25 PCs","Projector","Available"},
                    {"Room 106",1,"40 Chairs","Whiteboard","Available"},
                    {"Dean Office",2,"Private","N/A","Restricted"},
                    {"Meeting A",2,"Admin Only","Conference Setup","Restricted"},
                    {"Faculty Lounge",2,"Staff Only","N/A","Restricted"},
                    {"Dept. Head",2,"Private","N/A","Restricted"},
                };
                try (PreparedStatement ps = connection.prepareStatement(ins)) {
                    for (Object[] r : rooms) {
                        ps.setString(1,(String)r[0]); ps.setInt(2,(int)r[1]);
                        ps.setString(3,(String)r[2]); ps.setString(4,(String)r[3]);
                        ps.setString(5,(String)r[4]); ps.addBatch();
                    }
                    ps.executeBatch();
                }
                System.out.println("[DB] Rooms seeded.");
            }
        }

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM faculty")) {
            rs.next();
            if (rs.getInt(1) == 0) {
                String ins = "INSERT INTO faculty (email, password_hash, full_name) VALUES (?,?,?)";
                try (PreparedStatement ps = connection.prepareStatement(ins)) {
                    ps.setString(1,"demo@kau.edu.sa");
                    ps.setString(2,PasswordUtil.hash("1234"));
                    ps.setString(3,"Demo Faculty");
                    ps.executeUpdate();
                }
                System.out.println("[DB] Demo account created -> demo@kau.edu.sa / 1234");
            }
        }
    }

    // ── Faculty ────────────────────────────────────────────────────────────────
    /** FACTORY PATTERN: FacultyFactory.fromResultSet replaces inline new Faculty(...) */
    public Faculty authenticate(String email, String password) throws SQLException {
        String sql = "SELECT id, email, full_name FROM faculty WHERE email=? AND password_hash=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, PasswordUtil.hash(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return FacultyFactory.fromResultSet(rs); // <-- Factory
            }
        }
        return null;
    }

    public void registerFaculty(String email, String password, String fullName) throws SQLException {
        String sql = "INSERT INTO faculty (email, password_hash, full_name) VALUES (?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, fullName);
            ps.executeUpdate();
        }
    }

    // ── Rooms ──────────────────────────────────────────────────────────────────
    public ResultSet getRoomsByFloor(int floor) throws SQLException {
        String sql = "SELECT * FROM rooms WHERE floor=? ORDER BY id";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, floor);
        return ps.executeQuery();
    }

    /** FACTORY PATTERN: RoomFactory.fromResultSet replaces inline new Room(...) */
    public Room getRoomByName(String name) throws SQLException {
        String sql = "SELECT * FROM rooms WHERE name=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return RoomFactory.fromResultSet(rs); // <-- Factory
            }
        }
        return null;
    }

    public void updateRoomStatus(int roomId, String status) throws SQLException {
        String sql = "UPDATE rooms SET status=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status); ps.setInt(2, roomId); ps.executeUpdate();
        }
    }

    // ── Bookings ───────────────────────────────────────────────────────────────
    public int createBooking(int facultyId, int roomId, String permitFile) throws SQLException {
        String sql = "INSERT INTO bookings (faculty_id, room_id, permit_file) VALUES (?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,facultyId); ps.setInt(2,roomId); ps.setString(3,permitFile);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public ResultSet getBookingsByFaculty(int facultyId) throws SQLException {
        String sql = "SELECT b.id, r.name AS room, b.booked_at, b.permit_file " +
                     "FROM bookings b JOIN rooms r ON b.room_id = r.id " +
                     "WHERE b.faculty_id = ? ORDER BY b.booked_at DESC";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, facultyId);
        return ps.executeQuery();
    }

    public void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException ignored) {}
    }
}
