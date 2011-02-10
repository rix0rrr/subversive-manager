
package nl.rix0r.subversive.server;

import java.io.Reader;
import java.io.StringReader;
import nl.rix0r.subversive.subversion.Access;
import nl.rix0r.subversive.subversion.Directory;
import nl.rix0r.subversive.subversion.Group;
import nl.rix0r.subversive.subversion.Permission;
import nl.rix0r.subversive.subversion.User;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author rix0rrr
 */
public class ConfigParserTest {
    private DiskConfiguration config = new DiskConfiguration(null);

    @Test
    public void testBlockMatcher() {
        String[] cases = new String[] {
            "[foo]", "[a:/b/c]"
        };

        for (String one: cases) {
            Assert.assertTrue(
                "Doesn't match: " + one,
                DiskConfiguration.blockPattern.matcher(one).matches());
        }
    }

    @Test
    public void groupDefinition() throws Exception {
        config.load(input("[groups]", "foo=bar"));

        Assert.assertEquals(0, config.loadWarnings().size());
        Assert.assertTrue(config.group(new Group("foo")).contains(new User("bar")));
    }

    @Test
    public void repositoryWithDashes() throws Exception {
        config.load(input("[groups]",
                "foo-bar.boop=baz",
                "[repo:/]",
                "@foo-bar.boop=r"
                ));

        Group group = new Group("foo-bar", "boop");
        Assert.assertEquals(0, config.loadWarnings().size());
        Assert.assertNotNull(config.group(group));
        Assert.assertTrue("Permissions", config.permissions(null, group).size() == 1);
    }

    @Test
    public void permissionAssigmentGroup() throws Exception {
        config.load(input("[groups]", "foo=bar", "[a:/]", "@foo=r"));

        Assert.assertEquals(0, config.loadWarnings().size());
        Assert.assertEquals(Access.Read,
                ((Permission)config.permissions(new Directory("a", "/"), new Group("foo")).toArray()[0]).access());
    }

    @Test
    public void permissionAssigmentUser() throws Exception {
        config.load(input("[a:/]", "master_foo=r"));

        Assert.assertEquals(0, config.loadWarnings().size());
        Assert.assertEquals(Access.Read,
                ((Permission)config.permissions(new Directory("a", "/"), new User("master_foo")).toArray()[0]).access());
    }

    @Test
    public void skipInvalidBlocks() throws Exception {
        config.load(input(
                "[groups]",
                "admin=rix0r",
                "[:/]",
                "anothergroup=henk"
                ));
        Assert.assertEquals(1, config.groupDefinitions().size());
        Assert.assertEquals("admin", config.groupDefinitions().iterator().next().group().name());
    }

    private Reader input(String... lines) {
        return new StringReader(StringUtils.join(lines, "\n"));
    }
}
