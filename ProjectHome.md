Light-Weight Publish Subscribe (lwps) arose from a need to provide a simple abstraction for the publish/subscribe messaging model. The library is written in Java and provides some useful features such as multiple sessions, specifying arbitrary properties for each session and sending multi-line messages.

In addition, you can setup an HTTP proxy (implemented as servlets) to send and receive messages over HTTP. You'll need a servlet container such as Tomcat or JBoss to use this method.

A port of the library for mobile phones (J2ME) is also available. This allows the library to be used on J2ME clients. As in the normal version, you can either connect directly or via the HTTP proxy.