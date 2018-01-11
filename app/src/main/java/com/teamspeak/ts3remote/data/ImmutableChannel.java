package com.teamspeak.ts3remote.data;

/**
 * ImmutableChannel structure.
 * Can be created from a mutable channel.
 */
public final class ImmutableChannel {
    private final int id;
    private final String name;
    private final int channelParentID;
    private final int sortOrder;
    private final boolean subscribed;
    private final boolean hasPassword;
    private final boolean isDefault;

    private ImmutableChannel(final int channelID, final String name, final int channelParentID, final int sortOrder, final boolean subscribed, final boolean hasPassword, final boolean isDefault) {
        this.id = channelID;
        this.name = name;
        this.channelParentID = channelParentID;
        this.sortOrder = sortOrder;
        this.subscribed = subscribed;
        this.hasPassword = hasPassword;
        this.isDefault = isDefault;
    }

    public static final ImmutableChannel create(final Channel channel) {
        return new ImmutableChannel(channel.getID(), channel.getName(), channel.getChannelParentID(), channel.getSortOrder(), channel.isSubscribed(), channel.hasPassword(), channel.isDefault());
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getChannelParentID() {
        return channelParentID;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public boolean hasPassword() {
        return hasPassword;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
