package com.teamspeak.ts3remote.data;

/**
 * ImmutableClient - can be created from a normal Client.
 */
public final class ImmutableClient {
    private final int id;
    private final String name;
    private final int channelID;
    private final boolean isTalking;
    private final boolean inputMuted;
    private final boolean outputMuted;
    private final boolean away;
    private final int talkPower;
    private final boolean isTalker;
    private final String country;

    private ImmutableClient(final int clientID, final String name, final int channelID, final boolean isTalking, final boolean inputMuted, final boolean outputMuted, final boolean away, final int talkPower, final boolean isTalker, final String country) {
        this.id = clientID;
        this.name = name;
        this.channelID = channelID;
        this.inputMuted = inputMuted;
        this.outputMuted = outputMuted;
        this.away = away;
        this.isTalking = isTalking;
        this.talkPower = talkPower;
        this.isTalker = isTalker;
        this.country = country;
    }

    public static final ImmutableClient create(final Client client) {
        return new ImmutableClient(client.getID(), client.getName(), client.getChannelID(), client.isTalking(),
                client.isInputMuted() || client.isHardwareMuted(),
                client.isOutputMuted() || client.isHardwareOutputMuted(),
                client.isAway(), client.getTalkPower(), client.isTalker(), client.getCountry());
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getChannelID() {
        return channelID;
    }

    public boolean isTalking() {
        return isTalking;
    }

    public boolean isInputMuted() {
        return inputMuted;
    }

    public boolean isOutputMuted() {
        return outputMuted;
    }

    public boolean isAway() {
        return away;
    }

    public int getTalkPower() {
        return talkPower;
    }

    public boolean isTalker() {
        return isTalker;
    }

    public String getCountry() {
        return country;
    }
}
