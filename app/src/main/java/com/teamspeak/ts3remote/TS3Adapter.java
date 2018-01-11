package com.teamspeak.ts3remote;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import com.teamspeak.ts3remote.data.Common;

final class TS3Adapter extends BaseExpandableListAdapter implements Common, ExpandableListView.OnGroupCollapseListener, ExpandableListView.OnGroupExpandListener {
    private final List<ChannelItem> channelList = new ArrayList<ChannelItem>();
    private final List<ChannelItem> addLater = new ArrayList<ChannelItem>();
    private final SparseArray<ChannelItem> channelMap = new SparseArray<ChannelItem>();
    private final SparseArray<ClientItem> clientMap = new SparseArray<ClientItem>();
    private final TS3Activity activity;
    private final Map<String, Bitmap> countries = new HashMap<String, Bitmap>();

    TS3Adapter(final TS3Activity activity) {
        this.activity = activity;
    }

    void clear() {
        channelList.clear();
        channelMap.clear();
        clientMap.clear();
        notifyDataSetChanged();
    }

    ChannelItem getChannel(final int channelID) {
        return channelMap.get(channelID);
    }

    void addChannel(final ChannelItem channel) {
        channelMap.put(channel.getID(), channel);

        // Only add if this channel is top-level or its parent is expanded
        if(channel.getParent() == null || ((ChannelItem)channel.getParent()).isExpanded()) {
            displayChannel(channel);
        }
    }

    private void displayChannel(final ChannelItem channel) {
        Log.d(TAG, "Want to display channel, sortOrder = " + channel.getSortOrder());

        if(channel.getSortOrder() == 0) {
            if(channel.getParent() == null) {
                Log.d(TAG, "Adding on top, cOrder = 0");
                channelList.add(0, channel);
            } else {
                Log.d(TAG, "Adding on top as subchannel, cOrder = 0");
                final int pos = channelList.indexOf(channel.getParent());
                if(pos != -1) {
                    channelList.add(pos + 1, channel);
                }
            }
        } else {
            ChannelItem sortAfter = null;
            for(ChannelItem c : channelList) { // Take from visible clients, must not use getChannel here
                if(c.getID() == channel.getSortOrder()) {
                    sortAfter = c;
                    break;
                }
            }
            if(sortAfter == null) {
                Log.d(TAG, "No sortAfter, add later");
                addLater.add(channel);
                return;
            }
            final int pos = channelList.indexOf(sortAfter);
            Log.d(TAG, "Found sortAfter: " + sortAfter.getID() + " " + sortAfter.getName() + " -> Channel insertion pos: " + pos);
            if(pos != -1) {
                channelList.add(pos + 1, channel);
            }
        }

        // Check addLater list if this channel might have the sortOrder previously postponed
        ChannelItem toAdd = null;
        for(ChannelItem c : addLater) {
            if(c.getSortOrder() == channel.getID()) {
                Log.d(TAG, "Found addLater: " + c.getID() + " " + c.getName());
                toAdd = c;
                addLater.remove(c);
                break;
            }
        }
        if(toAdd != null) {
            addChannel(toAdd);
        }
    }

    void removeChannel(final int channelID) {
        final ChannelItem channel = getChannel(channelID);
        final ChannelItem parentChannel = (ChannelItem)channel.getParent();
        if(parentChannel != null)
            parentChannel.removeSubChannel(channel);
        channelList.remove(channel);
        channelMap.remove(channel.getID());
    }

    ClientItem getClient(final int clientID) {
        return clientMap.get(clientID);
    }

    void addClient(final ClientItem client, final int channelID) {
        final ChannelItem channel = getChannel(channelID);
        channel.addClient(client);
        clientMap.put(client.getID(), client);
        if(client.getID() == activity.getOwnClientID()) {
            expandChannelFamily(channel);
        }
    }

    void removeClient(final int clientID) {
        final ClientItem client = getClient(clientID);
        final ChannelItem channel = (ChannelItem)client.getParent();
        channel.removeClient(client);
        clientMap.remove(clientID);
    }

    void moveClient(final int clientID, final int newChannelID, final int oldChannelID) {
        final ClientItem client = clientMap.get(clientID);
        final ChannelItem newChannel = channelMap.get(newChannelID);
        final ChannelItem oldChannel = channelMap.get(oldChannelID);
        oldChannel.removeClient(client);
        newChannel.addClient(client);
        if(client.getID() == activity.getOwnClientID()) {
            expandChannelFamily(newChannel);
        }
    }

    private void expandChannelFamily(final ChannelItem channel) {
        ChannelItem top = channel;
        while(top.getParent() != null) {
            top.setExpanded(true);
            top = (ChannelItem)top.getParent();
        }
        top.setExpanded(true);
        recursiveExpandChannel(top);
        notifyDataSetChanged();
        expandCollapseAll();
    }

    @Override
    public int getGroupCount() {
        return channelList.size();
    }

    @Override
    public long getGroupId(final int groupPosition) {
        final ListItem item = (ListItem)getGroup(groupPosition);
        return item.getID();
    }

    @Override
    public Object getGroup(final int groupPosition) {
        return channelList.get(groupPosition);
    }

    int getGroupPosition(final ChannelItem item) {
        return channelList.indexOf(item);
    }

    @Override
    public int getChildrenCount(final int groupPosition) {
        return channelList.get(groupPosition).getChildCount();
    }

    @Override
    public long getChildId(final int groupPosition, final int childPosition) {
        final ListItem item = (ListItem)getChild(groupPosition, childPosition);
        return item.getID();
    }

