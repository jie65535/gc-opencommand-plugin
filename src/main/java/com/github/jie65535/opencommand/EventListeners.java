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

import java.util.ArrayList;

public final class EventListeners {

    private static StringBuilder consoleMessageHandler;

    public static void setConsoleMessageHandler(StringBuilder handler) {
        consoleMessageHandler = handler;
    }

    public static void onCommandResponse(ReceiveCommandFeedbackEvent event) {
        if (consoleMessageHandler != null && event.getPlayer() == null) {
            if (!consoleMessageHandler.isEmpty()) {
                // New line
                consoleMessageHandler.append(System.lineSeparator());
            }
            consoleMessageHandler.append(event.getMessage());
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
}
