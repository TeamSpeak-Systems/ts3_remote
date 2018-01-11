package com.teamspeak.ts3remote;

import com.teamspeak.ts3remote.data.ImmutableChannel;
import com.teamspeak.ts3remote.data.ImmutableClient;
import java.util.EventObject;

class TS3Event extends EventObject {
    TS3Event(Object obj) {
        super(obj);
    }
}

final class TS3AddClientEvent extends TS3Event {
    final protected ImmutableClient client;

    TS3AddClientEvent(final Object obj, final ImmutableClient client) {
        super(obj);
        this.client = client;
    }

    ImmutableClient getClient() {
        return client;
    }
}

final class TS3RemoveClientEvent extends TS3Event {
    final private int clientID;

    TS3RemoveClientEvent(final Object obj, final int clientID) {
        super(obj);
        this.clientID = clientID;
    }

    int getClientID() {
        return clientID;
    }
}

final class TS3AddChannelEvent extends TS3Event {
    final protected ImmutableChannel channel;

    TS3AddChannelEvent(final Object obj, final ImmutableChannel channel) {
        super(obj);
        this.channel = channel;
    }

    ImmutableChannel getChannel() {
        return channel;
    }
}

final class TS3RemoveChannelEvent extends TS3Event {
    final private int channelID;

    TS3RemoveChannelEvent(final Object obj, final int channelID) {
        super(obj);
        this.channelID = channelID;
    }

    int getChannelID() {
        return channelID;
    }
}

final class TS3MoveChannelEvent extends TS3Event {
    final protected ImmutableChannel channel;

    TS3MoveChannelEvent(final Object obj, final ImmutableChannel channel) {
        super(obj);
        this.channel = channel;
    }

    ImmutableChannel getChannel() {
        return channel;
    }
}

final class TS3ClientTalkingEvent extends TS3Event {
    final private int clientID;
    final private boolean talking;

    TS3ClientTalkingEvent(final Object obj, final int clientID, final boolean talking) {
        super(obj);
        this.clientID = clientID;
        this.talking = talking;
    }

    int getClientID() {
        return clientID;
    }

    boolean isTalking() {
        return talking;
    }
}

final class TS3UpdateClientEvent extends TS3Event {
    final private ImmutableClient client;

    TS3UpdateClientEvent(final Object obj, final ImmutableClient client) {
        super(obj);
        this.client = client;
    }

    ImmutableClient getClient() {
        return client;
    }
}

final class TS3ClientMoveEvent extends TS3Event {
    final private ImmutableClient client;
    final private int oldChannelID;

    TS3ClientMoveEvent(final Object obj, final ImmutableClient client, final int oldChannelID) {
        super(obj);
        this.client = client;
        this.oldChannelID = oldChannelID;
    }

    ImmutableClient getClient() {
        return client;
    }

    int getOldChannelID() {
        return oldChannelID;
    }
}

final class TS3UpdateChannelEvent extends TS3Event {
    final private ImmutableChannel channel;
    final private boolean sortOrderChanged;

    TS3UpdateChannelEvent(final Object obj, final ImmutableChannel channel, final boolean sortOrderChanged) {
        super(obj);
        this.channel = channel;
        this.sortOrderChanged = sortOrderChanged;
    }

    ImmutableChannel getChannel() {
        return channel;
    }

    boolean getSortOrderChanged() {
        return sortOrderChanged;
    }
}

final class TS3SubscribeChannelEvent extends TS3Event {
    final private ImmutableChannel channel;

    TS3SubscribeChannelEvent(final Object obj, final ImmutableChannel channel) {
        super(obj);
        this.channel = channel;
    }

    ImmutableChannel getChannel() {
        return channel;
    }
}

final class TS3ClearEvent extends TS3Event {
    TS3ClearEvent(final Object obj) {
        super(obj);
    }
}

final class TS3UpdateServerNameEvent extends TS3Event {
    final private String serverName;

    TS3UpdateServerNameEvent(final Object obj, final String serverName) {
        super(obj);
        this.serverName = serverName;
    }

    String getServerName() {
        return serverName;
    }
}

final class TS3ToastEvent extends TS3Event {
    final private String toastText;

    TS3ToastEvent(final Object obj, final String toastText) {
        super(obj);
        this.toastText = toastText;
    }

    String getInfoText() {
        return toastText;
    }
}

final class TS3StatusTextEvent extends TS3Event {
    final private String statusText;

    TS3StatusTextEvent(final Object obj, final String statusText) {
        super(obj);
        this.statusText = statusText;
    }

    String getStatusText() {
        return statusText;
    }
}
