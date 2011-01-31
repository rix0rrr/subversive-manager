
package nl.rix0r.subversive.server.generic;

import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test my LDAP class against Stuart Lewis' open LDAP server
 *
 * http://blog.stuartlewis.com/2008/07/07/test-ldap-service/
 * http://blog.stuartlewis.com/2008/08/18/test-ldap-service-upgraded-now-with-branches/
 *
 * His LDAP server doesn't allow anonymous search, so we use
 * a special user for that.
 *
 * @author rix0rrr
 */
public class LDAPTest {
    String url = "ldap://ldap.testathon.net:389/dc=testathon,dc=net";
    String search_user = "CN=stuart,OU=users,DC=testathon,DC=net";
    String search_pass = "stuart";

    @Test
    public void testFindDn() {
        String dn = new LDAP(url, search_user, search_pass).findUserDn("stuart", "cn");
        Assert.assertEquals("cn=stuart,ou=users,dc=testathon,dc=net", dn.toLowerCase());
    }

    @Test
    public void testFindDnDeep() {
        String dn = new LDAP(url, search_user, search_pass).findUserDn("alice", "cn");
        Assert.assertEquals("cn=alice,ou=staff,ou=users,dc=testathon,dc=net", dn.toLowerCase());
    }

    @Test
    public void testAuthenticate() {
        LDAP.validateCredentials(url, "cn", "alice", "alice", search_user, search_pass);
    }

    @Test
    public void testSearch() {
        List<Map<String, String>> found = new LDAP(url, search_user, search_pass)
                .search("is", new String[] { "cn", "uid" },
                new String[] { "cn", "fullName", "sn", "uid" });

        // Stuart, Dennis & Francis
        Assert.assertEquals(3, found.size());
    }
}
