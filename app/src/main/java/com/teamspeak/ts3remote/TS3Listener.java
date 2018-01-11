package com.teamspeak.ts3remote;

import java.util.EventListener;

interface TS3Listener extends EventListener {
    void onTS3Event(TS3Event evt);
}
