
package nl.rix0r.subversive.server;

import org.junit.Test;
import org.junit.Assert;

/**
 *
 * @author rix0rrr
 */
public class HtPasswdAuthorityTest {

    @Test
    public void testCrypt() {
        Assert.assertTrue(HtPasswdAuthority.verifyPasswordHash("test", "KAKXKb3yF3MKw"));
    }

    @Test
    public void testSha() {
        Assert.assertTrue(HtPasswdAuthority.verifyPasswordHash("test", "{SHA}qUqP5cyxm6YcTAhz05Hph5gvu9M="));
    }

}
