package com.github.jie65535.opencommand.socket.packet;

import emu.grasscutter.Grasscutter;

public class AuthPacket extends BasePacket {
    public String token;
    public String displayName;

    public AuthPacket(String token, String displayName) {
        this.displayName = displayName;
        this.token = token;
    }

    @Override
    public String getPacket() {
        return Grasscutter.getGsonFactory().toJson(this);
    }

    @Override
    public PacketEnum getType() {
        return PacketEnum.AuthPacket;
    }

    @Override
    public String toString() {
        return "AuthPacket [token=" + token + ", displayName=" + displayName + "]";
    }
}
