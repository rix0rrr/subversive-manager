
package nl.rix0r.subversive.server;

import nl.rix0r.subversive.subversion.Access;
import nl.rix0r.subversive.subversion.AddUserToGroup;
import nl.rix0r.subversive.subversion.Directory;
import nl.rix0r.subversive.subversion.GrantPermission;
import nl.rix0r.subversive.subversion.Group;
import nl.rix0r.subversive.subversion.NewGroup;
import nl.rix0r.subversive.subversion.Permission;
import nl.rix0r.subversive.subversion.User;
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
        String repo = "foo";
        new NewGroup(new Group(repo, "Owners")).apply(config);
        new GrantPermission(new Permission(
                new Directory(repo, "/"),
                new Group(repo, "Owners"),
                Access.Read)).apply(config);
        config.save(writer);

        Assert.assertTrue(writer.toString().contains("@foo.Owners="));
    }

    @Test
    public void writeRepoWithDash() throws Exception {
        String repo = "foo-bar";
        Group group = new Group(repo, "Owners");
        new NewGroup(group).apply(config);
        new AddUserToGroup(new User("henk"), group).apply(config);
        new GrantPermission(new Permission(
                new Directory(repo, "/"),
                group,
                Access.Read)).apply(config);
        config.save(writer);

        Assert.assertTrue("Permission", writer.toString().contains("@foo-bar.Owners=r"));
        Assert.assertTrue("Group definition", writer.toString().contains("foo-bar.Owners = henk"));
    }

    @Test
    public void writeGroupWithDash() throws Exception {
        String repo = "foo-bar";
        Group group = new Group(repo, "Repo Owners");
        new NewGroup(group).apply(config);
        new AddUserToGroup(new User("henk"), group).apply(config);
        new GrantPermission(new Permission(
                new Directory(repo, "/"),
                group,
                Access.Read)).apply(config);
        config.save(writer);

        Assert.assertTrue("Permission", writer.toString().contains("@foo-bar.Repo-Owners=r"));
        Assert.assertTrue("Group definition", writer.toString().contains("foo-bar.Repo-Owners = henk"));
    }

}
