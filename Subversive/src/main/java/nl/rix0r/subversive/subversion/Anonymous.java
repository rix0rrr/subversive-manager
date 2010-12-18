
package nl.rix0r.subversive.subversion;

/**
 * An unauthenticated (anonymous) user
 *
 * @author rix0rrr
 */
public class Anonymous implements Principal {

    @Override
    public boolean equals(Object o) {
        return o instanceof Anonymous;
    }

    @Override
    public int hashCode() {
        return 7;
    }

    @Override
    public String toString() {
        return "Anonymous";
    }
}
