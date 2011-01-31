
package nl.rix0r.subversive.subversion;

import java.util.Collection;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author rix0rrr
 */
public class ModificationsTest {

    @Test
    public void grantPermissions() {
        Configuration config = new Configuration();
        new NewGroup(new Group("foo", "Owners")).apply(config);
        new GrantPermission(new Permission(
                new Directory("foo", "/"),
                new Group("foo", "Owners"),
                Access.Read)).apply(config);
        Assert.assertFalse(first(config.groupDefinitions()).group().global());
    }

    private <T> T first(Collection<T> coll) {
        return coll.iterator().next();
    }
}
