package com.github.jie65535.opencommand.socket;

import com.github.jie65535.opencommand.socket.packet.player.PlayerList;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

// Socket 数据保存
public class SocketData {
    public static HashMap<String, PlayerList> playerList = new HashMap<>();

    public static String getPlayer(int uid) {
        for (PlayerList player : playerList.values()) {
            if (player.playerMap.get(uid) != null) {
                return player.playerMap.get(uid);
            }
        }
        return null;
    }

    public static String getPlayerInServer(int uid) {
        AtomicReference<String> ret = new AtomicReference<>();
        playerList.forEach((key, value) -> {
            if (value.playerMap.get(uid) != null) {
                ret.set(key);
            }
        });
        return ret.get();
    }
}
