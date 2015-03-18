Subversive makes some assumptions about the way permissions are structured in your svn\_access file, which may or may not be compatible with the way your permissions are currently structured.

## Groups ##

There are two types of groups:

  * Local groups: these groups have the name of the repository in them, such as Project.ReadOnly, or Newspaper.Editors. We consider local groups to be scoped to the repository in their name so they should only be assigned in that repository. The Subversive interface will not allow you to assign these groups in other repositories. Of course, you could still do this manually, but you'd better not. The advantage of local groups is that they can be edited by repository owners without influencing other repositories.

  * Global groups: groups without a repository name in them are global groups.  They cannot be edited from within Subversive, but they can be assigned permissions in repositories.

## Roles ##

Subversive distinguishes between two types of users:

  * Repository Owners; can edit all permissions and groups of the repositories they "own".

  * Administrators: can edit all repositories.

A user's role is determined by their membership of certain groups with special names. By default, all users in the global group "Admins" are considered Administrators, and every user in the local repository groups named "Owners" are considered Owner of that repository.

Since the Owners group is also a regular local group, it can be edited by the owners as well, which means they can assign ownership to other people.