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

import com.github.jie65535.opencommand.OpenCommandPlugin;
import com.github.jie65535.opencommand.socket.packet.*;
import com.github.jie65535.opencommand.socket.packet.player.PlayerList;
import emu.grasscutter.utils.JsonUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

// Socket 服务器
public class SocketServer {
    // 客户端超时时间
    private static final int TIMEOUT = 5000;
    private static final HashMap<String, ClientInfo> clientList = new HashMap<>();

    private static final HashMap<String, Integer> clientTimeout = new HashMap<>();
    private static Logger mLogger;

    public static HashMap<String, String> getOnlineClient() {
        HashMap<String, String> onlineClient = new HashMap<>();
        for (var key : clientList.entrySet()) {
            onlineClient.put(key.getValue().uuid, key.getValue().clientThread.getDisplayName());
        }
        return onlineClient;
    }

    public static ClientInfo getClientInfoByUuid(String uuid) {
        for (var key : clientList.entrySet()) {
            if (key.getValue().uuid.equals(uuid)) {
                return key.getValue();
            }
        }
        return null;
    }

    public static void startServer() throws IOException {
        int port = OpenCommandPlugin.getInstance().getConfig().socketPort;
        mLogger = OpenCommandPlugin.getInstance().getLogger();
        new Timer().schedule(new SocketClientCheck(), 500);
        new WaitClientConnect(port);
    }

    // 向全部客户端发送数据
    public static boolean sendAllPacket(BasePacket packet) {
        var p = SocketUtils.getPacket(packet);
        HashMap<String, ClientInfo> old = (HashMap<String, ClientInfo>) clientList.clone();
        for (var client : old.entrySet()) {
            if (!client.getValue().clientThread.sendPacket(p)) {
                mLogger.warn("[OpenCommand] Send packet to client {} failed", client.getKey());
                clientList.remove(client.getKey());
            }
        }
        return false;
    }

    // 根据地址发送到相应的客户端
    public static boolean sendPacket(String address, BasePacket packet) {
        var p = SocketUtils.getPacket(packet);
        var client = clientList.get(address);
        if (client != null) {
            if (client.clientThread.sendPacket(p)) {
                return true;
            }
            mLogger.warn("[OpenCommand] Send packet to client {} failed", address);
            clientList.remove(address);
        }
        return false;
    }

    public static boolean sendPacketAndWait(String address, BasePacket packet, SocketDataWait<?> wait) {
        var p = SocketUtils.getPacketAndPackID(packet);
        var client = clientList.get(address);
        if (client != null) {
            wait.uid = p.get(0);
            if (client.clientThread.sendPacket(p.get(1), wait)) {
                return true;
            }
            mLogger.warn("[OpenCommand] Send packet to client {} failed", address);
            clientList.remove(address);
        }
        return false;
    }

    public static boolean sendUidPacket(Integer playerId, BasePacket player) {
        var p = SocketUtils.getPacket(player);
        var clientID = SocketData.getPlayerInServer(playerId);
        if (clientID == null) return false;
        var client = clientList.get(clientID);
        if (client != null) {
            if (!client.clientThread.sendPacket(p)) {
                mLogger.warn("[OpenCommand] Send packet to client {} failed", clientID);
                clientList.remove(clientID);
                return false;
            }
            return true;
        }
        return false;
    }

    // 根据Uid发送到相应的客户端异步返回数据
    public static boolean sendUidPacketAndWait(Integer playerId, BasePacket player, SocketDataWait<?> socketDataWait) {
        var p = SocketUtils.getPacketAndPackID(player);
        var clientID = SocketData.getPlayerInServer(playerId);
        if (clientID == null) return false;
        var client = clientList.get(clientID);
        if (client != null) {
            socketDataWait.uid = p.get(0);
            if (!client.clientThread.sendPacket(p.get(1), socketDataWait)) {
                mLogger.warn("[OpenCommand] Send packet to client {} failed", clientID);
                clientList.remove(clientID);
                return false;
            }
            return true;
        }
        return false;
    }

