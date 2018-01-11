TS3 Remote App
--------------

This Android application serves as a remote view for a TeamSpeak 3 Client
running in the local network on a desktop PC. The connection to the TeamSpeak 3
Client requires the ClientQuery plugin running in the desktop client, which
opens a telnet port to which the Android application connects to. For this
to work the desktop PC needs to be in the same network as the Android device.
In the ClientQuery configuration the option "Open telnet port for everyone"
needs to be enabled, this opens the telnet port on 0.0.0.0 instead of
127.0.0.1, which is required to connect from another device.

All these requirements made the application only usable by people with some
knowledge of how their home network is working and who were able to read the
documentation. As a result, the rating in the play store was low because many
people expected a product which "just works", which this application is not by
design.

This app was originally written by an employee of TeamSpeak Systems GmbH and
released for free in the Google Play Store as "TS3 Remote".

Around 2015 the app was expanded with a full channel tree, but this version was
never released to the public.

In 2017 a security change to the ClientQuery plugin caused the application to
stop working, as it lacked a field to enter the API key created in the desktop
client, so the telnet connection from the Android app was refused by the
ClientQuery plugin in the desktop client.

TeamSpeak Systems GmbH does not consider this app a core product. To allow the
application to exist further for users who may still make good use of it,
TeamSpeak Systems GmbH added the required change for the API key and decided to
release the code as open source to the public, but decided against maintaining
the application themselves in the Google Play Store.
