package com.github.jie65535.opencommand;

import com.github.jie65535.opencommand.socket.SocketClient;
import com.github.jie65535.opencommand.socket.SocketUtils;
import com.github.jie65535.opencommand.socket.packet.player.PlayerList;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.server.event.game.ReceiveCommandFeedbackEvent;
import emu.grasscutter.server.event.player.PlayerJoinEvent;
import emu.grasscutter.server.event.player.PlayerQuitEvent;
import emu.grasscutter.utils.MessageHandler;

import java.util.ArrayList;

public final class EventListeners {

    private static MessageHandler consoleMessageHandler;

    public static void setConsoleMessageHandler(MessageHandler handler) {
        consoleMessageHandler = handler;
    }

    public static void onCommandResponse(ReceiveCommandFeedbackEvent event) {
        if (consoleMessageHandler != null && event.getPlayer() == null) {
            consoleMessageHandler.setMessage(event.getMessage());
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
