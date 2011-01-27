
package nl.rix0r.subversive.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import nl.rix0r.subversive.subversion.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author rix0rrr
 */
public class LdapAuthorityTest {
    private LdapAuthority ldap;

    @Before
    public void setUp() {
        ldap = new LdapAuthority("ldap://ldap.testathon.net:389/dc=testathon,dc=net");
        ldap.setSearchLogin("CN=stuart,OU=users,DC=testathon,DC=net", "stuart");
    }

    @Test
    public void interestingFields() {
        Assert.assertEquals(
                new HashSet<String>(Arrays.asList(new String[] { "fullname", "cn", "uid" })),
                new HashSet<String>(Arrays.asList(ldap.interestingFields())));
    }

    @Test
    public void search() throws Exception {
        Collection<User> user = ldap.findUsers("is");
        Assert.assertEquals(3, user.size());
    }

    @Test
    public void searchWithNastyChars() throws Exception {
        Collection<User> user = ldap.findUsers("s o*())");
        Assert.assertEquals(0, user.size());
    }
}
