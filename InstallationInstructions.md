# Installing an Application Server #

Subversive is a self-contained web application archive (.war). If you already have a Java application server, you can simply drop the war in and are ready to go.

If not, see here: InstallingApplicationServer.

# Installing Subversive #

  1. Deploy `subversive.war`. Jetty: simply drop the file into `/var/lib/jetty/webapps`. Tomcat or JBoss: deploy the file via the manager web interface.
  1. Copy `subversive.conf.dist` to `/etc/subversive.conf` and edit to your needs.

# Access to the Application #

Though you can have users access the application on the Application Server directly. you may want to put Subversive behind a Reverse Proxy, so it appears in the same URL space as the rest of your site and your Subversion repositories. You can also have the proxy server take care of HTTPS tunneling that way.

## Apache 2 ##
For Apache 2, the config looks like this:

```
ProxyPass /subversive http://localhost:8080/subversive
```

Be sure to add the following in /etc/apache2/mods-enabled/proxy.conf:

```
ProxyDomain .localhost
```

Otherwise Apache will generate a Redirect instead of transparently proxying the request.