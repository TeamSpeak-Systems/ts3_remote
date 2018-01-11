package com.teamspeak.ts3remote;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

final class ConnectDialog extends Dialog {
    static final String PREFS_KEY = "StoredIP";
    static final String PREFS_AUTH_KEY = "AuthKey";
    private OnDismissListener dismissListener;
    private OnCancelListener cancelListener;

    ConnectDialog(Context context, OnDismissListener dismissListener, OnCancelListener cancelListener) {
        super(context);
        this.dismissListener = dismissListener;
        this.cancelListener = cancelListener;
    }

    String getIP() {
        EditText ipEditText = (EditText) findViewById(R.id.ipEditText);
        return ipEditText.getText().toString();
    }

    String getAuthKey() {
        EditText authkeyEditText = (EditText) findViewById(R.id.authkeyEditText);
        return authkeyEditText.getText().toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.connect);
        setContentView(R.layout.connectdialog);
        setOnDismissListener(dismissListener);
        setOnCancelListener(cancelListener);

        // Load last IP from store
        SharedPreferences prefs = getOwnerActivity().getPreferences(Context.MODE_PRIVATE);
        if (prefs.contains(PREFS_KEY)) {
            final String storedIP = prefs.getString(PREFS_KEY, "");
            if (storedIP.length() > 0) {
                EditText ipEditText = (EditText) findViewById(R.id.ipEditText);
                ipEditText.setText(storedIP);
            }
        }

        // Load last AuthKey from Store
        if (prefs.contains(PREFS_AUTH_KEY)) {
            final String storedauthkey = prefs.getString(PREFS_AUTH_KEY, "");
            if (storedauthkey.length() > 0) {
                EditText authkeyEditText = (EditText) findViewById(R.id.authkeyEditText);
                authkeyEditText.setText(storedauthkey);
            }
        }

        Button button = (Button) findViewById(R.id.okButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            SharedPreferences prefs = getOwnerActivity().getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREFS_KEY, getIP());
            editor.putString(PREFS_AUTH_KEY, getAuthKey());

            editor.commit();
            dismiss();
            }
        });

        button = (Button) findViewById(R.id.cancelButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
            }
        });
    }
}
