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

import com.github.jie65535.opencommand.json.JsonRequest;
import com.github.jie65535.opencommand.json.JsonResponse;
import com.github.jie65535.opencommand.model.Client;
import emu.lunarcore.LunarCore;
import emu.lunarcore.game.player.Player;
import emu.lunarcore.util.Crypto;
import io.javalin.http.Context;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class OpenCommandHandler {

    private static final Map<String, Integer> codes = new HashMap<>();
    private static final Int2ObjectMap<Date> codeExpireTime = new Int2ObjectOpenHashMap<>();

    private static final SecureRandom secureRandom = new SecureRandom();

    public static void handle(Context context) {
        var plugin = OpenCommandPlugin.getInstance();
        try {
            var config = plugin.getConfig();
            var data = plugin.getData();
            var now = new Date();
            // Trigger cleanup action
            cleanupExpiredCodes();
            data.removeExpiredClients();

            var req = context.bodyAsClass(JsonRequest.class);
            if (req.action.equals("sendCode")) {
                int playerId = (int)Double.parseDouble(req.data.toString());
                var player = LunarCore.getGameServer().getPlayerByUid(playerId, false);
                if (player == null) {
                    context.json(new JsonResponse(404, "Player Not Found."));
                } else {
                    if (codeExpireTime.containsKey(playerId)) {
                        var expireTime = codeExpireTime.get(playerId);
                        if (now.before(expireTime)) {
                            context.json(new JsonResponse(403, "Requests are too frequent"));
                            return;
                        }
                    }

                    String token = req.token;
                    if (token == null || token.isEmpty())
                        token = Crypto.createSessionKey(player.getAccountUid());
                    int code = secureRandom.nextInt(1000, 9999);
                    codeExpireTime.put(playerId, new Date(now.getTime() + config.codeExpirationTime_S * 1000L));
                    codes.put(token, code);
                    data.addClient(new Client(token, playerId, new Date(now.getTime() + config.tempTokenExpirationTime_S * 1000L)));
                    player.sendMessage("[Open Command] Verification code: " + code);
                    context.json(new JsonResponse(token));
                }
                return;
            } else if (req.action.equals("ping")) {
                context.json(new JsonResponse(plugin.getVersion()));
                return;
            }

            // token is required
            if (req.token == null || req.token.isEmpty()) {
                context.json(new JsonResponse(401, "Unauthorized"));
                return;
            }
            var isConsole = req.token.equals(config.consoleToken);
            var client = data.getClientByToken(req.token);
            if (!isConsole && client == null) {
                context.json(new JsonResponse(401, "Unauthorized"));
                return;
            }

            if (isConsole) {
                if (req.action.equals("verify")) {
                    context.json(new JsonResponse());
                    return;
                } else if (req.action.equals("command")) {
                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (plugin) {
                        try {
                            plugin.getLogger().info(String.format("IP: %s run command in console > %s", context.ip(), req.data));
                            tryInvokeCommand(null, req.data.toString());
                            context.json(new JsonResponse("OK"));
                        } catch (Exception e) {
                            plugin.getLogger().warn("Run command failed.", e);
                            context.json(new JsonResponse(500, "error", e.getLocalizedMessage()));
                        }
                    }
                    return;
                } else if (req.action.equals("runmode")) {
                    context.json(new JsonResponse(200, "Success", 0));
                    return;
                }
            } else if (codes.containsKey(req.token)) {
                if (req.action.equals("verify")) {
                    if (codes.get(req.token) == (int)Double.parseDouble(req.data.toString())) {
                        codes.remove(req.token);
                        // update token expire time
                        client.tokenExpireTime = new Date(now.getTime() + config.tokenLastUseExpirationTime_H * 60L * 60L * 1000L);
                        context.json(new JsonResponse());
                        plugin.getLogger().info(String.format("Player %d has passed the verification, ip: %s", client.playerId, context.ip()));
                        plugin.saveData();
                    } else {
                        context.json(new JsonResponse(400, "Verification failed"));
                    }
                    return;
                }
            } else {
                if (req.action.equals("command")) {
                    // update token expire time
                    client.tokenExpireTime = new Date(now.getTime() + config.tokenLastUseExpirationTime_H * 60L * 60L * 1000L);
                    var player = LunarCore.getGameServer().getPlayerByUid(client.playerId, false);
                    if (player == null) {
                        context.json(new JsonResponse(404, "Player not found"));
                        return;
                    }

//                    var history = player.getChatManager().getHistoryByUid(GameConstants.SERVER_CONSOLE_UID);
                    try {
                        tryInvokeCommand(player, req.data.toString());
                        context.json(new JsonResponse("OK"));
                    } catch (Exception e) {
                        plugin.getLogger().warn("Run command failed.", e);
                        context.json(new JsonResponse(500, "error", e.getLocalizedMessage()));
                    }
                    return;
                }
            }
            context.json(new JsonResponse(403, "forbidden"));
        } catch (Throwable ex) {
            plugin.getLogger().error("[OpenCommand] handler error.", ex);
        }
    }

    private static void tryInvokeCommand(Player sender, String rawMessage) {
        for (var command : rawMessage.split("\n[/!]|\\|")) {
            command = command.trim();
            if (command.isEmpty()) continue;
            if (command.charAt(0) == '/' || command.charAt(0) == '!') {
                command = command.substring(1);
            }
            LunarCore.getCommandManager().invoke(sender, command);
        }
    }

    private static void cleanupExpiredCodes() {
        var now = new Date();
        codeExpireTime.int2ObjectEntrySet().removeIf(entry -> entry.getValue().before(now));
        if (codeExpireTime.isEmpty())
            codes.clear();
    }
}