    @Override
    public Object getChild(final int groupPosition, final int childPosition) {
        return channelList.get(groupPosition).getClient(childPosition);
    }

    private int getItemDepth(ListItem item) {
        int d = -1;
        do {
            item = item.getParent();
            d++;
        } while(item != null);
        return d;
    }

    private int getChannelIconResID(final ChannelItem item) {
        final boolean isSubscribed = item.isSubscribed() || item.getID() == activity.getCurrentChannelID();
        if(item.hasPassword()) {
            return isSubscribed ? R.drawable.channel_yellow_subscribed : R.drawable.channel_yellow;
        }
        return isSubscribed ? R.drawable.channel_green_subscribed : R.drawable.channel_green;
    }

    private int getClientIconResID(final ClientItem item) {
        if(item.isAway()) {
            return R.drawable.player_away;
        } else if(item.isOutputMuted()) {
            return R.drawable.player_output_muted;
        } else if(item.isInputMuted()) {
            return R.drawable.player_input_muted;
        } else if(item.isTalking()) {
            return R.drawable.player_on;
        }
        return R.drawable.player_off;
    }

    private Bitmap getCountryBitmap(final String country) {
        if(!activity.getEnableCountryFlags() || country.length() == 0) {
            return null;
        }

        Bitmap bitmap = countries.get(country); // Already in cache?
        if(bitmap != null) {
            return bitmap;
        }

        // Not in cache, load from assets directory
        final InputStream is;
        try {
            is = activity.getAssets().open("countries/" + country.toLowerCase() + ".png");
        } catch(IOException e) {
            Log.e(TAG, "Failed to read country icon for: " + country + " = " + e.getMessage());
            return null;
        }
        bitmap = BitmapFactory.decodeStream(is);
        countries.put(country, bitmap); // Store in cache by country string
        return bitmap;
    }

    @Override
    public View getGroupView(final int groupPosition, final boolean isExpanded, final View convertView, final ViewGroup parent) {
        final ChannelItem item = (ChannelItem)getGroup(groupPosition);
        final LayoutInflater inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View v = inflater.inflate(R.layout.listitemchannel, null);
        final ImageView iconView = (ImageView)v.findViewById(R.id.listViewIcon);
        final ImageView channelIconView = (ImageView)v.findViewById(R.id.listViewChannelIcon);
        final TextView textView = (TextView)v.findViewById(R.id.listViewText);
        v.setPadding(64 + getItemDepth(item) * 32, 0, 0, 0);
        iconView.setImageResource(getChannelIconResID(item));
        textView.setText(item.getName());
        if(item.isDefault())
            channelIconView.setImageResource(R.drawable.channel_default);
        else if(item.hasPassword())
            channelIconView.setImageResource(R.drawable.channel_password);
        return v;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild, final View convertView, final ViewGroup parent) {
        final ClientItem item = (ClientItem)getChild(groupPosition, childPosition);
        final LayoutInflater inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View v = inflater.inflate(R.layout.listitemclient, null);
        final ImageView iconView = (ImageView)v.findViewById(R.id.listViewIcon);
        final ImageView countryIconView = (ImageView)v.findViewById(R.id.listViewIconCountry);
        final TextView textView = (TextView)v.findViewById(R.id.listViewText);
        v.setPadding(64 + getItemDepth(item) * 32, 0, 0, 0);
        iconView.setImageResource(getClientIconResID(item));
        textView.setText(item.getName());
        countryIconView.setImageBitmap(getCountryBitmap(item.getCountry()));
        return v;
    }

    @Override
    public boolean isChildSelectable(final int groupPosition, final int childPosition) {
        return false;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void expandCollapseAll() {
        for(int i = 0; i < channelList.size(); ++i) {
            if(channelList.get(i).isExpanded() && !activity.getListView().isGroupExpanded(i)) {
                activity.getListView().expandGroup(i);
            } else if(!channelList.get(i).isExpanded() && activity.getListView().isGroupExpanded(i)) {
                activity.getListView().collapseGroup(i);
            }
        }
    }

    private void recursiveCollapseChannel(final ChannelItem channel) {
        final List<ChannelItem> subChannels = channel.getSubChannels();
        for(final ChannelItem subChannel : subChannels) {
            channelList.remove(subChannel);
            Log.d(TAG, "Remove on collapse: " + subChannel.getName());
            recursiveCollapseChannel(subChannel);
        }
    }

    private void recursiveExpandChannel(final ChannelItem channel) {
        final List<ChannelItem> subChannels = channel.getSubChannels();
        for(int i = subChannels.size() - 1; i >= 0; --i) {
            final ChannelItem subChannel = subChannels.get(i);
            if(!channelList.contains(subChannel)) {  // Ensure channel isn't added twice
                channelList.add(channelList.indexOf(channel) + 1, subChannel);
            }
            Log.d(TAG, "Add on Expand: " + subChannel.getName());
            if(subChannel.isExpanded()) {
                recursiveExpandChannel(subChannel);
            }
        }
    }

    @Override
    public void onGroupCollapse(final int groupPosition) {
        final ChannelItem channel = channelList.get(groupPosition);
        if(!channel.isExpanded())
            return;
        channel.setExpanded(false);
        recursiveCollapseChannel(channel);
        notifyDataSetChanged();
        expandCollapseAll();
    }

    @Override
    public void onGroupExpand(final int groupPosition) {
        final ChannelItem channel = channelList.get(groupPosition);
        if(channel.isExpanded())
            return;
        channel.setExpanded(true);
        recursiveExpandChannel(channel);
        notifyDataSetChanged();
        expandCollapseAll();
    }
}
