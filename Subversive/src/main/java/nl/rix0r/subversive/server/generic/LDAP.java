package nl.rix0r.subversive.server.generic;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * LDAP client
 *
 * @author rix0rrr
 */
public class LDAP {
    private final String url;
    private final String userDn;
    private final String password;

    private int maxSearchResults = 50;

    private DirContext context;

    /**
     * Anonymous binding
     */
    public LDAP(String url) {
        this(url, "", "");
    }

    /**
     * Named binding
     */
    public LDAP(String url, String userDn, String password) {
        this.url      = url;
        this.userDn   = userDn   != null ? userDn : "";
        this.password = password != null ? password : "";
    }

    /**
     * Bind using the given credentials
     */
    private void bind() throws NamingException {
        if (context != null) return;

        Hashtable<String, String> env = new Hashtable<String, String>();

        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);
        if (userDn.equals("")) {
            env.put(Context.SECURITY_AUTHENTICATION, "none");
        }
        else {
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, userDn);
            env.put(Context.SECURITY_CREDENTIALS, password);
        }

        context = new InitialDirContext(env);
    }

    /**
     * Try authentication
     *
     * Returns in case of success, throws an exception on failure
     */
    public void authenticate() {
        try {
            bind();
        } catch (NamingException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Return true if the password with which the class was instantiated
     * is correct, false otherwise
     */
    public boolean validPassword() {
        try {
            bind();
            return !password.equals("");
        } catch (NamingException ex) {
            return false;
        }
    }

    public String findUserDn(String uid) {
        return findUserDn(uid, "uid");
    }

    /**
     * Find the DN of an object given a UID
     *
     * Call this on an anonymously bound LDAP server to obtain
     * the DN to use for authentication.
     */
    public String findUserDn(String uid, String searchField) {
        try {
            bind();

            SearchControls ctrls = new SearchControls();
            ctrls.setReturningAttributes(new String[] {});
            ctrls.setCountLimit(1);
            ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            String filter = "(&(" + escape(searchField) + "=" + escape(uid) + "))";
            NamingEnumeration<SearchResult> answer = context.search("", filter, ctrls);

            if (answer.hasMore())
                return answer.next().getNameInNamespace();

            return "";
        } catch (NamingException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Return a map of given properties for the identified object
     */
    public Map<String, String> getProperties(String dn, String... properties) {
        try {
            bind();

            Attributes as = context.getAttributes(dn, properties);
            return makeMap(as, properties);

        } catch (NamingException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private Map<String, String> makeMap(Attributes attributes, String... properties) throws NamingException {
        Map<String, String> ret = new HashMap<String, String>();

        for (String property: properties) {
            if (property == null) throw new IllegalArgumentException("Passed a null property to makeMap");

            String value = "";
            Attribute a = attributes.get(property);
            if (a != null && a.get() != null) value = a.get().toString();
            ret.put(property, value);
        }

        return ret;
    }

    private String[] extendArray(String[] original, String... added) {
        String[] ret = new String[original.length + added.length];
        for (int i = 0; i < original.length; i++) ret[i] = original[i];
        for (int i = 0; i < added.length; i++) ret[original.length + i] = added[i];
        return ret;
    }

    /**
     * Search the given fields for the given string
     *
     * Won't return more than maxSearchResults entries
     */
    public List<Map<String, String>> search(String like, String[] fields, String... properties) {
        if (like == null || like.equals("")) return new ArrayList<Map<String, String>>();
        
        try {
            bind();

            SearchControls ctrls = new SearchControls();
            ctrls.setReturningAttributes(properties);
            ctrls.setCountLimit(maxSearchResults);
            ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            String filter = buildFilterString(like, fields);
            NamingEnumeration<SearchResult> answer = context.search("", filter, ctrls);

            List<Map<String, String>> ret = new ArrayList<Map<String, String>>();

            while (answer.hasMore()) {
                SearchResult sr = (SearchResult)answer.next();
                String dn       = sr.getNameInNamespace();

                Map<String, String> props = makeMap(sr.getAttributes(), properties);
                props.put("dn", dn != null ? dn : "");
                ret.add(props);
            }

            return ret;
        } catch (NamingException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private String buildFilterString(String like, String... fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("(|");
        for (String field: fields)
            if (field != null && !field.equals(""))
                sb.append("(").append(escape(field.trim())).append("=*").append(escape(like.trim())).append("*)");

        sb.append(")");
        return sb.toString();
    }

    public static void validateCredentials(String url, String searchField, String username, String password) {
        validateCredentials(url, searchField, username, password, "", "");
    }

    /**
     * Complete credentials validating procedure
     *
     * First, binds with the search user (or anonymously) to find the DN of
     * the desired user object. Then try to bind with the found user DN and the
     * password to authenticate them.
     */
    public static void validateCredentials(String url, String searchField, String username, String password, String searchUserDn, String searchPassword) {
        LDAP searchLdap = new LDAP(url, searchUserDn, searchPassword);
        try {
            String userDn = searchLdap.findUserDn(username, searchField);
            if (userDn.equals("")) throw new RuntimeException("Username not found: " + username);

            LDAP authLdap = new LDAP(url, userDn, password);
            try {
                authLdap.authenticate();
            } finally {
                authLdap.close();
            }
        } finally {
            searchLdap.close();
        }
    }

    @Override
    public void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private static String escape(String x) {
       StringBuilder sb = new StringBuilder();
       for (int i = 0; i < x.length(); i++) {
           char curChar = x.charAt(i);
           switch (curChar) {
               case '\\':
                   sb.append("\\5c");
                   break;
               case '*':
                   sb.append("\\2a");
                   break;
               case '(':
                   sb.append("\\28");
                   break;
               case ')':
                   sb.append("\\29");
                   break;
               case '\u0000':
                   sb.append("\\00");
                   break;
               default:
                   sb.append(curChar);
           }
       }
       return sb.toString();
    }

    public void close() {
        try {
            if (context != null) {
                context.close();
                context = null;
            }
        } catch (Exception e) {
            // Swallow the cleanup exception
        }
    }

}
