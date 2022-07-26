package com.github.jie65535.opencommand.socket;

public class ClientInfo {

    public final String uuid;
    public final SocketServer.ClientThread clientThread;
    public final String ip;

    public ClientInfo(String uuid, String ip, SocketServer.ClientThread clientThread) {
        this.uuid = uuid;
        this.clientThread = clientThread;
        this.ip = ip;
    }
}
