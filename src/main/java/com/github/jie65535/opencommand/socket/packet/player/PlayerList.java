package com.github.jie65535.opencommand.socket.packet.player;

import com.github.jie65535.opencommand.socket.packet.BasePacket;
import com.github.jie65535.opencommand.socket.packet.PacketEnum;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 玩家列表信息
public class PlayerList extends BasePacket {
    public int player = -1;
    public List<String> playerList = new ArrayList<>();
    public Map<Integer, String> playerMap = new HashMap<>();

    @Override
    public String getPacket() {
        return Grasscutter.getGsonFactory().toJson(this);
    }

    @Override
    public PacketEnum getType() {
        return PacketEnum.PlayerList;
    }
}
