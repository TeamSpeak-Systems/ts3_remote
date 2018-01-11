package com.teamspeak.ts3remote;

import android.util.Log;
import android.util.SparseArray;
import com.teamspeak.ts3remote.data.Channel;
import com.teamspeak.ts3remote.data.Client;
import com.teamspeak.ts3remote.data.Common;
import com.teamspeak.ts3remote.data.ImmutableChannel;
import com.teamspeak.ts3remote.data.ImmutableClient;

final class TS3Data implements Common {
    private int scHandlerID;
    private int clientID;
    private int currentChannelID;
    private final TS3EventSender ts3EventSender;
    private final SparseArray<Client> clients = new SparseArray<Client>();
    private final SparseArray<Channel> channels = new SparseArray<Channel>();

    TS3Data(TS3EventSender ts3EventSender) {
        this.ts3EventSender = ts3EventSender;
    }

    int getCurrentScHandlerID() {
        return scHandlerID;
    }

    void setCurrentScHandlerID(final int scHandlerID) {
        this.scHandlerID = scHandlerID;
    }

    synchronized int getClientID() {
        return clientID;
    }

    synchronized void setClientID(final int clientID) {
        this.clientID = clientID;
    }

    synchronized int getCurrentChannelID() {
        return currentChannelID;
    }

    synchronized void setCurrentChannelID(final int channelID) {
        this.currentChannelID = channelID;
    }

    Client getClient(final int clientID) {
        return clients.get(clientID);
    }

    void addClient(final int clientID, final String name, final int channelID, final boolean isTalking, final boolean inputMuted, final boolean hardwareMuted, final boolean outputMuted, final boolean hardwareOutputMuted, final boolean away, final int talkPower, final boolean isTalker, final String country) {
        if(getClient(clientID) != null) {
            Log.d(TAG, "Client " + name + " (" + clientID + "%) exists, ignoring");
            return;
        }

        final Client client = new Client(clientID, name, channelID, isTalking, inputMuted, hardwareMuted, outputMuted, hardwareOutputMuted, away, talkPower, isTalker, country);
        clients.put(clientID, client);
        Log.d(TAG, "ADD CLIENT: " + clientID + " " + name + " " + channelID + " " + inputMuted + " " + hardwareMuted + " " + outputMuted + " " + hardwareOutputMuted + " " + away);

        ts3EventSender.fireTS3Event(new TS3AddClientEvent(this, ImmutableClient.create(client)));
    }

    void removeClient(final int clientID) {
        clients.remove(clientID);
        ts3EventSender.fireTS3Event(new TS3RemoveClientEvent(this, clientID));
    }

    Channel getChannel(final int channelID) {
        return channels.get(channelID);
    }

    void addChannel(final int channelID, final String name, final int channelParentID, final int sortOrder, final boolean subscribed, final boolean hasPassword, final boolean isDefault) {
        if(getChannel(channelID) != null) {
            Log.d(TAG, "Channel " + name + " (" + channelID + "%) exists, ignoring");
            return;
        }

        final Channel channel = new Channel(channelID, name, channelParentID, sortOrder, subscribed, hasPassword, isDefault);
        channels.put(channelID, channel);
        Log.d(TAG, "ADD CHANNEL: " + channelID + " " + name + " " + channelParentID + " " + sortOrder + " " + subscribed + " " + hasPassword + " " + isDefault);

        ts3EventSender.fireTS3Event(new TS3AddChannelEvent(this, ImmutableChannel.create(channel)));
    }

    void removeChannel(final int channelID) {
        Log.d(TAG, "REMOVE CHANNEL: " + channelID);
        channels.remove(channelID);
        ts3EventSender.fireTS3Event(new TS3RemoveChannelEvent(this, channelID));
    }

    void moveChannel(final int channelID, final int channelParentID, final int sortOrder) {
        final Channel channel = getChannel(channelID);
        if(channel == null) {
            Log.d(TAG, "Failed to find channel in moveChannel: " + channelID);
            return;
        }

        channel.setChannelParentID(channelParentID);
        channel.setSortOrder(sortOrder);
        ts3EventSender.fireTS3Event(new TS3MoveChannelEvent(this, ImmutableChannel.create(channel)));
    }

    void clear() {
        clients.clear();
        channels.clear();
    }
}
