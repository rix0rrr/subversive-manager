package nl.rix0r.subversive.server;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import nl.rix0r.subversive.subversion.User;
import org.apache.commons.io.FileUtils;
import org.mortbay.jetty.security.B64Code;
import org.mortbay.jetty.security.UnixCrypt;

/**
 * Credentials authority that reads its user database from a htpasswd-formatted
 * file
 *
 * This is a plain text file of the form:
 *
 *   user1:hash1
 *   user2:hash2
 *
 * Where hashes are either crypt()ed, MD5'ed or SHA'ed.
 *
 * MD5 is not supported as Apache uses some kind of non-standard implementation.
 *
 * @author rix0rrr
 */
public class HtPasswdAuthority implements CredentialsAuthority {
    private final File file;

    public HtPasswdAuthority(File file) {
        if (!file.exists()) throw new RuntimeException("User password file does not exist: " + file);
        if (!file.canRead()) throw new RuntimeException("User password file not readable: " + file);
        this.file = file;
    }

    /**
     * Verify username and password from the htpasswd file
     */
    public boolean authenticate(String username, String password) {
        if (username == null || username.equals("") || password == null) return false;

        try {
            List<String> lines = FileUtils.readLines(file);
            for (String line: lines)
                if (line.startsWith(username + ":"))
                    return verifyPasswordHash(password, line.substring(username.length() + 1));

            return false; // Not found
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Verify whether the hash of the given password equals the stored hash
     */
    static boolean verifyPasswordHash(String password, String hash) {
        if (hash.startsWith("$apr1$")) {
            // MD5 hash
            throw new RuntimeException("MD5 htpasswords not supported! They use an Apache-specific hash.");
        }

        if (hash.startsWith("{SHA}")) {
            // SHA hash
            String cred = new String(B64Code.encode(digest("sha", password)));
            return cred.equals(hash.substring(5));
        }

        // Default: crypt hash
        String salt = hash.substring(0, 2);
        String cred = UnixCrypt.crypt(password, salt);
        return cred.equals(hash);
    }

    private static byte[] digest(String algo, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algo);
            digest.update(password.getBytes());
            return digest.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }


    public List<User> findUsers(String like) {
        List<User> ret = new ArrayList<User>();
        for (User u: allUsers())
            if (u.matches(like))
                ret.add(u);
        return ret;
    }

    public List<User> initialSet() {
        return allUsers();
    }

    private List<User> allUsers() {
        try {
            List<String> lines  = FileUtils.readLines(file);

            List<User>   result = new ArrayList<User>(lines.size());
            for (String line: lines) {
                if (line.startsWith("#")) continue;
                int ix = line.indexOf(":");
                if (ix > 0)
                    result.add(new User(line.substring(0, ix)));
            }

            return result;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
