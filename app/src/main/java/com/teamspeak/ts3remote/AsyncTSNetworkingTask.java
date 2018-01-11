package com.teamspeak.ts3remote;

import android.os.AsyncTask;

/**
 * AsyncTSNetworkingTask executes the actual request in async code.
 */
final class AsyncTSNetworkingTask extends AsyncTask<String, Void, Void>
{
    private ClientQueryParser cqp = null;
    AsyncTSNetworkingTask(ClientQueryParser _cqp) {
        this.cqp = _cqp;
    }

    @Override
    protected Void doInBackground(String... params) {
        if (params == null || params.length == 0)
            return null;

        for (int i = 0; i < params.length; i++) {
            String command = params[i];
            cqp.send(command);
        }

        return null;
    }
}
