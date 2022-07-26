package com.github.jie65535.opencommand.socket;

import com.github.jie65535.opencommand.socket.packet.player.PlayerList;

import java.util.ArrayList;
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

    public static OnlinePlayer getOnlinePlayer() {
        ArrayList<String> player = new ArrayList<>();
        playerList.forEach((address, playerMap) -> playerMap.playerMap.forEach((uid, name) -> player.add(name)));
        return new OnlinePlayer(player);
    }

    public static class OnlinePlayer {
        public int count;
        public ArrayList<String> playerList;

        public OnlinePlayer(ArrayList<String> playerList) {
            this.playerList = playerList;
            this.count = playerList.size();
        }

    }
}
