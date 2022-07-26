package com.github.jie65535.opencommand.socket.packet;

import emu.grasscutter.Grasscutter;

public class RunConsoleCommand extends BasePacket {
    public String command;

    public RunConsoleCommand(String command) {
        this.command = command;
    }

    @Override
    public String getPacket() {
        return Grasscutter.getGsonFactory().toJson(this);
    }

    @Override
    public PacketEnum getType() {
        return PacketEnum.RunConsoleCommand;
    }
}
