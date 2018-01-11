package com.teamspeak.ts3remote;

import java.util.List;
import java.util.ArrayList;
import android.util.Log;
import com.teamspeak.ts3remote.data.Common;

class ListItem implements Common {
    enum Type {
        CLIENT, CHANNEL
    };

    private final Type type;
    private ListItem parent;
    private final int id;
    private String name;

    ListItem(final Type type, final ListItem parent, final int id, final String name) {
        this.type = type;
        this.parent = parent;
        this.id = id;
        this.name = name;
    }

    Type getType() {
        return type;
    }

    ListItem getParent() {
        return parent;
    }

    void setParent(final ListItem parent) {
        this.parent = parent;
    }

    int getID() {
        return id;
    }

    String getName() {
        return name;
    }

    void setName(final String name) {
        this.name = name;
    }
}

final class ChannelItem extends ListItem {
    private final List<ChannelItem> subChannels = new ArrayList<ChannelItem>();
    private final List<ChannelItem> addLater = new ArrayList<ChannelItem>();
    private final List<ClientItem> clients = new ArrayList<ClientItem>();
    private boolean expanded;
    private int sortOrder;
    private boolean subscribed;
    private boolean hasPassword;
    private boolean isDefault;

    ChannelItem(final int channelID, final String name, final ChannelItem parent, final int sortOrder, final boolean subscribed, final boolean hasPassword, final boolean isDefault) {
        super(Type.CHANNEL, parent, channelID, name);
        expanded = false;
        this.sortOrder = sortOrder;
        this.subscribed = subscribed;
        this.hasPassword = hasPassword;
        this.isDefault = isDefault;
        if(parent != null) {
            parent.addSubChannel(this);
        }
        // Log.d(TAG, "ChannelItem: " + channelID + " " + name + " " + parent);
    }

    int getChildCount() {
        return clients.size();
    }

    ClientItem getClient(final int pos) {
        return clients.get(pos);
    }

    void addClient(final ClientItem client) {
        client.setParent(this);
        final int pos = getClientInsertionPos(client.getName(), client.getTalkPower(), client.isTalker(), 0, getChildCount());
        clients.add(pos, client);
    }

    void removeClient(final ClientItem client) {
        client.setParent(null);
        clients.remove(client);
    }

    List<ChannelItem> getSubChannels() {
        return subChannels;
    }

    void addSubChannel(final ChannelItem subChannel) {
        Log.d(TAG, "addSubChannel, sortOrder = " + subChannel.getSortOrder());

        if(subChannel.getSortOrder() == 0) {
            Log.d(TAG, "Adding on top, cOrder = 0");
            subChannels.add(0, subChannel);
        } else {
            ChannelItem sortAfter = null;
            for(ChannelItem c : subChannels) {
                if(c.getID() == subChannel.getSortOrder()) {
                    sortAfter = c;
                    break;
                }
            }
            if(sortAfter == null) {
                Log.d(TAG, "No sortAfter, add later");
                addLater.add(subChannel);
                return;
            }
            final int pos = subChannels.indexOf(sortAfter);
            Log.d(TAG, "Found sortAfter: " + sortAfter.getID() + " " + sortAfter.getName() + " -> Channel insertion pos: " + pos);
            subChannels.add(pos + 1, subChannel);
        }

        // Check addLater list if this channel might have the sortOrder previously postponed
        ChannelItem toAdd = null;
        for(ChannelItem c : addLater) {
            if(c.getSortOrder() == subChannel.getID()) {
                Log.d(TAG, "Found addLater: " + c.getID() + " " + c.getName());
                toAdd = c;
                addLater.remove(c);
                break;
            }
        }
        if(toAdd != null) {
            addSubChannel(toAdd);
        }
    }

    void removeSubChannel(final ChannelItem subChannel) {
        subChannels.remove(subChannel);
    }

    boolean isExpanded() {
        return expanded;
    }

    void setExpanded(final boolean expanded) {
        Log.d(TAG, "SET EXPANDED: " + getName() + " " + expanded);
        this.expanded = expanded;
    }

    int getSortOrder() {
        return sortOrder;
    }

    void setSortOrder(final int sortOrder) {
        this.sortOrder = sortOrder;
    }

