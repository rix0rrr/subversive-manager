
package nl.rix0r.subversive.subversion;

/**
 * A single user, identified by a username
 *
 * Users optionally also have a Full Name, which can be retrieved from a system
 * like LDAP.
 *
 * @author rix0rrr
 */
public class User implements Principal {
    private String username;
    private String fullName;

    protected User() {
    }

    public User(String username) {
        this.username = username;
        this.fullName = "";
    }

    public User(String username, String fullName) {
        this.username = username;
        this.fullName = fullName;
    }

    public String fullName() {
        return fullName;
    }

    public String username() {
        return username;
    }

    public boolean matches(String like) {
        return username.toLowerCase().contains(like.toLowerCase())
                || fullName.toLowerCase().contains(like.toLowerCase());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final User other = (User) obj;
        if ((this.username == null) ? (other.username != null) : !this.username.equals(other.username)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 19 * hash + (this.username != null ? this.username.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return fullName.equals("") ? username : fullName;
    }
}
