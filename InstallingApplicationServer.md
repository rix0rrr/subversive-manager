# Introduction #

You need a J2EE server to run Subversive on, such as Tomcat, Jetty, JBoss, GlassFish or any other popular servers. This page shows you how to install such a server.

# Jetty on Debian #

My favorite J2EE server is Jetty, as it's very lightweight. It's also very easy to install on Debian.

```
apt-get install jetty

Edit /etc/default/jetty
  NO_START=0

/etc/init.d/jetty start
```

Done! By default, Debian's Jetty is set up to only allow connections from localhost. You can change this by editing /etc/default/jetty. Or put Subversive behind an Apache Reverse Proxy (see [InstallationInstructions](InstallationInstructions.md)).