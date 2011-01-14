
package nl.rix0r.subversive.subversion;

import junit.framework.TestCase;

/**
 * JUnit 3 for GWT compatibility's sake
 *
 * @author rix0rrr
 */
public class EditSessionTest extends TestCase {
    public void testGroups() {
        Configuration config = new TestConfigurationBuilder()
                .group(new Group("global"))
                .group(new Group("foo", "local"))
                .group(new Group("bar", "irrelevant"))
                .build();

        Configuration subset = config.subset("foo");
        EditSession session = new EditSession("foo", subset);
        assertEquals(2, session.availableGroups().size());
    }

}
