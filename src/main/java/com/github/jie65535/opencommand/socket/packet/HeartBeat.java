package com.github.jie65535.opencommand.socket.packet;

import emu.grasscutter.Grasscutter;

// 心跳包
public class HeartBeat extends BasePacket {
    public String ping;

    public HeartBeat(String ping) {
        this.ping = ping;
    }

    @Override
    public String getPacket() {
        return Grasscutter.getGsonFactory().toJson(this);
    }

    @Override
    public PacketEnum getType() {
        return PacketEnum.HeartBeat;
    }
}
