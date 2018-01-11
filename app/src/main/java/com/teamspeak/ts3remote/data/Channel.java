package com.teamspeak.ts3remote.data;

/**
 * Channel is a mutable datastructure to store a teamspeak 3 channel.
 * This class can't be extended.
 */
public final class Channel {

    /**
     * constant channel id
     */
    private final int id;

    /**
     * name of a channel
     */
    private String name;

    /**
     * channel id of the parent channel (in the server tree)
     */
    private int channelParentID;

    /**
     *
     */
    private int sortOrder;

    /**
     * isChannelSubscribed
     */
    private boolean subscribed;

    /**
     * hasChannelPassword
     */
    private boolean hasPassword;

    /**
     * IsDefaultChannel
     */
    private boolean isDefault;

    public Channel(final int channelID, final String name, final int channelParentID, final int sortOrder, final boolean subscribed, final boolean hasPassword, final boolean isDefault) {
        this.id = channelID;
        this.name = name;
        this.channelParentID = channelParentID;
        this.sortOrder = sortOrder;
        this.subscribed = subscribed;
        this.hasPassword = hasPassword;
        this.isDefault = isDefault;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getChannelParentID() {
        return channelParentID;
    }

    public void setChannelParentID(final int channelParentID) {
        this.channelParentID = channelParentID;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(final int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(final boolean subscribed) {
        this.subscribed = subscribed;
    }

    public boolean hasPassword() {
        return hasPassword;
    }

    public void setHasPassword(final boolean hasPassword) {
        this.hasPassword = hasPassword;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setIsDefault(final boolean isDefault) {
        this.isDefault = isDefault;
    }
}
