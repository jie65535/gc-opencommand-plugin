package com.github.jie65535.opencommand.socket.packet;

// 基本数据包
public abstract class BasePacket {
    public abstract String getPacket();

    public abstract PacketEnum getType();
}
