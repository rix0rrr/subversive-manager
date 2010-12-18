package nl.rix0r.subversive.subversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A specialization of Configuration that can serialize itself to a
 * svn_access.conf file.
 *
 * @author rix0rrr
 */
public class DiskConfiguration extends Configuration {
    private File file;
    private List<String> warnings = Collections.emptyList();

    public DiskConfiguration(File file) {
        this.file = file;
    }

    public void load() throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        try {
            ConfigParser parser = new ConfigParser();

            String line;
            int number = 1;
            while ((line = r.readLine()) != null)
                parser.parse(number++, line);

        } finally {
            r.close();
        }
    }

    public void save() throws IOException {
    }

    public List<String> loadWarnings() {
        return warnings;
    }

    private static final Pattern blockPattern = Pattern.compile("^\\[([^\\]:]+)(:[^\\]]+)\\]$");
    private static final Pattern defPattern   = Pattern.compile("^([^=]+)=(.*)$");

    /**
     * Context relevant for parsing during loading of the file
     */
    protected class ConfigParser {
        private List<String> warnings = new LinkedList<String>();
        private boolean   inGroupBlock   = false;
        private Directory blockDirectory = null;

        /**
         * Parse a line from the config file
         *
         * Recognize the form of the line and dispatch to the proper
         * handler.
         */
        public void parse(int number, String line) {
            line = line.trim();
            if (line.equals(""))      return; // Nothing
            if (line.startsWith("#")) return; // Comment

            if (line.startsWith("[")) block(number, line);
            if (line.indexOf("=") != -1) definition(number, line);

            warning("Line " + number + ": not a comment, block or definition: " + line);
        }

        /**
         * Parse a line of the form [block]
         *
         * Recognized forms are are
         *
         *   [group]            Start of group definitions
         *   [/]                Repository index
         *   [REPO:/PATH]       Start of permission
         */
        private void block(int number, String line) {
            Matcher m = blockPattern.matcher(line);
            if (!m.matches()) {
                warning("Line " + number + ": invalid block: " + line);
                return;
            }

            inGroupBlock   = false;
            blockDirectory = null;
            if (m.groupCount() == 1) {
                if (m.group(1).equals("group")) {
                    inGroupBlock = true;
                    return;
                }

                if (m.group(1).equals("/")) {
                    blockDirectory = new Directory("", "/");
                    return;
                }

                warning("Line " + number + ": only [group] or [/] allowed in block: " + line);
                return;

            }

            assert m.groupCount() == 2;
            String repo = m.group(1);
            String path = m.group(2);
            blockDirectory = new Directory(repo, path);
        }

        private void definition(int number, String line) {
            Matcher m = defPattern.matcher(line);
            if (!m.matches()) {
                warning("Line " + number + ": invalid definition: " + line);
                return;
            }

            // Can't be in two kinds of groups at once
            assert !(inGroupBlock && blockDirectory != null);

            String definee    = m.group(1).trim();
            String definition = m.group(2).trim();

            if (definee.equals("")) {
                warning("Line " + number + ": left-hand side of assignment is empty: " + line);
                return;
            }

            if (inGroupBlock) {
                //
                // group = user, user, @group, user definition
                //

                GroupDefinition def = addGroup(constructGroup(definee));
                for (String username: definition.split(","))
                    def.addUser(constructUser(username.trim()));
            }

            if (blockDirectory != null) {
                //
                // principal = r/rw/ definition
                //
                Principal principal = constructPrincipal(definee);
                Access    access    = constructAccess(definition);
                if (access == null) {
                    warning("Line " + number + ": unrecognized access spec: " + access);
                    return;
                }

                addPermission(new Permission(blockDirectory, principal, access));
            }

            warning("Line " + number + ": definition outside of a [block]: " + line);
        }

        private void warning(String warning) {
            warnings.add(warning);
        }

        public List<String> warnings() {
            return Collections.unmodifiableList(warnings);
        }

        /**
         * Construct a Group instance from a "repo_group" name
         */
        protected Group constructGroup(String name) {
            String[] parts = name.split("_", 2);

            if (parts.length == 1) return new Group(decode(parts[0]));
            return new Group(parts[0], decode(parts[1]));
        }

        protected User constructUser(String username) {
            return new User(username);
        }

        protected Access constructAccess(String spec) {
            if (spec.equals("r")) return Access.Read;
            if (spec.equals("rw")) return Access.ReadWrite;
            if (spec.equals("")) return Access.Revoke;
            return null;
        }

        protected Principal constructPrincipal(String spec) {
            if (spec.startsWith("@")) return constructGroup(spec.substring(1));
            return constructUser(spec);
        }

        protected String decode(String name) {
            return name.replace("-", " ");
        }
    }
}
