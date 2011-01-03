
package nl.rix0r.subversive.subversion;

import java.io.Serializable;

/**
 * A Modification represents a change to a Configuration
 *
 * @author rix0rrr
 */
public interface Modification extends Serializable {

    /**
     * Apply the modification represented by this object to the Configuration
     */
    public void apply(Configuration configuration);
}
