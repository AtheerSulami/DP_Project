package phase3;

/**
 * Room – data record for a room fetched from the database.
 */
public class Room {
    private final int    id;
    private final String name;
    private final int    floor;
    private final String capacity;
    private final String equipment;
    private final String status;

    public Room(int id, String name, int floor, String capacity, String equipment, String status) {
        this.id        = id;
        this.name      = name;
        this.floor     = floor;
        this.capacity  = capacity;
        this.equipment = equipment;
        this.status    = status;
    }

    public int    getId()        { return id; }
    public String getName()      { return name; }
    public int    getFloor()     { return floor; }
    public String getCapacity()  { return capacity; }
    public String getEquipment() { return equipment; }
    public String getStatus()    { return status; }

    public boolean isAvailable()  { return "Available".equalsIgnoreCase(status); }
    public boolean isRestricted() { return "Restricted".equalsIgnoreCase(status); }
}