    boolean isSubscribed() {
        return subscribed;
    }

    void setSubscribed(final boolean subscribed) {
        this.subscribed = subscribed;
    }

    boolean hasPassword() {
        return hasPassword;
    }

    void setHasPassword(final boolean hasPassword) {
        this.hasPassword = hasPassword;
    }

    boolean isDefault() {
        return isDefault;
    }

    void setIsDefault(final boolean isDefault) {
        this.isDefault = isDefault;
    }

    private int getClientInsertionPos(final String name, final int talkPower, final boolean isTalker, final int from, final int to) {
        if(from == to) {
            return from;
        }
        final int middle = (from + to) / 2;
        final ClientItem middleItem = clients.get(middle);
        if(middleItem.getTalkPower() > talkPower) { // our talk power is smaller than the middle
            // is_in_bottom_half;
            return getClientInsertionPos(name, talkPower, isTalker, middle + 1, to); // position must be in bottom half (middle + 1, to)
        }
        if(middleItem.getTalkPower() < talkPower) { // our talk power is bigger than the middle
            // is_in_top_half
            return getClientInsertionPos(name, talkPower, isTalker, from, middle); // position must be in top half (from, middle)
        }
        // our talk power is exactly the middle, check isTalker
        if(middleItem.isTalker() != isTalker) {
            if(isTalker) { // we are talker, middle item is not
                // is_in_top_half
                return getClientInsertionPos(name, talkPower, isTalker, from, middle); // position must be in top half (from, middle)
            } else { // we are not talker, middle item is
                // is_in_bottom_half
                return getClientInsertionPos(name, talkPower, isTalker, middle + 1, to); // position must be in bottom half (middle + 1, to)
            }
        }
        // isTalker is also the same, gogo alphabet
        if(middleItem.getName().compareToIgnoreCase(name) < 0 || (middleItem.getName().compareToIgnoreCase(name) == 0 && middleItem.getName().compareTo(name) < 0)) {
            // is_in_bottom_half;
            return getClientInsertionPos(name, talkPower, isTalker, middle + 1, to); // position must be in bottom half (middle + 1, to)
        }
        return getClientInsertionPos(name, talkPower, isTalker, from, middle); // position must be in top half (from, middle)
    }
}

final class ClientItem extends ListItem {
    private boolean talking;
    private boolean inputMuted;
    private boolean outputMuted;
    private boolean away;
    private int talkPower;
    private boolean isTalker;
    private String country;

    ClientItem(final int clientID, final String name) {
        super(Type.CLIENT, null, clientID, name);
        this.talking = false;
        this.inputMuted = false;
        this.outputMuted = false;
        this.away = false;
        this.talkPower = 0;
        this.isTalker = false;
        this.country = "";
    }

    ClientItem(final int clientID, final String name, final boolean talking, final boolean inputMuted, final boolean outputMuted, final boolean away, final int talkPower, final boolean isTalker, final String country) {
        super(Type.CLIENT, null, clientID, name);
        this.talking = talking;
        this.inputMuted = inputMuted;
        this.outputMuted = outputMuted;
        this.away = away;
        this.talkPower = talkPower;
        this.isTalker = isTalker;
        this.country = country;
    }

    boolean isTalking() {
        return talking;
    }

    void setTalking(final boolean talking) {
        this.talking = talking;
    }

    boolean isInputMuted() {
        return inputMuted;
    }

    void setInputMuted(final boolean inputMuted) {
        this.inputMuted = inputMuted;
    }

    boolean isOutputMuted() {
        return outputMuted;
    }

    void setOutputMuted(final boolean outputMuted) {
        this.outputMuted = outputMuted;
    }

    boolean isAway() {
        return away;
    }

    void setAway(final boolean away) {
        this.away = away;
    }

    int getTalkPower() {
        return talkPower;
    }

    void setTalkPower(final int talkPower) {
        this.talkPower = talkPower;
    }

    boolean isTalker() {
        return isTalker;
    }

    void setIsTalker(final boolean isTalker) {
        this.isTalker = isTalker;
    }

    String getCountry() {
        return country;
    }

    void setCountry(final String country) {
        this.country = country;
    }
}
