package com.teamspeak.ts3remote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

public final class HelpViewActivity extends Activity {
    public static void openHelpView(final Context context) {
        context.startActivity(new Intent(context, HelpViewActivity.class));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.helpview);

        final WebView webView = (WebView)findViewById(R.id.helpWebView);
        final String html =
                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">" +
                "<html>" +
                "<head>" +
                "<title>TS3 Remote</title>" +
                "</head>" +
                "<body>" +
                "<h3>1) Setting up the TeamSpeak client</h3>" +
                "<ul style=\"margin-left:-1.2em;\">" +
                "<li>In your TeamSpeak client, open the Plugins dialog.</li>" +
                "<li>Ensure the ClientQuery plugin is enabled.</li>" +
                "<li>Open the ClientQuery settings dialog (double-click on the ClientQuery line or click the Settings button below)</li>" +
                "<li>Enable &quot;Open telnet port for everyone&quot;</li>" +
                "<li>Close the ClientQuery options dialog and reload the ClientQuery plugin (&quot;Reload&quot; from the ClientQuery contextmenu or just click &quot;Reload all&quot;)<br>" +
                "The Windows firewall might ask you if you want to allow connections. Allow them, else your Android phone cannot connect.</li>" +
                "</ul>" +
                "<h3>2) Setting up TS3 Remote on Android</h3>" +
                "<ul style=\"margin-left:-1.2em;\">" +
                "<li>Start TS3 Remote and enter the IP to your PC where the TeamSpeak client is running.</li>" +
                "<li>If you want to change the IP dialog later, select &quot;Connect&quot; in the menu.</li>" +
                "</ul>" +
                "<p>You can see your IP using the &quot;ipconfig&quot; command in a DOS shell on Windows, or &quot;ifconfig&quot; in a Linux shell.</p>" +
                "<p>For example ipconfig on Windows might tell:<br></p>" +
                "<pre>Ethernet-Adapter LAN-Verbindung:<br>" +
                "<br>" +
                "Verbindungsspezifisches DNS-Suffix:<br>" +
                "IPv4-Adresse . . . : 192.168.1.10<br>" +
                "Subnetzmaske . . . : 255.255.255.0<br></pre>" +
                "<p>Then you would enter &quot;192.168.1.10&quot; into TS3 Remote. If the connection fails, either the IP was wrong, your firewall is blocking the connection, WLAN is disabled on your phone or the WLAN network cannot reach your PC.</p>" +
                "<p style=\"margin-top:2em;font-size:small;\">Press the &quot;Back&quot; button to return to the previous view.</p>" +
                "</body>" +
                "</html>";

        webView.loadData(html, "text/html", null);
    }
}
