package com.github.jie65535.opencommand.socket;

import com.github.jie65535.opencommand.OpenCommandConfig;
import com.github.jie65535.opencommand.OpenCommandPlugin;
import com.github.jie65535.opencommand.socket.packet.BasePacket;
import com.github.jie65535.opencommand.socket.packet.HeartBeat;
import com.github.jie65535.opencommand.socket.packet.HttpPacket;
import com.github.jie65535.opencommand.socket.packet.Packet;
import com.github.jie65535.opencommand.socket.packet.player.Player;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.command.CommandMap;
import emu.grasscutter.utils.MessageHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

// Socket 客户端
public class SocketClient {
    public static ClientThread clientThread;

    public static Logger mLogger;

    public static Timer timer;

    public static boolean connect = false;

    // 连接服务器
    public static void connectServer() {
        OpenCommandConfig config = OpenCommandPlugin.getInstance().getConfig();
        mLogger = OpenCommandPlugin.getInstance().getLogger();
        clientThread = new ClientThread(config.socketHost, config.socketPort);

        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new SendHeartBeatPacket(), 500);
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

    // 数据包接收
    private static class ReceiveThread extends Thread {
        private InputStream is;
        private String token;

        public ReceiveThread(Socket socket) {
            token = OpenCommandPlugin.getInstance().getConfig().socketToken;
            try {
                is = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            start();
        }

        @Override
        public void run() {
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    String data = SocketUtils.readString(is);
                    Packet packet = Grasscutter.getGsonFactory().fromJson(data, Packet.class);
                    if (packet.token.equals(token)) {
                        switch (packet.type) {
                            // 玩家类
                            case Player:
                                var player = Grasscutter.getGsonFactory().fromJson(packet.data, Player.class);
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
                                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                                        synchronized (playerData) {
                                            try {
                                                var resultCollector = new MessageHandler();
                                                playerData.setMessageHandler(resultCollector);
                                                CommandMap.getInstance().invoke(playerData, playerData, command);
                                                sendPacket(new HttpPacket(200, resultCollector.getMessage()), packet.packetID);
                                            } catch (Exception e) {
                                                OpenCommandPlugin.getInstance().getLogger().warn("Run command failed.", e);
                                                sendPacket(new HttpPacket(500, "error", e.getLocalizedMessage()), packet.packetID);
                                            } finally {
                                                playerData.setMessageHandler(null);
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
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
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
                socket = new Socket(ip, port);
                connect = true;
                os = socket.getOutputStream();
                mLogger.info("Connect to server: " + ip + ":" + port);
                new ReceiveThread(socket);
            } catch (IOException e) {
                connect = false;
                mLogger.warn("Connect to server failed: " + ip + ":" + port);
                mLogger.warn("[OpenCommand] Reconnect to server");
                connectServer();
                throw new RuntimeException(e);
            }
        }
    }
}
