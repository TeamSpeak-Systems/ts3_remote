package com.teamspeak.ts3remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import android.content.Context;
import android.util.Log;
import com.teamspeak.ts3remote.data.Channel;
import com.teamspeak.ts3remote.data.Client;
import com.teamspeak.ts3remote.data.Common;
import com.teamspeak.ts3remote.data.ImmutableChannel;
import com.teamspeak.ts3remote.data.ImmutableClient;

final class ClientQueryParser extends Thread implements Common {

    private enum State {
        DISCONNECTED,
        HANDSHAKING,
        INITIALIZING,
        IDLE
    }

    private State state = State.DISCONNECTED;

    private static final int antiIdleTimerDelay = 300000;  // 5 minutes

    /**
     * Host (Where the TS Client runs)
     */
    private String host;

    /**
     * AuthKey which is required by the
     */
    private String authKey;

    /**
     * Port to connect to.
     */
    private static final int port = 25639;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String sentCmd = "";
    private final Queue<String> commandsToSend = new LinkedList<String>();
    private final TS3EventSender ts3EventSender = new TS3EventSender();
    private final TS3Data ts3Data = new TS3Data(ts3EventSender);
    private final Timer antiIdleTimer = new Timer();
    private final Context context;
    private boolean wantAutoreconnect = false;
    private boolean disconnectRequested = false;
    private int autoreconnectCounter = 0;

    ClientQueryParser(Context context) {
        this.context = context;
        antiIdleTimer.schedule(new AntiIdleTimerTask(), antiIdleTimerDelay, antiIdleTimerDelay);  // Start anti-idle timer
    }

    TS3EventSender getTS3EventSender() {
        return ts3EventSender;
    }

    TS3Data getTS3Data() {
        return ts3Data;
    }

    void setHost(final String host) {
        this.host = host;
    }

    void setAuthKey(final String authKey) {
        this.authKey = authKey;
    }

