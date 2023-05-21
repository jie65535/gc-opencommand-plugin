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
package com.github.jie65535.opencommand.socket;

import com.github.jie65535.opencommand.EventListeners;
import com.github.jie65535.opencommand.OpenCommandConfig;
import com.github.jie65535.opencommand.OpenCommandPlugin;
import com.github.jie65535.opencommand.socket.packet.*;
import com.github.jie65535.opencommand.socket.packet.player.Player;
import com.github.jie65535.opencommand.socket.packet.player.PlayerList;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.command.CommandMap;
import emu.grasscutter.utils.JsonUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

// Socket 客户端
public class SocketClient {
    public static ClientThread clientThread;

    public static Logger mLogger;

    public static Timer timer;

    public static boolean connect = false;

    public static ReceiveThread receiveThread;

    // 连接服务器
    public static void connectServer() {
        if (connect) return;
        if (clientThread != null) {
            mLogger.warn("[OpenCommand] Retry connecting to the server after 15 seconds");
            try {
                Thread.sleep(15000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
        OpenCommandConfig config = OpenCommandPlugin.getInstance().getConfig();
        mLogger = OpenCommandPlugin.getInstance().getLogger();
        clientThread = new ClientThread(config.socketHost, config.socketPort);

        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new SendHeartBeatPacket(), 500);
        timer.schedule(new SendPlayerListPacket(), 1000);
    }

    // 发送数据包
    public static boolean sendPacket(BasePacket packet) {
        var p = SocketUtils.getPacket(packet);
        if (!clientThread.sendPacket(p)) {
            mLogger.warn("[OpenCommand] Send packet to server failed");
            mLogger.info("[OpenCommand] Reconnect to server");
            connect = false;
            connectServer();
            return false;
        }
        return true;
    }

    // 发送数据包带数据包ID
    public static boolean sendPacket(BasePacket packet, String packetID) {
        if (!clientThread.sendPacket(SocketUtils.getPacketAndPackID(packet, packetID))) {
            mLogger.warn("[OpenCommand] Send packet to server failed");
            mLogger.info("[OpenCommand] Reconnect to server");
            connect = false;
            connectServer();
            return false;
        }
        return true;
    }

    // 心跳包发送
    private static class SendHeartBeatPacket extends TimerTask {
        @Override
        public void run() {
            if (connect) {
                sendPacket(new HeartBeat("Pong"));
            }
        }
    }

    private static class SendPlayerListPacket extends TimerTask {
        @Override
        public void run() {
            if (connect) {
                PlayerList playerList = new PlayerList();
                playerList.player = Grasscutter.getGameServer().getPlayers().size();
                ArrayList<String> playerNames = new ArrayList<>();
                for (emu.grasscutter.game.player.Player player : Grasscutter.getGameServer().getPlayers().values()) {
                    playerNames.add(player.getNickname());
                    playerList.playerMap.put(player.getUid(), player.getNickname());
                }
                playerList.playerList = playerNames;
                sendPacket(playerList);
            }
        }
    }

    // 数据包接收
    private static class ReceiveThread extends Thread {
        private InputStream is;
        private boolean exit = false;

        public ReceiveThread(Socket socket) {
            try {
                is = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            start();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (exit) {
                        return;
                    }
                    String data = SocketUtils.readString(is);
                    Packet packet = JsonUtils.decode(data, Packet.class);
                    switch (packet.type) {
                        // 玩家类
                        case Player:
                            var player = JsonUtils.decode(packet.data, Player.class);
                            switch (player.type) {
                                // 运行命令
                                case RunCommand -> {
                                    var command = player.data;
                                    var playerData = OpenCommandPlugin.getInstance().getServer().getPlayerByUid(player.uid);
                                    if (playerData == null) {
                                        sendPacket(new HttpPacket(404, "Player not found."), packet.packetID);
                                        return;
                                    }
                                    // Player MessageHandler do not support concurrency
                                    var handler = EventListeners.getPlayerMessageHandler(playerData.getUid());
                                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                                    synchronized (handler) {
                                        try {
                                            handler.setLength(0);
                                            CommandMap.getInstance().invoke(playerData, playerData, command);
                                            sendPacket(new HttpPacket(200, handler.toString()), packet.packetID);
                                        } catch (Exception e) {
                                            OpenCommandPlugin.getInstance().getLogger().warn("[OpenCommand] Run command failed.", e);
                                            sendPacket(new HttpPacket(500, "error", e.getLocalizedMessage()), packet.packetID);
                                        }
                                    }
                                }
                                // 发送信息
                                case DropMessage -> {
                                    var playerData = OpenCommandPlugin.getInstance().getServer().getPlayerByUid(player.uid);
                                    if (playerData == null) {
                                        return;
                                    }
                                    playerData.dropMessage(player.data);
                                }
                            }
                            break;
                        case RunConsoleCommand:
                            var consoleCommand = JsonUtils.decode(packet.data, RunConsoleCommand.class);
                            var plugin = OpenCommandPlugin.getInstance();
                            //noinspection SynchronizationOnLocalVariableOrMethodParameter
                            synchronized (plugin) {
                                try {
                                    var resultCollector = new StringBuilder();
                                    EventListeners.setConsoleMessageHandler(resultCollector);
                                    CommandMap.getInstance().invoke(null, null, consoleCommand.command);
                                    sendPacket(new HttpPacket(resultCollector.toString()), packet.packetID);
                                } catch (Exception e) {
                                    mLogger.warn("[OpenCommand] Run command failed.", e);
                                    EventListeners.setConsoleMessageHandler(null);
                                    sendPacket(new HttpPacket(500, "error", e.getLocalizedMessage()), packet.packetID);
                                } finally {
                                    EventListeners.setConsoleMessageHandler(null);
                                }
                            }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    if (!sendPacket(new HeartBeat("Pong"))) {
                        return;
                    }
                }
            }
        }

        public void exit() {
            exit = true;
        }
    }

    // 客户端连接线程
    private static class ClientThread extends Thread {
        private final String ip;
        private final int port;
        private Socket socket;
        private OutputStream os;

        public ClientThread(String ip, int port) {
            this.ip = ip;
            this.port = port;
            start();
        }

        public Socket getSocket() {
            return socket;
        }

        public boolean sendPacket(String string) {
            return SocketUtils.writeString(os, string);
        }

        @Override
        public void run() {
            try {
                connect = true;
                if (receiveThread != null) {
                    receiveThread.exit();
                }

                socket = new Socket(ip, port);
                os = socket.getOutputStream();
                mLogger.info("[OpenCommand] Connect to server: " + ip + ":" + port);
                SocketClient.sendPacket(new AuthPacket(OpenCommandPlugin.getInstance().getConfig().socketToken, OpenCommandPlugin.getInstance().getConfig().socketDisplayName));
                receiveThread = new ReceiveThread(socket);
            } catch (IOException e) {
                connect = false;
                mLogger.warn("[OpenCommand] Connect to server failed: " + ip + ":" + port);
                connectServer();
            }
        }
    }
}
