
package nl.rix0r.subversive.subversion;

import java.io.Serializable;

/**
 * Representation of Access rights
 *
 * @author rix0rrr
 */
public enum Access implements Serializable {
    Read,
    ReadWrite,
    Revoke;
}
