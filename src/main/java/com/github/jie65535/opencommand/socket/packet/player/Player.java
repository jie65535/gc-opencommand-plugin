package com.github.jie65535.opencommand.socket.packet.player;

import com.github.jie65535.opencommand.socket.SocketServer;
import com.github.jie65535.opencommand.socket.packet.BasePacket;
import com.github.jie65535.opencommand.socket.packet.PacketEnum;
import emu.grasscutter.Grasscutter;

// 玩家操作类
public class Player extends BasePacket {
    public PlayerEnum type;
    public int uid;
    public String data;

    @Override
    public String getPacket() {
        return Grasscutter.getGsonFactory().toJson(this);
    }

    @Override
    public PacketEnum getType() {
        return PacketEnum.Player;
    }

    public static void dropMessage(int uid, String str) {
        Player p = new Player();
        p.type = PlayerEnum.DropMessage;
        p.uid = uid;
        p.data = str;
        SocketServer.sendAllPacket(p);
    }
}