    // 客户端超时检测
    private static class SocketClientCheck extends TimerTask {
        @Override
        public void run() {
            HashMap<String, Integer> old = (HashMap<String, Integer>) clientTimeout.clone();
            for (var client : old.entrySet()) {
                var clientID = client.getKey();
                var clientTime = client.getValue();
                if (clientTime > TIMEOUT) {
                    mLogger.info("[OpenCommand] Client {} timeout, disconnect.", clientID);
                    clientList.remove(clientID);
                    clientTimeout.remove(clientID);
                    SocketData.playerList.remove(clientID);
                } else {
                    clientTimeout.put(clientID, clientTime + 500);
                }
            }
        }
    }

    // 客户端数据包处理
    static class ClientThread extends Thread {
        private final Socket socket;
        private InputStream is;
        private OutputStream os;
        private final String address;
        private final String token;
        private boolean auth = false;
        private String displayName = "";

        private final HashMap<String, SocketDataWait<?>> socketDataWaitList = new HashMap<>();

        public ClientThread(Socket accept) {
            socket = accept;
            address = socket.getInetAddress() + ":" + socket.getPort();
            token = OpenCommandPlugin.getInstance().getConfig().socketToken;
            try {
                is = accept.getInputStream();
                os = accept.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            start();
        }

        public Socket getSocket() {
            return socket;
        }

        // 发送数据包
        public boolean sendPacket(String packet) {
            return SocketUtils.writeString(os, packet);
        }

        // 发送异步数据包
        public boolean sendPacket(String packet, SocketDataWait<?> socketDataWait) {
            if (SocketUtils.writeString(os, packet)) {
                socketDataWaitList.put(socketDataWait.uid, socketDataWait);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    String data = SocketUtils.readString(is);
                    Packet packet = JsonUtils.decode(data, Packet.class);
                    if (packet.type == PacketEnum.AuthPacket) {
                        AuthPacket authPacket = JsonUtils.decode(packet.data, AuthPacket.class);
                        if (authPacket.token.equals(token)) {
                            auth = true;
                            displayName = authPacket.displayName;
                            mLogger.info("[OpenCommand] Client {} auth success, name: {}", address, displayName);
                            clientList.put(address, new ClientInfo(UUID.randomUUID().toString(), address, this));
                            clientTimeout.put(address, 0);
                        } else {
                            mLogger.warn("[OpenCommand] Client {} auth failed", address);
                            socket.close();
                            return;
                        }
                    }
                    if (!auth) {
                        mLogger.warn("[OpenCommand] Client {} auth failed", address);
                        socket.close();
                        return;
                    }
                    switch (packet.type) {
                        // 缓存玩家列表
                        case PlayerList -> {
                            PlayerList playerList = JsonUtils.decode(packet.data, PlayerList.class);
                            SocketData.playerList.put(address, playerList);
                        }
                        // Http信息返回
                        case HttpPacket -> {
                            HttpPacket httpPacket = JsonUtils.decode(packet.data, HttpPacket.class);
                            var socketWait = socketDataWaitList.get(packet.packetID);
                            if (socketWait == null) {
                                mLogger.error("[OpenCommand] HttpPacket: " + packet.packetID + " not found");
                                return;
                            }
                            socketWait.setData(httpPacket);
                            socketDataWaitList.remove(packet.packetID);
                        }
                        // 心跳包
                        case HeartBeat -> clientTimeout.put(address, 0);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    mLogger.error("[OpenCommand] Client {} disconnect.", address);
                    clientList.remove(address);
                    clientTimeout.remove(address);
                    SocketData.playerList.remove(address);
                    break;
                }
            }
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // 等待客户端连接
    private static class WaitClientConnect extends Thread {
        ServerSocket socketServer;

        public WaitClientConnect(int port) throws IOException {
            socketServer = new ServerSocket(port);
            start();
        }

        @Override
        public void run() {
            mLogger.info("[OpenCommand] Start socket server on port " + socketServer.getLocalPort());
            // noinspection InfiniteLoopStatement
            while (true) {
                try {
                    Socket accept = socketServer.accept();
                    String address = accept.getInetAddress() + ":" + accept.getPort();
                    mLogger.info("[OpenCommand] Client connect: " + address);
                    new ClientThread(accept);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
