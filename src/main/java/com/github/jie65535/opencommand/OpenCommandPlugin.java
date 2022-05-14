package com.github.jie65535.opencommand;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.plugin.Plugin;

public class OpenCommandPlugin extends Plugin {
    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {
        Grasscutter.getHttpServer().addRouter(OpenCommandHandler.class);
        Grasscutter.getLogger().info("[OpenCommand] Enabled");
    }

    @Override
    public void onDisable() {
        Grasscutter.getLogger().info("[OpenCommand] Disabled");
    }
}
