package com.github.jie65535.opencommand.socket.packet;

// 数据包结构
public class Packet {
    public String token;
    public PacketEnum type;
    public String data;
    public String packetID;

    @Override
    public String toString() {
        return "Packet [token=" + token + ", type=" + type + ", data=" + data + ", packetID=" + packetID + "]";
    }
}
