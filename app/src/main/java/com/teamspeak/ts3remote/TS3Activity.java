package com.teamspeak.ts3remote;

import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.preference.PreferenceManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import com.teamspeak.ts3remote.data.Common;
import com.teamspeak.ts3remote.data.ImmutableChannel;
import com.teamspeak.ts3remote.data.ImmutableClient;
import java.util.Timer;
import java.util.TimerTask;

public final class TS3Activity extends Activity implements Common, OnDismissListener, OnCancelListener, TS3Listener, TS3UIListener {
    private final static int DIALOG_CONNECT = 0;
    private boolean dialogCancelled = false;
    private ExpandableListView listView;
    private TS3Adapter adapter;
    private ClientQueryParser clientQueryParser;
    private boolean dirty = false;
    private final Timer viewUpdateTimer = new Timer();
    private final static int viewUpdateTimerDelay = 200;
    private boolean prefEnableCountryFlags;
    private Menu contextMenu = null;
    private long contextMenuID = 0;

    ExpandableListView getListView() {
        return listView;
    }

    boolean getEnableCountryFlags() {
        return prefEnableCountryFlags;
    }

    int getOwnClientID() {
        // TODO: Maybe better use own class variable here living in this thread to avoid calls to synchronized function
        return clientQueryParser.getTS3Data().getClientID();
    }

    int getCurrentChannelID() {
        // TODO: Maybe better use own class variable here living in this thread to avoid calls to synchronized function
        return clientQueryParser.getTS3Data().getCurrentChannelID();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        listView = (ExpandableListView)findViewById(R.id.listView);
        adapter = new TS3Adapter(this);
        listView.setAdapter(adapter);
        listView.setDivider(null);
        listView.setChildDivider(null);
        listView.setOnGroupExpandListener(adapter);
        listView.setOnGroupCollapseListener(adapter);
        listView.setOnCreateContextMenuListener(this);

        viewUpdateTimer.schedule(new ViewUpdateTimerTask(), viewUpdateTimerDelay, viewUpdateTimerDelay);

        // If an IP was stored, immediately connect to it.
        final SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        if(prefs.contains(ConnectDialog.PREFS_KEY) && prefs.contains(ConnectDialog.PREFS_AUTH_KEY)) {
            final String storedIP = prefs.getString(ConnectDialog.PREFS_KEY, "");
            final String authKey = prefs.getString(ConnectDialog.PREFS_AUTH_KEY, "");
            if(storedIP.length() > 0) {
                createConnection(storedIP, authKey);
            }
        }

        // No connection yet (first-time install)? Show connect dialog
        if(clientQueryParser == null) {
            showDialog(DIALOG_CONNECT);
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        loadPreferences();
    }

    private void loadPreferences() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefEnableCountryFlags = prefs.getBoolean("enableCountryFlags", true);
        dirty = true;
    }

    private void createConnection(final String host, final String authKey) {
        clientQueryParser = new ClientQueryParser(this);
        clientQueryParser.getTS3EventSender().addTS3Listener(this);
        clientQueryParser.setHost(host);
        clientQueryParser.setAuthKey(authKey);
        clientQueryParser.start();
    }

