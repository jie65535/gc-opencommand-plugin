/*
 * gc-opencommand
 * Copyright (C) 2022-2023 jie65535
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

import emu.lunarcore.LunarCore;
import emu.lunarcore.plugin.Plugin;
import emu.lunarcore.util.Crypto;
import emu.lunarcore.util.JsonUtils;
import org.slf4j.Logger;

import java.io.*;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class OpenCommandPlugin extends Plugin {

    private static OpenCommandPlugin instance;

    private OpenCommandPlugin(Identifier identifier, URLClassLoader classLoader, File dataFolder, Logger logger) {
        super(identifier, classLoader, dataFolder, logger);
    }

    public static OpenCommandPlugin getInstance() {
        return instance;
    }

    private OpenCommandConfig config;

    private OpenCommandData data;

    @Override
    public void onLoad() {
        instance = this;
        // 加载配置
        loadConfig();
        // 加载数据
        loadData();
    }

    @Override
    public void onEnable() {
        LunarCore.getHttpServer().getApp().post("/opencommand/api", OpenCommandHandler::handle);
        getLogger().info("[OpenCommand] Enabled. https://github.com/jie65535/gc-opencommand-plugin");
    }

    @Override
    public void onDisable() {
        saveData();
        getLogger().info("[OpenCommand] Disabled");
    }

    public OpenCommandConfig getConfig() {
        return config;
    }

    public OpenCommandData getData() {
        return data;
    }

    private void loadConfig() {
        var configFile = new File(getDataFolder(), "config.json");
        if (!configFile.exists()) {
            config = new OpenCommandConfig();
            saveConfig();
        } else {
            try {
                config = JsonUtils.decode(Files.readString(configFile.toPath(), StandardCharsets.UTF_8),
                        OpenCommandConfig.class);
            } catch (Exception exception) {
                config = new OpenCommandConfig();
                getLogger().error("[OpenCommand] There was an error while trying to load the configuration from config.json. Please make sure that there are no syntax errors. If you want to start with a default configuration, delete your existing config.json.");
            }
        }

        // 检查控制台Token
        if (config.consoleToken == null || config.consoleToken.isEmpty()) {
            config.consoleToken = Crypto.createSessionKey("1");
            saveConfig();
            getLogger().warn("Detected that consoleToken is empty, automatically generated Token for you as follows: {}", config.consoleToken);
        }
    }

    private void saveConfig() {
        var configFile = new File(getDataFolder(), "config.json");
        try (var file = new FileWriter(configFile)) {
            file.write(JsonUtils.encode(config));
        } catch (IOException e) {
            getLogger().error("[OpenCommand] Unable to write to config file.");
        } catch (Exception e) {
            getLogger().error("[OpenCommand] Unable to save config file.");
        }
    }

    private void loadData() {
        var dataFile = new File(getDataFolder(), "data.json");
        if (!dataFile.exists()) {
            data = new OpenCommandData();
            saveData();
        } else {
            try {
                data = JsonUtils.decode(Files.readString(dataFile.toPath(), StandardCharsets.UTF_8),
                        OpenCommandData.class);
            } catch (Exception exception) {
                getLogger().error("[OpenCommand] There was an error while trying to load the data from data.json. Please make sure that there are no syntax errors. If you want to start with a default data, delete your existing data.json.");
            }
            if (data == null) {
                data = new OpenCommandData();
            }
        }
    }

    public void saveData() {
        try (var file = new FileWriter(new File(getDataFolder(), "data.json"))) {
            file.write(JsonUtils.encode(data));
        } catch (IOException e) {
            getLogger().error("[OpenCommand] Unable to write to data file.");
        } catch (Exception e) {
            getLogger().error("[OpenCommand] Unable to save data file.");
        }
    }
}
