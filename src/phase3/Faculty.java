package phase3;

/**
 * Faculty – lightweight data record for a logged-in faculty member.
 * Passed between frames so every screen knows who is logged in.
 */
public class Faculty {
    private final int    id;
    private final String email;
    private final String fullName;

    public Faculty(int id, String email, String fullName) {
        this.id       = id;
        this.email    = email;
        this.fullName = fullName;
    }

    public int    getId()       { return id; }
    public String getEmail()    { return email; }
    public String getFullName() { return fullName; }

    @Override
    public String toString() {
        return "Faculty{id=" + id + ", email='" + email + "', name='" + fullName + "'}";
    }
}
