package com.teamspeak.ts3remote.data;

/**
 * Client is the state of the client.
 *
 */
public final class Client {
    /**
     * Client id
     */
    private final int id;

    /**
     * Display Name
     */
    private String name;

    /**
     * Current channel user is inside
     */
    private int channelID;

    /**
     * Is user talking
     */
    private boolean isTalking;

    /**
     *
     */
    private boolean inputMuted;

    /**
     *
     */
    private boolean hardwareMuted;

    /**
     *
     */
    private boolean outputMuted;

    /**
     *
     */
    private boolean hardwareOutputMuted;

    /**
     * Is user afk
     */
    private boolean away;

    /**
     * current talkpower
     */
    private int talkPower;

    /**
     *
     */
    private boolean isTalker;

    /**
     * Country code
     */
    private String country;

    public Client(final int clientID, final String name, final int channelID, final boolean isTalking, final boolean inputMuted, final boolean hardwareMuted, final boolean outputMuted, final boolean hardwareOutputMuted, final boolean away, final int talkPower, final boolean isTalker, final String country) {
        this.id = clientID;
        this.name = name;
        this.channelID = channelID;
        this.isTalking = isTalking;
        this.inputMuted = inputMuted;
        this.hardwareMuted = hardwareMuted;
        this.outputMuted = outputMuted;
        this.hardwareOutputMuted = hardwareOutputMuted;
        this.away = away;
        this.talkPower = talkPower;
        this.isTalker = isTalker;
        this.country = country;
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

    public int getChannelID() {
        return channelID;
    }

    public void setChannelID(final int channelID) {
        this.channelID = channelID;
    }

    public boolean isTalking() {
        return isTalking;
    }

    public void setTalking(final boolean isTalking) {
        this.isTalking = isTalking;
    }

    public boolean isInputMuted() {
        return inputMuted;
    }

    public void setInputMuted(final boolean inputMuted) {
        this.inputMuted = inputMuted;
    }

    public boolean isHardwareMuted() {
        return hardwareMuted;
    }

    public void setHardwareMuted(final boolean hardwareMuted) {
        this.hardwareMuted = hardwareMuted;
    }

    public boolean isOutputMuted() {
        return outputMuted;
    }

    public void setOutputMuted(final boolean outputMuted) {
        this.outputMuted = outputMuted;
    }

    public boolean isHardwareOutputMuted() {
        return hardwareOutputMuted;
    }

    public void setHardwareOutputMuted(final boolean hardwareOutputMuted) {
        this.hardwareOutputMuted = hardwareOutputMuted;
    }

    public boolean isAway() {
        return away;
    }

    public void setAway(final boolean away) {
        this.away = away;
    }

    public int getTalkPower() {
        return talkPower;
    }

    public void setTalkPower(final int talkPower) {
        this.talkPower = talkPower;
    }

    public boolean isTalker() {
        return isTalker;
    }

    public void setIsTalker(final boolean isTalker) {
        this.isTalker = isTalker;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }
}
