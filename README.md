1️⃣ Singleton — "نسخة واحدة فقط"
التعريف:
الـ Singleton يضمن أن الكلاس لا يُنشأ منه غير نسخة واحدة فقط طوال عمر البرنامج، ويوفر نقطة وصول عالمية لهذه النسخة.
التطبيق في الكود — DatabaseManager.java:
// ① النسخة الوحيدة — مخفية وstatic
private static DatabaseManager instance;

// ② Constructor خاص — لا أحد يقدر يعمل new DatabaseManager()
private DatabaseManager() throws SQLException {
    connection = DriverManager.getConnection(URL, DB_USER, DB_PASS);
    initSchema();
}

// ③ نقطة الوصول الوحيدة
public static synchronized DatabaseManager getInstance() throws SQLException {
    if (instance == null || instance.connection.isClosed()) {
        instance = new DatabaseManager(); // تُنشأ مرة واحدة فقط
    }
    return instance;
}

-------------------------

2️⃣ Factory — "مصنع الكائنات"
التعريف:
الـ Factory يُمركز إنشاء الكائنات في مكان واحد بدل ما تتكرر عملية الـ new في كل أجزاء الكود.
التطبيق في الكود — RoomFactory.java:
public class RoomFactory {

    private RoomFactory() {} // لا نسخ — static utility فقط

    // إنشاء Room من ResultSet
    public static Room fromResultSet(ResultSet rs) throws SQLException {
        return new Room(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getInt("floor"),
            rs.getString("capacity"),
            rs.getString("equipment"),
            rs.getString("status")
        );
    }
}

قبل وبعد:
java// ❌ قبل — نفس الكود يتكرر في 3 أماكن مختلفة
new Room(rs.getInt("id"), rs.getString("name"), rs.getInt("floor"),
         rs.getString("capacity"), rs.getString("equipment"), rs.getString("status"));

// ✅ بعد — سطر واحد في كل مكان
RoomFactory.fromResultSet(rs);

-------------------------

3️⃣ Observer — "إشعار المشتركين تلقائياً"
التعريف:
الـ Observer يُنشئ علاقة One-to-Many بين الكائنات؛ لما يتغير حال الـ Subject (الناشر)، يتم إخطار جميع المشتركين تلقائياً دون ما الناشر يعرف تفاصيلهم.
الأدوار في المشروع:
Observer (الواجهة) --> BookingObserver.java
Subject (الناشر) --> BookingEventPublisher.java
Concrete Observer --> NetworkBroadcastObserver.java

// BookingObserver.java — الواجهة
public interface BookingObserver {
    void onBookingConfirmed(BookingService.BookingResult result, Room room);
}

// BookingEventPublisher.java — الناشر
public void notifyBookingConfirmed(BookingResult result, Room room) {
    for (BookingObserver obs : observers) {
        obs.onBookingConfirmed(result, room); // يخبر الكل
    }
}

// NetworkBroadcastObserver.java — مشترك ينفذ الـ broadcast
public void onBookingConfirmed(BookingResult result, Room room) {
    BookingServer.broadcast("ROOM_BOOKED:" + room.getName() + ":Occupied");
}

د:
java// ❌ قبل — ConfirmationFrame يتكلم مع BookingServer مباشرة
BookingServer.broadcast("ROOM_BOOKED:" + room.getName() + ":Occupied");

// ✅ بعد — ConfirmationFrame فقط يُشعر الناشر، والباقي يصير تلقائياً
BookingEventPublisher.getInstance().notifyBookingConfirmed(result, room);

-------------------------

4️⃣ Facade — "الواجهة المبسطة"
التعريف:
الـ Facade يوفر واجهة موحدة ومبسطة لمجموعة أنظمة معقدة. شاشات الـ UI بدل ما تتكلم مع 4 أنظمة مختلفة، تتكلم مع كلاس واحد فقط.
قاعدة البيانات --> DatabaseManager
الحجز المتزامن --> BookingService
ملفات الادخال والاخراج --> PermitWriter
الشبكة --> BookingServer

// تسجيل الدخول — بدل ما LoginFrame يعرف DatabaseManager
public Faculty login(String email, String password) throws SQLException {
    return DatabaseManager.getInstance().authenticate(email, password);
}

// الحجز — بدل ما ConfirmationFrame يعرف BookingService وBookingServer
public BookingResult processBooking(Faculty faculty, Room room) throws Exception {
    Future<BookingResult> future = BookingService.submitBooking(faculty, room);
    BookingResult result = future.get();
    if (result.success()) {
        BookingEventPublisher.getInstance().notifyBookingConfirmed(result, room);
    }
    return result;
}

// إغلاق كل الأنظمة بأمر واحد
public void shutdown() {
    BookingServer.stop();
    BookingService.shutdown();
    DatabaseManager.getInstance().close();
}

// ❌ قبل — LoginFrame يتعامل مع الأنظمة مباشرة
DatabaseManager db = DatabaseManager.getInstance();
Faculty f = db.authenticate(email, password);
BookingServer.start();

// ✅ بعد — LoginFrame يتكلم مع الـ Facade فقط
SilentCheckFacade facade = SilentCheckFacade.getInstance();
Faculty f = facade.login(email, password);
facade.startServer();
