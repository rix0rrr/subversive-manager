
package nl.rix0r.subversive.subversion;

/**
 * Class to generate Configuration objects for testing
 *
 * @author rix0rrr
 */
public class TestConfigurationBuilder {
    private Configuration config = new Configuration();
    private Directory directory = null;

    public TestConfigurationBuilder group(Group group, User... users) {
        GroupDefinition gd = config.addGroup(group);
        for (User user: users)
            gd.addUser(user);
        return this;
    }

    public TestConfigurationBuilder directory(String repository, String path) {
        this.directory = new Directory(repository, path);
        return this;
    }

    public TestConfigurationBuilder permission(Principal principal, Access access) {
        if (directory == null) throw new RuntimeException("Call directory() before calling permission().");
        config.addPermission(new Permission(directory, principal, access));
        return this;
    }

    public Configuration build() {
        return config;
    }
}
