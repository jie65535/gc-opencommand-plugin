package com.github.jie65535.opencommand;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.plugin.Plugin;

public class OpenCommandPlugin extends Plugin {
    private OpenCommandHandler handler;

    @Override
    public void onLoad() {
        handler = new OpenCommandHandler();
    }


    @Override
    public void onEnable() {
        var app = Grasscutter.getDispatchServer().getServer();
        app.post("/opencommand/api", handler);
        Grasscutter.getLogger().info("Open command enabled");
    }

    @Override
    public void onDisable() {

    }
}
