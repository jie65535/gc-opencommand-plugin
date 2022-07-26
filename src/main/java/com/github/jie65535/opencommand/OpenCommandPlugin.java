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
import com.github.jie65535.opencommand.socket.SocketServer;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.plugin.Plugin;
import emu.grasscutter.server.event.EventHandler;
import emu.grasscutter.server.event.HandlerPriority;
import emu.grasscutter.server.event.game.ReceiveCommandFeedbackEvent;
import emu.grasscutter.server.event.player.PlayerJoinEvent;
import emu.grasscutter.server.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class OpenCommandPlugin extends Plugin {

    private static OpenCommandPlugin instance;

    public static OpenCommandPlugin getInstance() {
        return instance;
    }

    private OpenCommandConfig config;

    @Override
    public void onLoad() {
        instance = this;
        loadConfig();
    }

    @Override
    public void onEnable() {
        new EventHandler<>(ReceiveCommandFeedbackEvent.class)
                .priority(HandlerPriority.HIGH)
                .listener(EventListeners::onCommandResponse)
                .register(this);
        if (Grasscutter.getConfig().server.runMode == Grasscutter.ServerRunMode.GAME_ONLY) {
            // 仅运行游戏服务器时注册玩家加入和离开事件
            new EventHandler<>(PlayerJoinEvent.class)
                    .priority(HandlerPriority.HIGH)
                    .listener(EventListeners::onPlayerJoin)
                    .register(this);
            new EventHandler<>(PlayerQuitEvent.class)
                    .priority(HandlerPriority.HIGH)
                    .listener(EventListeners::onPlayerQuit)
                    .register(this);
        } else if (Grasscutter.getConfig().server.runMode == Grasscutter.ServerRunMode.DISPATCH_ONLY) {
            getHandle().addRouter(OpenCommandOnlyHttpHandler.class);
        } else {
            getHandle().addRouter(OpenCommandHandler.class);
        }
        getLogger().info("[OpenCommand] Enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("[OpenCommand] Disabled");
    }

    public OpenCommandConfig getConfig() {
        return config;
    }

    private void loadConfig() {
        var configFile = new File(getDataFolder(), "config.json");
        if (!configFile.exists()) {
            config = new OpenCommandConfig();
            try (var file = new FileWriter(configFile)) {
                file.write(Grasscutter.getGsonFactory().toJson(config));
            } catch (IOException e) {
                getLogger().error("[OpenCommand] Unable to write to config file.");
            } catch (Exception e) {
                getLogger().error("[OpenCommand] Unable to save config file.");
            }
        } else {
            try (var file = new FileReader(configFile)) {
                config = Grasscutter.getGsonFactory().fromJson(file, OpenCommandConfig.class);
            } catch (Exception exception) {
                config = new OpenCommandConfig();
                getLogger().error("[OpenCommand] There was an error while trying to load the configuration from config.json. Please make sure that there are no syntax errors. If you want to start with a default configuration, delete your existing config.json.");
            }
        }
        // 启动Socket
        startSocket();
    }

    private void startSocket() {
        if (Grasscutter.getConfig().server.runMode == Grasscutter.ServerRunMode.GAME_ONLY) {
            getLogger().info("[OpenCommand] Starting socket client...");
            SocketClient.connectServer();
        } else if (Grasscutter.getConfig().server.runMode == Grasscutter.ServerRunMode.DISPATCH_ONLY) {
            getLogger().info("[OpenCommand] Starting socket server...");
            try {
                SocketServer.startServer();
            } catch (IOException e) {
                getLogger().error("[OpenCommand] Unable to start socket server.", e);
            }
        }
    }
}