    /**
     * Disconnect from client.
     */
    void disconnect() {
        wantAutoreconnect = false;
        disconnectRequested = true;
        if(socket != null) {
            try {
                socket.close();  // Note: Cannot reuse socket anymore, so we must not autoreconnect
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }

    /**
     * Join a channel
     * Command
     * @param channelID
     * @param clientID
     */
    void joinChannel(final long channelID, final int clientID) {
        new AsyncTSNetworkingTask(this).execute("clientmove cid=" + channelID + " clid=" + clientID);
    }

    /**
     * Subscribe a channel
     * Command
     * @param channelID
     */
    void subscribeChannel(final long channelID) {
        new AsyncTSNetworkingTask(this).execute("channelsubscribe cid=" + channelID);
    }

    /**
     * Unsubscribe a channel
     * Command
     * @param channelID
     */
    void unsubscribeChannel(final long channelID) {
        new AsyncTSNetworkingTask(this).execute("channelunsubscribe cid=" + channelID);
    }

    /**
     * Subscribe all channels
     * Command
     */
    void subscribeAll() {
        new AsyncTSNetworkingTask(this).execute("channelsubscribeall");
    }

    /**
     * Unsubscribe all channels
     * Command
     */
    void unsubscribeAll() {
        new AsyncTSNetworkingTask(this).execute("channelunsubscribeall");
    }

    /**
     * Connect to a client.
     * @param host
     * @param port
     * @return
     */
    private boolean connect(final String host, final int port) {
        Log.d(TAG, "Connecting to " + host + ":" + port);
        if(host.length() == 0) {
            return false;
        }

        socket = new Socket();

        try {
            socket.connect(new InetSocketAddress(host, port), 2000);  // 2 seconds timeout
        } catch (IOException e) {
            Log.d(TAG, "EXCEPTION: connect failed: " + e.getMessage());
            //e.printStackTrace();
            return false;
        }

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()), 4096);
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch(IOException e) {
            Log.d(TAG, "EXCEPTION: creating i/o failed: " + e.getMessage());
            //e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Close Connection
     */
    private void closeConnection() {
        if(socket == null) {
            return;
        }

        try {
            in.close();
            out.close();
            socket.close();
        } catch(IOException e) {
            //e.printStackTrace();
        }

        socket = null;
        in = null;
        out = null;
        state = State.DISCONNECTED;
    }

    /**
     * Send Command.
     * Will be executed by AsyncTSNetworkingTask.
     * @param message
     */
    public synchronized void send(final String message) {
        if(out == null)
            return;
        Log.d(TAG, "> " + message);
        sentCmd = message;
        out.println(message);
    }

    private String getParamValue(final String group, final String key) {
        int start = group.indexOf(key + "=");
        if(start != -1) {
            start = start + key.length() + 1;
            final int end = group.indexOf(" ", start);
            if(end != -1 && end < group.length())
                return group.substring(start, end);
            return group.substring(start);
        }
        return "";
    }

    private int getParamValueAsInt(final String group, final String key) {
        try {
            return Integer.parseInt(getParamValue(group, key));
        } catch(NumberFormatException ex) {
            return 0;
        }
    }

    private String unescapeString(final String s) {
        return s.replace("\\\\", "\\").replace("\\/", "/").replace("\\s", " ").replace("\\p", "|");
    }

    private final class VoiceAwayResult {
        final int talking;
        final int inputMuted;
        final int hardwareMuted;
        final int outputMuted;
        final int hardwareOutputMuted;
        final int away;
        final int talkPower;
        final int isTalker;

        VoiceAwayResult(final int talking, final int inputMuted, final int hardwareMuted, final int outputMuted, final int hardwareOutputMuted, final int away, final int talkPower, final int isTalker) {
            this.talking = talking;
            this.inputMuted = inputMuted;
            this.hardwareMuted = hardwareMuted;
            this.outputMuted = outputMuted;
            this.hardwareOutputMuted = hardwareOutputMuted;
            this.away = away;
            this.talkPower = talkPower;
            this.isTalker = isTalker;
        }
    }

    private VoiceAwayResult parseVoiceAway(final String group) {
        int talking = -1;
        int inputMuted = -1;
        int hardwareMuted = -1;
        int outputMuted = -1;
        int hardwareOutputMuted = -1;
        int away = -1;
        int talkPower = -1;
        int isTalker = -1;

        final String client_flag_talking = getParamValue(group, "client_flag_talking");
        final String client_input_muted = getParamValue(group, "client_input_muted");
        final String client_input_hardware = getParamValue(group, "client_input_hardware");
        final String client_output_muted = getParamValue(group, "client_output_muted");
        final String client_output_hardware = getParamValue(group, "client_output_hardware");
        final String client_away = getParamValue(group, "client_away");
        final String client_talk_power = getParamValue(group, "client_talk_power");
        final String client_is_talker = getParamValue(group, "client_is_talker");

        if(client_flag_talking.length() > 0) {
            talking = Integer.parseInt(client_flag_talking);
        }
        if(client_input_muted.length() > 0) {
            inputMuted = Integer.parseInt(client_input_muted);
        }
        if(client_input_hardware.length() > 0) {
            hardwareMuted = Integer.parseInt(client_input_hardware);
        }
        if(client_output_muted.length() > 0) {
            outputMuted = Integer.parseInt(client_output_muted);
        }
        if(client_output_hardware.length() > 0) {
            hardwareOutputMuted = Integer.parseInt(client_output_hardware);
        }
        if(client_away.length() > 0) {
            away = Integer.parseInt(client_away);
        }
        if(client_talk_power.length() > 0) {
            talkPower = Integer.parseInt(client_talk_power);
        }
        if(client_is_talker.length() > 0) {
            isTalker = Integer.parseInt(client_is_talker);
        }
        return new VoiceAwayResult(talking, inputMuted, hardwareMuted, outputMuted, hardwareOutputMuted, away, talkPower, isTalker);
    }

    /**
     * Polls from ClientQueryPlugin and processes information.
     * @param line
     */
    private void receiveLine(final String line) {
        Log.d(TAG, "$ " + line);

        String first = "";
        int pos = line.indexOf(' ');
        if(pos != -1) {
            String s = line.substring(0, pos);
            pos = s.indexOf('=');
            if(pos == -1) {
                first = s;
            }
        }
        final String[] groups = line.split("\\|");

        Log.d(TAG, "first = " + first);

        if(first.equals("selected")) {
            if(state != State.HANDSHAKING) {  // Not when connecting
                commandsToSend.add("clientnotifyunregister");
            }

            ts3Data.setCurrentScHandlerID(getParamValueAsInt(groups[0], "schandlerid"));

            commandsToSend.add("auth apikey=" + authKey);
            commandsToSend.add("clientnotifyregister schandlerid=" + ts3Data.getCurrentScHandlerID() + " event=notifytalkstatuschange");
            commandsToSend.add("clientnotifyregister schandlerid=" + ts3Data.getCurrentScHandlerID() + " event=notifycliententerview");
            commandsToSend.add("clientnotifyregister schandlerid=" + ts3Data.getCurrentScHandlerID() + " event=notifyclientleftview");
            commandsToSend.add("clientnotifyregister schandlerid=" + ts3Data.getCurrentScHandlerID() + " event=notifyclientmoved");
            commandsToSend.add("clientnotifyregister schandlerid=" + ts3Data.getCurrentScHandlerID() + " event=notifyclientupdated");
            commandsToSend.add("clientnotifyregister schandlerid=" + ts3Data.getCurrentScHandlerID() + " event=notifyconnectstatuschange");
            commandsToSend.add("clientnotifyregister schandlerid=" + ts3Data.getCurrentScHandlerID() + " event=notifychannelcreated");
            commandsToSend.add("clientnotifyregister schandlerid=" + ts3Data.getCurrentScHandlerID() + " event=notifychanneledited");
            commandsToSend.add("clientnotifyregister schandlerid=" + ts3Data.getCurrentScHandlerID() + " event=notifychanneldeleted");
            commandsToSend.add("clientnotifyregister schandlerid=" + ts3Data.getCurrentScHandlerID() + " event=notifychannelmoved");
            commandsToSend.add("clientnotifyregister schandlerid=" + ts3Data.getCurrentScHandlerID() + " event=notifychannelsubscribed");
            commandsToSend.add("clientnotifyregister schandlerid=" + ts3Data.getCurrentScHandlerID() + " event=notifychannelunsubscribed");
            commandsToSend.add("clientnotifyregister schandlerid=0 event=notifycurrentserverconnectionchanged");  // Listen to all tabs
            if(!commandsToSend.contains("servervariable virtualserver_name"))
                commandsToSend.add("servervariable virtualserver_name");
            if(!commandsToSend.contains("whoami"))
                commandsToSend.add("whoami");
            // Followed by "channellist", added dynamically in reply to whoami
            // Followed by "clientlist -away -voice -country", added dynamically in reply to channellist

            if(state == State.HANDSHAKING) {  // No error on first connect, so this would not be sent off below
                send(commandsToSend.poll());
            }

            ts3EventSender.fireTS3Event(new TS3StatusTextEvent(this, ""));
            state = State.INITIALIZING;
            return;
        }

        // Error
        if(first.equals("error")) {  // Ready to send off next command?
        	Log.d(TAG, "Got error, current state is " + state);
            final int id = getParamValueAsInt(groups[0], "id");
            if(id == 1794) {  // Not connected
                ts3Data.clear();
                commandsToSend.clear();
                fireDisconnectEvents(R.string.no_server);
            } else if (id == 1538 && state == State.INITIALIZING) {
            	// Did we just sent auth?
            	Log.d(TAG, "Got error 1538 when initializing");
            	ts3Data.clear();
                commandsToSend.clear();
                fireDisconnectEvents(R.string.invalid_auth_key);
            } else if(!commandsToSend.isEmpty()) {
                send(commandsToSend.poll());
            }
            return;
        }

        // Server tab changed
        if(first.equals("notifycurrentserverconnectionchanged")) {
            final String scHandlerID = getParamValue(groups[0], "schandlerid");
            ts3Data.clear();
            commandsToSend.clear();
            ts3EventSender.fireTS3Event(new TS3ClearEvent(this));
            state = State.INITIALIZING;
            send("use " + scHandlerID);
            return;
        }

        // Connect status changed
        if(first.equals("notifyconnectstatuschange")) {
            final String status = getParamValue(groups[0], "status");
            if(status.equals("connected")) {
                ts3Data.clear();
                ts3EventSender.fireTS3Event(new TS3StatusTextEvent(this, ""));
                // Do not clear commandsToSend here, we might prevent all clientnotifyregister being sent
                state = State.INITIALIZING;
            } else if(status.equals("connection_established")) {
                if(!commandsToSend.contains("servervariable virtualserver_name") && !commandsToSend.contains("whoami")) {  // Prevent sending this twice
                    commandsToSend.add("whoami");
                    send("servervariable virtualserver_name");  // First command actually
                }
            } else if(status.equals("disconnected")) {
                ts3Data.clear();
                commandsToSend.clear();
                fireDisconnectEvents(R.string.no_server);
            }
            return;
        }

        if(state == State.INITIALIZING) {
            if(first.startsWith("notify")) {
                return;  // Ignore, we cannot handle those yet
            } else if(sentCmd.startsWith("servervariable")) {
                final String name = getParamValue(groups[0], "virtualserver_name");
                if(name.length() > 0) {
                    ts3EventSender.fireTS3Event(new TS3UpdateServerNameEvent(this, unescapeString(name)));
                }
            } else if(sentCmd.equals("whoami")) {
                final int clientID = getParamValueAsInt(groups[0], "clid");
                final int channelID = getParamValueAsInt(groups[0], "cid");
                if(clientID > 0 && channelID > 0) {
                    ts3Data.setClientID(clientID);
                    ts3Data.setCurrentChannelID(channelID);
                    commandsToSend.add("channellist -flags");
                }
            } else if(sentCmd.startsWith("channellist")) {
                ts3Data.clear();
                for(String group : groups) {
                    final int cid = getParamValueAsInt(group, "cid");
                    final String name = getParamValue(group, "channel_name");
                    final int pid = getParamValueAsInt(group, "pid");
                    final int cOrder = getParamValueAsInt(group, "channel_order");
                    final boolean subscribed = getParamValueAsInt(group, "channel_flag_are_subscribed") == 1;
                    final boolean hasPassword = getParamValueAsInt(group, "channel_flag_password") == 1;
                    final boolean isDefault = getParamValueAsInt(group, "channel_flag_default") == 1;
                    ts3Data.addChannel(cid, unescapeString(name), pid, cOrder, subscribed, hasPassword, isDefault);
                }
                commandsToSend.add("clientlist -away -voice -country");
            } else if(sentCmd.startsWith("clientlist")) {
                for(String group : groups) {
                    final int clid = getParamValueAsInt(group, "clid");
                    if(clid == 0)
                        continue;
                    final int type = getParamValueAsInt(group, "client_type");
                    if(type == 1)
                        continue;  // Ignore serverquery clients
                    final int cid = getParamValueAsInt(group, "cid");
                    final String name = getParamValue(group, "client_nickname");
                    final String country = getParamValue(group, "client_country");
                    final VoiceAwayResult result = parseVoiceAway(group);
                    ts3Data.addClient(clid, unescapeString(name), cid, result.talking == 1, result.inputMuted == 1, result.hardwareMuted == 0, result.outputMuted == 1, result.hardwareOutputMuted == 0, result.away == 1, result.talkPower, result.isTalker == 1, country);
                }
                // End of automatic commands on login
                if(!commandsToSend.isEmpty()) {
                    Log.e(TAG, "clientlist arrived, commands not empty");
                }
                sentCmd = "";
                state = State.IDLE;
                wantAutoreconnect = true;
                autoreconnectCounter = 0;
                Log.d(TAG, "State changed to IDLE");
            }
        } else if(state == State.IDLE) {

            final int scHandlerID = getParamValueAsInt(groups[0], "schandlerid");
            // Should no longer be necessary as we unregister notifications on tab switch. Let's see
            if(scHandlerID > 0 && scHandlerID != ts3Data.getCurrentScHandlerID()) {
                Log.e(TAG, "Wrong scHandlerID: " + scHandlerID + " " +  ts3Data.getCurrentScHandlerID());
                return;  // Ignore events on other tabs
            }

            if(first.equals("notifytalkstatuschange")) {
                final int clid = getParamValueAsInt(groups[0], "clid");
                final int status = getParamValueAsInt(groups[0], "status");
                //System.out.format("notifytalkstatuschange: %d %d\n", clid, status);
                if(status != 2) {  // Ignore STATUS_TALKING_WHILE_DISABLED
                    final Client client = ts3Data.getClient(clid);
                    if(client != null && !client.isInputMuted() && !client.isHardwareMuted()) {
                        client.setTalking(status == 1);
                        ts3EventSender.fireTS3Event(new TS3ClientTalkingEvent(this, client.getID(), client.isTalking()));
                    }
                }
            } else if(first.equals("notifycliententerview")) {
                int ctid = getParamValueAsInt(groups[0], "ctid");  // ctid only in first group
                for(String group : groups) {
                    final int type = getParamValueAsInt(group, "client_type");
                    if(type == 1)
                        continue;  // Ignore serverquery clients
                    final int clid = getParamValueAsInt(group, "clid");
                    final String name = getParamValue(group, "client_nickname");
                    final VoiceAwayResult result = parseVoiceAway(group);
                    final String country = getParamValue(group, "client_country");
                    ts3Data.addClient(clid, unescapeString(name), ctid, result.talking == 1, result.inputMuted == 1, result.hardwareMuted == 0, result.outputMuted == 1, result.hardwareOutputMuted == 0, result.away == 1, result.talkPower, result.isTalker == 1, country);
                }
            } else if(first.equals("notifyclientleftview")) {
                for(String group : groups) {
                    final int clid = getParamValueAsInt(group, "clid");
                    ts3Data.removeClient(clid);
                }
            } else if(first.equals("notifyclientmoved")) {
                final int clid = getParamValueAsInt(groups[0], "clid");
                final int ctid = getParamValueAsInt(groups[0], "ctid");
                final Client client = ts3Data.getClient(clid);
                if(client != null) {
                    final int oldChannelID = client.getChannelID();
                    client.setChannelID(ctid);
                    ts3EventSender.fireTS3Event(new TS3ClientMoveEvent(this, ImmutableClient.create(client), oldChannelID));
                }
            } else if(first.equals("notifyclientupdated")) {
                final String name = getParamValue(groups[0], "client_nickname");  // Check for nickname change
                final String country = getParamValue(groups[0], "client_country");  // Check for country change
                final VoiceAwayResult result = parseVoiceAway(groups[0]);  // Check for voice and away status change
                if(name.length() > 0 || result.talking != -1 || result.inputMuted != -1 || result.hardwareMuted != -1 || result.outputMuted != -1 || result.hardwareOutputMuted != -1 || result.away != -1 || result.talkPower != -1 || result.isTalker != -1) {
                    final int clid = getParamValueAsInt(groups[0], "clid");
                    final Client client = ts3Data.getClient(clid);
                    if(client != null) {
                        if(name.length() > 0)
                            client.setName(unescapeString(name));
                        if(country.length() > 0)
                            client.setCountry(country);
                        if(result.talking != -1)
                            client.setTalking(result.talking == 1);
                        if(result.inputMuted != -1)
                            client.setInputMuted(result.inputMuted == 1);
                        if(result.hardwareMuted != -1)
                            client.setHardwareMuted(result.hardwareMuted == 0);
                        if(result.outputMuted != -1)
                            client.setOutputMuted(result.outputMuted == 1);
                        if(result.hardwareOutputMuted != -1)
                            client.setHardwareOutputMuted(result.hardwareOutputMuted == 0);
                        if(result.away != -1)
                            client.setAway(result.away == 1);
                        if(result.talkPower != -1)
                            client.setTalkPower(result.talkPower);
                        if(result.isTalker != -1)
                            client.setIsTalker(result.isTalker == 1);
                        ts3EventSender.fireTS3Event(new TS3UpdateClientEvent(this, ImmutableClient.create(client)));
                    }
                }
            } else if(first.equals("notifychannelcreated")) {
                final int cid = getParamValueAsInt(groups[0], "cid");
                final String name = getParamValue(groups[0], "channel_name");
                final int cpid = getParamValueAsInt(groups[0], "cpid");
                final int cOrder = getParamValueAsInt(groups[0], "channel_order");
                final boolean subscribed = getParamValueAsInt(groups[0], "channel_flag_are_subscribed") == 1;
                final boolean hasPassword = getParamValueAsInt(groups[0], "channel_flag_password") == 1;
                final boolean isDefault = getParamValueAsInt(groups[0], "channel_flag_default") == 1;
                ts3Data.addChannel(cid, unescapeString(name), cpid, cOrder, subscribed, hasPassword, isDefault);
            } else if(first.equals("notifychanneledited")) {
                final int cid = getParamValueAsInt(groups[0], "cid");
                final Channel channel = ts3Data.getChannel(cid);
                if(channel != null) {
                    final String name = getParamValue(groups[0], "channel_name");
                    final String flagPassword = getParamValue(groups[0], "channel_flag_password");
                    final String flagDefault = getParamValue(groups[0], "channel_flag_default");
                    final String channelOrder = getParamValue(groups[0], "channel_order");
                    if(name.length() > 0 || flagPassword.length() > 0 || flagDefault.length() > 0 || channelOrder.length() > 0) {
                        if(name.length() > 0)
                            channel.setName(unescapeString(name));
                        if(flagPassword.length() > 0)
                            channel.setHasPassword(Integer.parseInt(flagPassword) == 1);
                        if(flagDefault.length() > 0)
                            channel.setIsDefault(Integer.parseInt(flagDefault) == 1);
                        if(channelOrder.length() > 0)
                            channel.setSortOrder(Integer.parseInt(channelOrder));
                        ts3EventSender.fireTS3Event(new TS3UpdateChannelEvent(this, ImmutableChannel.create(channel), channelOrder.length() > 0));
                    }
                }
            } else if(first.equals("notifychanneldeleted")) {
                for(String group : groups) {
                    final int cid = getParamValueAsInt(group, "cid");
                    ts3Data.removeChannel(cid);
                }
            } else if(first.equals("notifychannelmoved")) {
                for(String group : groups) {
                    final int cid = getParamValueAsInt(group, "cid");
                    final int cpid = getParamValueAsInt(group, "cpid");
                    final int order = getParamValueAsInt(group, "order");
                    ts3Data.moveChannel(cid, cpid, order);
                }
            } else if(first.equals("notifychannelsubscribed")) {
                for(String group : groups) {
                    final int cid = getParamValueAsInt(group, "cid");
                    final Channel channel = ts3Data.getChannel(cid);
                    if(channel != null) {
                        channel.setSubscribed(true);
                        Log.d(TAG, "Subscribed, send event: " + cid);
                        ts3EventSender.fireTS3Event(new TS3SubscribeChannelEvent(this, ImmutableChannel.create(channel)));
                    }
                }
            } else if(first.equals("notifychannelunsubscribed")) {
                for(String group : groups) {
                    final int cid = getParamValueAsInt(group, "cid");
                    final Channel channel = ts3Data.getChannel(cid);
                    if(channel != null) {
                        channel.setSubscribed(false);
                        Log.d(TAG, "Unsubscribed, send event: " + cid);
                        ts3EventSender.fireTS3Event(new TS3SubscribeChannelEvent(this, ImmutableChannel.create(channel)));
                    }
                }
            }

            // Explicitly sent, as we get no error on notify messages
            if(!commandsToSend.isEmpty()) {
                send(commandsToSend.poll());
            }
        }
    }

    private void receiveLoop() {
        state = State.HANDSHAKING;
        while(true) {
            final String line;
            try {
                line = in.readLine();
            } catch(IOException e) {
                Log.d(TAG, "Failed to read from clientquery: " + e.getMessage());
                break;
            }
            if(line == null) {
                Log.d(TAG, "Line is null, breaking");
                break;
            }
            if(line.length() > 0) {
                receiveLine(line);
            }
        }
    }

    private String getString(int id) {
        return context.getResources().getString(id);
    }

    private void fireDisconnectEvents(int message) {
        ts3EventSender.fireTS3Event(new TS3StatusTextEvent(this, getString(message)));
        ts3EventSender.fireTS3Event(new TS3ClearEvent(this));
        ts3EventSender.fireTS3Event(new TS3UpdateServerNameEvent(this, ""));
    }

    @Override
    public void run() {
        Log.d(TAG, "**** New Thread ****");
        while(true) {
            Log.d(TAG, "Thread wants to connect, host=" + host + ", port=" + port + ", authKey=" + authKey);
            if(!connect(host, port)) {
                fireDisconnectEvents(R.string.no_connection);
                if(!wantAutoreconnect || autoreconnectCounter > 10) {
                    if(!disconnectRequested) {
                        // Show Toast notification if we failed to connect, but not when requesting a disconnect and socket.connect was terminated
                        ts3EventSender.fireTS3Event(new TS3ToastEvent(this, String.format(getString(R.string.connection_failed), host)));
                    }
                    break;  // No autoreconnect
                } else {
                    autoreconnectCounter++;
                    continue;  // Autoreconnect
                }
            }
            receiveLoop();
            Log.d(TAG, "**** receiveLoop exited ****");
            ts3Data.clear();
            commandsToSend.clear();
            closeConnection();
            if(!wantAutoreconnect && autoreconnectCounter < 10) {
                break;  // No autoreconnect
            }
            // Autoreconnect
            autoreconnectCounter++;
        }
        fireDisconnectEvents(R.string.no_connection);
        Log.d(TAG, "**** Thread done ****");
    }

    private class AntiIdleTimerTask extends TimerTask {
        @Override
        public void run() {
            if(state == State.IDLE && commandsToSend.isEmpty()) {
                send("ayt");
            }
        }
    }
}
