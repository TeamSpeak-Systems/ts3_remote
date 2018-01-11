package com.teamspeak.ts3remote;

import com.teamspeak.ts3remote.data.ImmutableChannel;
import com.teamspeak.ts3remote.data.ImmutableClient;

interface TS3UIListener {
    void addClient(final ImmutableClient client);
    void removeClient(final int clientID);
    void addChannel(final ImmutableChannel channel);
    void removeChannel(final int channelID);
    void moveChannel(final ImmutableChannel channel);
    void updateClientTalking(final int clientID, final boolean talking);
    void updateClient(final ImmutableClient client);
    void moveClient(final ImmutableClient client, final int oldChannelID);
    void updateChannel(final ImmutableChannel channel, final boolean sortOrderChanged);
    void subscribeChannel(final ImmutableChannel channel);
    void clear();
    void updateServerName(final String serverName);
    void toastInfo(final String toastText);
    void statusTextInfo(final String statusText);
}