    private void destroyConnection() {
        try {
            clientQueryParser.disconnect();
            clientQueryParser.join();
        } catch(NullPointerException e) {
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        clientQueryParser = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.connectMenuItem:
                showDialog(DIALOG_CONNECT);
                return true;
            case R.id.settingsMenuItem:
                TS3PreferencesActivity.openPreferences(this);
                return true;
            case R.id.infoMenuItem:
                HelpViewActivity.openHelpView(this);
                return true;
            case R.id.exitMenuItem:
                destroyConnection();
                viewUpdateTimer.cancel();
                finish();
                return true;
            case R.id.subscribeAllMenuItem:
                clientQueryParser.subscribeAll();

                if(clientQueryParser == null) {
                    int duration = Toast.LENGTH_SHORT;
                    Context context = getApplicationContext();
                    CharSequence text = "Please connect first!";
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
                return true;

            case R.id.unsubscribeAllMenuItem:
                clientQueryParser.unsubscribeAll();
                if(clientQueryParser == null) {
                    int duration = Toast.LENGTH_SHORT;
                    Context context = getApplicationContext();
                    CharSequence text = "Please connect first!";
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DIALOG_CONNECT:
                Dialog dlg = new ConnectDialog(this, this, this);
                dlg.setOwnerActivity(this);
                return dlg;
        }
        return null;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if(!dialogCancelled) {
            ConnectDialog connectDialog = (ConnectDialog)dialog;
            if(connectDialog != null && connectDialog.getIP().length() > 0 && connectDialog.getAuthKey().length() > 0) {
                destroyConnection();
                createConnection(connectDialog.getIP(), connectDialog.getAuthKey());
            }
        }
        dialogCancelled = false;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        dialogCancelled = true;
    }

    @Override
    public void onCreateContextMenu (ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo)menuInfo;
        contextMenuID = info.id;
        final ChannelItem channel = adapter.getChannel((int)contextMenuID);
        if(channel == null)
            return;
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.listcontextmenu, menu);
        final MenuItem item = menu.findItem(channel.isSubscribed() ? R.id.subscribeChannelMenuItem : R.id.unsubscribeChannelMenuItem);
        item.setVisible(false);
        contextMenu = menu;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        contextMenu = null;
        switch(item.getItemId()) {
            case R.id.switchToChannelMenuItem:
                clientQueryParser.joinChannel(contextMenuID, getOwnClientID());
                return true;
            case R.id.subscribeChannelMenuItem:
                clientQueryParser.subscribeChannel(contextMenuID);
                return true;
            case R.id.unsubscribeChannelMenuItem:
                clientQueryParser.unsubscribeChannel(contextMenuID);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void addClient(final ImmutableClient client) {
        Log.d(TAG, "addClient: " + client.getName() + " " + client.isTalking());
        final ClientItem clientItem = new ClientItem(client.getID(), client.getName(), client.isTalking(), client.isInputMuted(), client.isOutputMuted(), client.isAway(), client.getTalkPower(), client.isTalker(), client.getCountry());
        adapter.addClient(clientItem, client.getChannelID());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void removeClient(final int clientID) {
        Log.d(TAG, "removeClient: " + clientID);
        adapter.removeClient(clientID);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void addChannel(final ImmutableChannel channel) {
        Log.d(TAG, "addChannel: " + channel.getID() + " " + channel.getName() + " " + channel.getChannelParentID() + " " + channel.getSortOrder());
        ChannelItem parentChannel = null;
        if(channel.getChannelParentID() != 0)
            parentChannel = adapter.getChannel(channel.getChannelParentID());
        final ChannelItem channelItem = new ChannelItem(channel.getID(), channel.getName(), parentChannel, channel.getSortOrder(), channel.isSubscribed(), channel.hasPassword(), channel.isDefault());
        adapter.addChannel(channelItem);
        adapter.notifyDataSetChanged();
        if(channelItem.isSubscribed()) {
            // Expand subscribed channels
            final int pos = adapter.getGroupPosition(channelItem);
            if(pos != -1) {
                listView.expandGroup(pos);
            }
        }
    }

    @Override
    public void moveChannel(final ImmutableChannel channel) {
        Log.d(TAG, "moveChannel: " + channel.getID() + " " + channel.getName() + " " + channel.getChannelParentID() + " " + channel.getSortOrder());
        removeChannel(channel.getID());
        addChannel(channel);
    }

    @Override
    public void removeChannel(final int channelID) {
        Log.d(TAG, "removeChannel: " + channelID);
        adapter.removeChannel(channelID);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void updateClientTalking(final int clientID, final boolean talking) {
        final ClientItem clientItem = adapter.getClient(clientID);
        if(clientItem == null)
            return;

        Log.d(TAG, "updateClientTalking: " + clientItem.getName() + " " + talking);
        clientItem.setTalking(talking);
        dirty = true;
    }

    @Override
    public void updateClient(final ImmutableClient client) {
        final ClientItem clientItem = adapter.getClient(client.getID());
        if(clientItem == null)
            return;

        final boolean sortParametersChanged = clientItem.getName() != client.getName() || clientItem.getTalkPower() != client.getTalkPower() || clientItem.isTalker() != client.isTalker();
        Log.d(TAG, "updateClient: " + client.getID() + " " + client.getName() + " -> " + sortParametersChanged);

        clientItem.setName(client.getName());
        clientItem.setInputMuted(client.isInputMuted());
        clientItem.setOutputMuted(client.isOutputMuted());
        clientItem.setAway(client.isAway());
        clientItem.setTalkPower(client.getTalkPower());
        clientItem.setIsTalker(client.isTalker());
        clientItem.setCountry(client.getCountry());

        if(sortParametersChanged) {
            final ChannelItem channel = adapter.getChannel(client.getChannelID());
            channel.removeClient(clientItem);
            channel.addClient(clientItem);
        }

        dirty = true;
    }

    @Override
    public void moveClient(final ImmutableClient client, final int oldChannelID) {
        Log.d(TAG, "moveClient: " + client.getName() + " " + client.getChannelID());
        adapter.moveClient(client.getID(), client.getChannelID(), oldChannelID);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void updateChannel(final ImmutableChannel channel, final boolean sortOrderChanged) {
        Log.d(TAG, "updateChannel: " + channel.getName() + " " + sortOrderChanged);
        // Sort order changed? Then remove and re-add channel
        if(sortOrderChanged) {
            removeChannel(channel.getID());
            addChannel(channel);
            return;
        }

        final ChannelItem channelItem = adapter.getChannel(channel.getID());
        if(channelItem == null)
            return;
        // Sort order the same, just update
        channelItem.setName(channel.getName());
        channelItem.setHasPassword(channel.hasPassword());
        channelItem.setIsDefault(channel.isDefault());
        dirty = true;
    }

    @Override
    public void subscribeChannel(final ImmutableChannel channel) {
        Log.d(TAG, "subscribeChannel: " + channel.getID() + " " + channel.getName());
        final ChannelItem channelItem = adapter.getChannel(channel.getID());
        if(channelItem == null)
            return;
        channelItem.setSubscribed(channel.isSubscribed());
        if(channel.isSubscribed() && !channelItem.isExpanded()) {
            final int pos = adapter.getGroupPosition(channelItem);
            if(pos != -1) {
                listView.expandGroup(pos);
            }
        }
        dirty = true;
    }

    @Override
    public void clear() {
        Log.d(TAG, "Clear");
        adapter.clear();
        if(contextMenu != null) {
            contextMenu.close();
            contextMenu = null;
        }
    }

    @Override
    public void updateServerName(final String serverName) {
        setTitle(serverName.length() > 0 ? serverName : getString(R.string.app_name));
    }

    @Override
    public void toastInfo(final String toastText) {
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void statusTextInfo(final String statusText) {
        Log.d(TAG, "statusTextInfo: " + statusText);
        final TextView textView = (TextView)findViewById(R.id.statusInfoTextView);
        if(textView != null) {
            textView.setText(statusText);
            textView.setVisibility(statusText.length() > 0 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onTS3Event(TS3Event evt) {
        if(evt instanceof TS3AddClientEvent) {
            final TS3AddClientEvent addEvt = (TS3AddClientEvent)evt;
            addClient(addEvt.getClient());
        } else if(evt instanceof TS3RemoveClientEvent) {
            final TS3RemoveClientEvent removeEvt = (TS3RemoveClientEvent)evt;
            removeClient(removeEvt.getClientID());
        } else if(evt instanceof TS3AddChannelEvent) {
            final TS3AddChannelEvent addChannelEvt = (TS3AddChannelEvent)evt;
            addChannel(addChannelEvt.getChannel());
        } else if(evt instanceof TS3RemoveChannelEvent) {
            final TS3RemoveChannelEvent rmChannelEvt = (TS3RemoveChannelEvent)evt;
            removeChannel(rmChannelEvt.getChannelID());
        } else if(evt instanceof TS3MoveChannelEvent) {
            final TS3MoveChannelEvent mvChannelEvt = (TS3MoveChannelEvent)evt;
            moveChannel(mvChannelEvt.getChannel());
        } else if(evt instanceof TS3ClientTalkingEvent) {
            final TS3ClientTalkingEvent talkEvt = (TS3ClientTalkingEvent)evt;
            updateClientTalking(talkEvt.getClientID(), talkEvt.isTalking());
        } else if(evt instanceof TS3UpdateClientEvent) {
            final TS3UpdateClientEvent muteEvt = (TS3UpdateClientEvent)evt;
            updateClient(muteEvt.getClient());
        } else if(evt instanceof TS3ClientMoveEvent) {
            final TS3ClientMoveEvent moveEvt = (TS3ClientMoveEvent)evt;
            moveClient(moveEvt.getClient(), moveEvt.getOldChannelID());
        } else if(evt instanceof TS3UpdateChannelEvent) {
            final TS3UpdateChannelEvent updateChannelEvt = (TS3UpdateChannelEvent)evt;
            updateChannel(updateChannelEvt.getChannel(), updateChannelEvt.getSortOrderChanged());
        } else if(evt instanceof TS3SubscribeChannelEvent) {
            final TS3SubscribeChannelEvent subscribeChannelEvt = (TS3SubscribeChannelEvent)evt;
            subscribeChannel(subscribeChannelEvt.getChannel());
        } else if(evt instanceof TS3ClearEvent) {
            clear();
        } else if(evt instanceof TS3UpdateServerNameEvent) {
            final TS3UpdateServerNameEvent updateServerNameEvt = (TS3UpdateServerNameEvent)evt;
            updateServerName(updateServerNameEvt.getServerName());
        } else if(evt instanceof TS3ToastEvent) {
            final TS3ToastEvent toastEvt = (TS3ToastEvent)evt;
            toastInfo(toastEvt.getInfoText());
        } else if(evt instanceof TS3StatusTextEvent) {
            final TS3StatusTextEvent statusTextEvent = (TS3StatusTextEvent)evt;
            statusTextInfo(statusTextEvent.getStatusText());
        }
    }

    private class ViewUpdateTimerTask extends TimerTask {
        @Override
        public void run() {
            if(dirty) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
                dirty = false;
            }
        }
    }
}