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

import com.github.jie65535.opencommand.json.JsonRequest;
import com.github.jie65535.opencommand.json.JsonResponse;
import emu.grasscutter.command.CommandMap;
import emu.grasscutter.server.http.Router;
import emu.grasscutter.utils.Crypto;
import emu.grasscutter.utils.MessageHandler;
import emu.grasscutter.utils.Utils;
import express.Express;
import express.http.Request;
import express.http.Response;
import io.javalin.Javalin;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class OpenCommandHandler implements Router {

    @Override
    public void applyRoutes(Express express, Javalin javalin) {
        express.post("/opencommand/api", OpenCommandHandler::handle);
    }

    private static final Map<String, Integer> clients = new HashMap<>();
    private static final Map<String, Date> tokenExpireTime = new HashMap<>();
    private static final Map<String, Integer> codes = new HashMap<>();
    private static final Int2ObjectMap<Date> codeExpireTime = new Int2ObjectOpenHashMap<>();

    public static void handle(Request request, Response response) {
        // Trigger cleanup action
        cleanupExpiredData();
        var plugin = OpenCommandPlugin.getInstance();
        var config = plugin.getConfig();
        var now = new Date();

        var req = request.body(JsonRequest.class);
        response.type("application/json");
        if (req.action.equals("sendCode")) {
            int playerId = (int) req.data;
            var player = plugin.getServer().getPlayerByUid(playerId);
            if (player == null) {
                response.json(new JsonResponse(404, "Player Not Found."));
            } else {
                if (codeExpireTime.containsKey(playerId)) {
                    var expireTime = codeExpireTime.get(playerId);
                    if (now.before(expireTime)) {
                        response.json(new JsonResponse(403, "Requests are too frequent"));
                        return;
                    }
                }

                String token = req.token;
                if (token == null || token.isEmpty())
                    token = Utils.bytesToHex(Crypto.createSessionKey(32));
                int code = Utils.randomRange(1000, 9999);
                codeExpireTime.put(playerId, new Date(now.getTime() + config.codeExpirationTime_S * 1000L));
                tokenExpireTime.put(token, new Date(now.getTime() + config.tempTokenExpirationTime_S * 1000L));
                codes.put(token, code);
                clients.put(token, playerId);
                player.dropMessage("[Open Command] Verification code: " + code);
                response.json(new JsonResponse(token));
            }
            return;
        } else if (req.action.equals("ping")) {
            response.json(new JsonResponse());
            return;
        }

        // token is required
        if (req.token == null || req.token.isEmpty()) {
            response.json(new JsonResponse(401, "Unauthorized"));
            return;
        }
        var isConsole = req.token.equals(config.consoleToken);
        if (!isConsole && !clients.containsKey(req.token)) {
            response.json(new JsonResponse(401, "Unauthorized"));
            return;
        }

        if (isConsole) {
            if (req.action.equals("verify")) {
                response.json(new JsonResponse());
                return;
            } else if (req.action.equals("command")) {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (plugin) {
                    try {
                        plugin.getLogger().info(String.format("IP: %s run command in console > %s", request.ip(), req.data));
                        var resultCollector = new MessageHandler();
                        EventListeners.setConsoleMessageHandler(resultCollector);
                        CommandMap.getInstance().invoke(null, null, req.data.toString());
                        response.json(new JsonResponse(resultCollector.getMessage()));
                    } catch (Exception e) {
                        plugin.getLogger().warn("Run command failed.", e);
                        EventListeners.setConsoleMessageHandler(null);
                        response.json(new JsonResponse(500, "error", e.getLocalizedMessage()));
                    }
                }
                return;
            }
        } else if (codes.containsKey(req.token)) {
            if (req.action.equals("verify")) {
                if (codes.get(req.token).equals(req.data)) {
                    codes.remove(req.token);
                    // update token expire time
                    tokenExpireTime.put(req.token, new Date(now.getTime() + config.tokenLastUseExpirationTime_H * 60L * 60L * 1000L));
                    response.json(new JsonResponse());
                    plugin.getLogger().info(String.format("Player %d has passed the verification, ip: %s", clients.get(req.token), request.ip()));
                } else {
                    response.json(new JsonResponse(400, "Verification failed"));
                }
                return;
            }
        } else {
            if (req.action.equals("command")) {
                // update token expire time
                tokenExpireTime.put(req.token, new Date(now.getTime() + config.tokenLastUseExpirationTime_H * 60L * 60L * 1000L));
                var playerId = clients.get(req.token);
                var player = plugin.getServer().getPlayerByUid(playerId);
                var command = req.data.toString();
                if (player == null) {
                    response.json(new JsonResponse(404, "Player not found"));
                    return;
                }
                // Player MessageHandler do not support concurrency
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (player) {
                    try {
                        var resultCollector = new MessageHandler();
                        player.setMessageHandler(resultCollector);
                        CommandMap.getInstance().invoke(player, player, command);
                        response.json(new JsonResponse(resultCollector.getMessage()));
                    } catch (Exception e) {
                        plugin.getLogger().warn("Run command failed.", e);
                        response.json(new JsonResponse(500, "error", e.getLocalizedMessage()));
                    } finally {
                        player.setMessageHandler(null);
                    }
                }
                return;
            }
        }
        response.json(new JsonResponse(403, "forbidden"));
    }

    private static void cleanupExpiredData() {
        var now = new Date();
        codeExpireTime.int2ObjectEntrySet().removeIf(entry -> entry.getValue().before(now));

        var it = tokenExpireTime.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getValue().before(now)) {
                it.remove();
                // remove expired token
                clients.remove(entry.getKey());
            }
        }
    }
}
