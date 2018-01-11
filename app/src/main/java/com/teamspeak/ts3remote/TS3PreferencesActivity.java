package com.teamspeak.ts3remote;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public final class TS3PreferencesActivity extends PreferenceActivity {
    static void openPreferences(final Context context) {
        context.startActivity(new Intent(context, TS3PreferencesActivity.class));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}