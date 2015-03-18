# Subversive: The Subversion configuration manager #

Subversive is an editor for Subversion configuration files: it has been
specifically designed to delegate the management of repository configurations
to end-users, so system administrators don't have to deal with this. This is
especially useful in large organizations, where the people using the
repositories are usually not the people running the servers.

Subversive gives people the ability to edit the access rights to their own
repositories directly, preventing them from making mistakes and also
isolates them from accessing repositories they shouldn't be messing with.

## Design Goals/Features ##

  * **Works without a database.** Reads and writes the Subversion configuration file directly.

  * **Does not destroy manual edits.** System administrators still have the ability to edit the Subversion configuration file manually with a text editor; their changes will be picked up by Subversive, not overwritten on the next access.

  * **Supports LDAP.** Users can be authenticated against and user lists retrieved from both a .htpasswd file as used by Apache (typically used as a WebDAV/Subversion server), as well as an LDAP server. This makes Subversive suitable for both small and large organizations.

(Please read the PermissionsModel page first to determine if Subversive's access model is a good fit for your organization).

## Requirements ##

  * Requires a Java application server, such as Tomcat or Jetty.

## Not supported but planned ##

  * Creation of repositories directly from tool
  * Hook script management
