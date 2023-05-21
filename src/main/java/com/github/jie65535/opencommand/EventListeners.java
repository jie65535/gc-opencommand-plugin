/*
 * gc-opencommand
 * Copyright (C) 2022  jie65535
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.jie65535.opencommand;

import com.github.jie65535.opencommand.socket.SocketClient;
import com.github.jie65535.opencommand.socket.packet.player.PlayerList;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.server.event.game.ReceiveCommandFeedbackEvent;
import emu.grasscutter.server.event.player.PlayerJoinEvent;
import emu.grasscutter.server.event.player.PlayerQuitEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.ArrayList;

public final class EventListeners {

    private static StringBuilder consoleMessageHandler;
    private static final Int2ObjectMap<StringBuilder> playerMessageHandlers = new Int2ObjectOpenHashMap<>();

    public static void setConsoleMessageHandler(StringBuilder handler) {
        consoleMessageHandler = handler;
    }

    /**
     * 获取新的玩家消息处理类
     * 获取时将创建或清空消息处理器并返回实例，**在执行命令前获取！**
     * @param uid 玩家uid
     * @return 新的玩家消息处理类
     */
    public static StringBuilder getPlayerMessageHandler(int uid) {
        var handler = playerMessageHandlers.get(uid);
        if (handler == null) {
            handler = new StringBuilder();
            playerMessageHandlers.put(uid, handler);
        }
        return handler;
    }

    /**
     * 命令执行反馈事件处理
     */
    public static void onCommandResponse(ReceiveCommandFeedbackEvent event) {
        StringBuilder handler;
        if (event.getPlayer() == null) {
            handler = consoleMessageHandler;
        } else {
            handler = playerMessageHandlers.get(event.getPlayer().getUid());
        }

        if (handler != null) {
            if (!handler.isEmpty()) {
                // New line
                handler.append(System.lineSeparator());
            }
            handler.append(event.getMessage());
        }
    }

    public static void onPlayerJoin(PlayerJoinEvent playerJoinEvent) {
        PlayerList playerList = new PlayerList();
        playerList.player = Grasscutter.getGameServer().getPlayers().size();
        ArrayList<String> playerNames = new ArrayList<>();
        playerNames.add(playerJoinEvent.getPlayer().getNickname());
        playerList.playerMap.put(playerJoinEvent.getPlayer().getUid(), playerJoinEvent.getPlayer().getNickname());
        for (Player player : Grasscutter.getGameServer().getPlayers().values()) {
            playerNames.add(player.getNickname());
            playerList.playerMap.put(player.getUid(), player.getNickname());
        }
        playerList.playerList = playerNames;

        SocketClient.sendPacket(playerList);
    }

    /**
     * 仅游戏模式下玩家离开事件处理方法
     * 用于更新玩家列表
     */
    public static void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
        PlayerList playerList = new PlayerList();
        playerList.player = Grasscutter.getGameServer().getPlayers().size();
        ArrayList<String> playerNames = new ArrayList<>();
        for (Player player : Grasscutter.getGameServer().getPlayers().values()) {
            playerNames.add(player.getNickname());
            playerList.playerMap.put(player.getUid(), player.getNickname());
        }
        playerList.playerMap.remove(playerQuitEvent.getPlayer().getUid());
        playerNames.remove(playerQuitEvent.getPlayer().getNickname());
        playerList.playerList = playerNames;
        SocketClient.sendPacket(playerList);
    }

    /**
     * 玩家离开事件处理 2
     * 用于清理内存
     */
    public static void onPlayerQuit2(PlayerQuitEvent playerQuitEvent) {
        var uid = playerQuitEvent.getPlayer().getUid();
        if (playerMessageHandlers.containsKey(uid)) {
            playerMessageHandlers.remove(uid);
        }
    }
}
