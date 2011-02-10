
package nl.rix0r.subversive.subversion;

/**
 * A single user, identified by a username
 *
 * Users optionally also have a Full Name, which can be retrieved from a system
 * like LDAP.
 *
 * @author rix0rrr
 */
public class User implements Principal, Comparable {
    private String username;
    private String fullName;

    public User() { }

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
        if ((this.username == null) ? (other.username != null) : !this.username.toLowerCase().equals(other.username.toLowerCase())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 19 * hash + (this.username != null ? this.username.toLowerCase().hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return fullName.equals("") ? username : fullName;
    }

    public int compareTo(Object o) {
        return toString().compareToIgnoreCase(o.toString());
    }
}
