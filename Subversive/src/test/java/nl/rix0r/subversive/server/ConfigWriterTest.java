
package nl.rix0r.subversive.server;

import nl.rix0r.subversive.subversion.Access;
import nl.rix0r.subversive.subversion.Directory;
import nl.rix0r.subversive.subversion.GrantPermission;
import nl.rix0r.subversive.subversion.Group;
import nl.rix0r.subversive.subversion.NewGroup;
import nl.rix0r.subversive.subversion.Permission;
import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rix0rrr
 */
public class ConfigWriterTest {
    private DiskConfiguration config = new DiskConfiguration(null);
    private StringBuilderWriter writer = new StringBuilderWriter();

    @Test
    public void localGroupReference() throws Exception {
        new NewGroup(new Group("foo", "Owners")).apply(config);
        new GrantPermission(new Permission(
                new Directory("foo", "/"),
                new Group("foo", "Owners"),
                Access.Read)).apply(config);
        config.save(writer);

        Assert.assertTrue(writer.toString().contains("@foo.Owners="));
    }

}
