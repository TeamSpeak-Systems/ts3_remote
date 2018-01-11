package com.teamspeak.ts3remote;

import android.app.Activity;
import android.util.Log;
import com.teamspeak.ts3remote.data.Common;

final class TS3EventSender implements Common {
    private TS3Listener singleListener;

    void addTS3Listener(final TS3Listener l) {
        if(singleListener != null) {
            Log.e(TAG, "Already have singleListener");
        }
        singleListener = l;
    }

    void fireTS3Event(final TS3Event evt) {

        Activity activity = (Activity)singleListener;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                singleListener.onTS3Event(evt);
            }
        });
    }
}
