package nl.rix0r.subversive.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.rix0r.subversive.subversion.Access;
import nl.rix0r.subversive.subversion.Anonymous;
import nl.rix0r.subversive.subversion.Configuration;
import nl.rix0r.subversive.subversion.Directory;
import nl.rix0r.subversive.subversion.Group;
import nl.rix0r.subversive.subversion.GroupDefinition;
import nl.rix0r.subversive.subversion.Permission;
import nl.rix0r.subversive.subversion.Principal;
import nl.rix0r.subversive.subversion.User;

/**
 * A specialization of Configuration that can serialize itself to a
 * svn_access.conf file.
 *
 * FIXME: Preserve comments in the file.
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
        Reader r = new InputStreamReader(new FileInputStream(file));
        try {
            load(r);
        } finally {
            r.close();
        }
    }

    protected void load(Reader reader) throws IOException {
        BufferedReader r = new BufferedReader(reader);
        ConfigParser parser = new ConfigParser();

        String line;
        int number = 1;
        while ((line = r.readLine()) != null)
            parser.parse(number++, line);

        warnings = parser.warnings;
    }

    public void save() throws IOException {
        Writer w = new OutputStreamWriter(new FileOutputStream(file));
        try {
            save(w);
        } finally {
            w.close();
        }
    }

    protected void save(Writer writer) throws IOException {
        ConfigWriter w = new ConfigWriter(writer);

        w.startGroupDefinitions();
        for (GroupDefinition gd: groupDefinitions())
            w.groupDefinition(gd);

        for (Permission p: permissions(null, null))
            w.permission(p);
    }

    public List<String> loadWarnings() {
        return warnings;
    }

    protected static final Pattern blockPattern = Pattern.compile("^\\[  ([^\\]:]+) (?: : ([^\\]]+))?  \\]$", Pattern.COMMENTS);
    protected static final Pattern defPattern   = Pattern.compile("^([^=]+)=(.*)$");

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

            if (line.startsWith("["))
                block(number, line);
            else if(line.indexOf("=") != -1)
                definition(number, line);
            else
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
            if (m.groupCount() == 1 || m.group(2) == null)
                simpleBlock(m.group(1), number, line);
            else
                directoryBlock(m.group(1), m.group(2));
        }

        /**
         * Parse a block of the form [groups] or [/]
         */
        private void simpleBlock(String contents, int number, String line) {
            if (contents.equals("groups"))
                inGroupBlock = true;

            else if (contents.equals("/"))
                blockDirectory = new Directory("", "/");

            else
                warning("Line " + number + ": only [groups] or [/] allowed in block: " + line);
        }

        private void directoryBlock(String repo, String path) {
            blockDirectory = new Directory(repo, path);
        }

        private void definition(int number, String line) {
            Matcher m = defPattern.matcher(line);
            if (!m.matches()) {
                warning("Line " + number + ": invalid definition: " + line);
                return;
            }

            String definee    = m.group(1).trim();
            String definition = m.group(2).trim();

            if (definee.equals("")) {
                warning("Line " + number + ": left-hand side of assignment is empty: " + line);
                return;
            }

            // Can't be in two kinds of groups at once
            assert !(inGroupBlock && blockDirectory != null);

            if (inGroupBlock)
                groupDefinition(definee, definition);
            else if(blockDirectory != null)
                accessDefinition(definee, definition, number);
            else
                warning("Line " + number + ": definition outside of a [block]: " + line);
        }

        private void groupDefinition(String groupName, String members) {
            //
            // group = user, user, @group, user definition
            //
            Group group = ConfigGroup.deserialize(groupName);
            for (String username: members.split(","))
                addUserToGroup(group, ConfigUser.deserialize(username.trim()));
        }

        private void accessDefinition(String principalDef, String accessCode, int number) {
            //
            // principal = r/rw/ definition
            //
            Principal principal = ConfigPrincipal.deserialize(principalDef);
            Access    access    = ConfigAccess.deserialize(accessCode);
            if (access == null) {
                warning("Line " + number + ": unrecognized access spec: " + access);
                return;
            }

            addPermission(new Permission(blockDirectory, principal, access));
        }

        private void warning(String warning) {
            warnings.add(warning);
        }

        public List<String> warnings() {
            return Collections.unmodifiableList(warnings);
        }

    }

    /**
     * A class that writes a memory configuration to disk
     */
    protected class ConfigWriter {
        private final PrintWriter writer;
        private Directory lastDirectory;

        public ConfigWriter(Writer writer) {
            this.writer = new PrintWriter(writer);
        }

        public void startGroupDefinitions() {
            writer.println("[groups]");
        }

        public void groupDefinition(GroupDefinition groupDefinition) {
            writer.print(ConfigGroup.serialize(groupDefinition.group()));
            writer.print(" = ");

            boolean comma = false;

            for (User user: groupDefinition.users()) {
                if (comma) writer.print(",");
                comma = true;

                writer.print(ConfigUser.serialize(user));
            }

            writer.println();
        }

        private void directory(Directory directory) {
            if (lastDirectory != null && lastDirectory.equals(directory)) return;

            writer.println("[" + directory.repository() + ":" + directory.path() + "]");
            lastDirectory = directory;
        }

        /**
         * Writes a given Permission line to the file
         *
         * Prepends a directory header if the current Permission's directory
         * is different than the last one's. Requires that permissions are
         * presented in a sorted fashion.
         */
        public void permission(Permission permission) {
            directory(permission.directory());

            writer.print(ConfigPrincipal.serialize(permission.principal()));
            writer.print("=");
            writer.print(ConfigAccess.serialize(permission.access()));
            writer.println();
        }
    }

    private static class ConfigEntity {
        protected static String decode(String name) {
            return name.replace("-", " ");
        }

        protected static String encode(String name) {
            return name.replace(" ", "-");
        }
    }

    private static class ConfigGroup extends ConfigEntity {
        /**
         * Construct a Group instance from a "repo_group" name
         */
        protected static Group deserialize(String name) {
            String[] parts = name.split("_", 2);

            if (parts.length == 1) return new Group(decode(parts[0]));
            return new Group(decode(parts[0]), decode(parts[1]));
        }

        protected static String serialize(Group group) {
            if (group.global()) return encode(group.name());
            return encode(group.repository()) + "_" + encode(group.name());
        }
    }

    private static class ConfigUser extends ConfigEntity {
        protected static User deserialize(String username) {
            return new User(decode(username));
        }

        protected static String serialize(User user) {
            return encode(user.username());
        }
    }

    private static class ConfigPrincipal extends ConfigEntity {
        protected static Principal deserialize(String spec) {
            spec = spec.trim();

            if (spec.startsWith("@")) return ConfigGroup.deserialize(decode(spec.substring(1)));
            if (spec.equals("*")) return new Anonymous();
            return ConfigUser.deserialize(decode(spec));
        }

        protected static String serialize(Principal principal) {
            if (principal instanceof Group) return "@" + encode(((Group)principal).name());
            if (principal instanceof Anonymous) return "*";
            assert principal instanceof User;
            return encode(((User)principal).username());
        }
    }

    private static class ConfigAccess extends ConfigEntity {
        protected static Access deserialize(String spec) {
            if (spec.equals("r")) return Access.Read;
            if (spec.equals("rw")) return Access.ReadWrite;
            if (spec.equals("")) return Access.Revoke;
            return null;
        }

        protected static String serialize(Access access) {
            if (access == Access.Read) return "r";
            if (access == Access.ReadWrite) return "rw";
            return "";
        }
    }
}
