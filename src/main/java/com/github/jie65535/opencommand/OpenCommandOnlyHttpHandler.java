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
import com.github.jie65535.opencommand.model.Client;
import com.github.jie65535.opencommand.socket.SocketData;
import com.github.jie65535.opencommand.socket.SocketDataWait;
import com.github.jie65535.opencommand.socket.SocketServer;
import com.github.jie65535.opencommand.socket.packet.HttpPacket;
import com.github.jie65535.opencommand.socket.packet.RunConsoleCommand;
import com.github.jie65535.opencommand.socket.packet.player.Player;
import com.github.jie65535.opencommand.socket.packet.player.PlayerEnum;
import emu.grasscutter.server.http.Router;
import emu.grasscutter.utils.Crypto;
import emu.grasscutter.utils.Utils;
import io.javalin.Javalin;
import io.javalin.http.Context;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class OpenCommandOnlyHttpHandler implements Router {

    @Override
    public void applyRoutes(Javalin javalin) {
        javalin.post("/opencommand/api", OpenCommandOnlyHttpHandler::handle);
    }

    private static final Map<String, Integer> codes = new HashMap<>();
    private static final Int2ObjectMap<Date> codeExpireTime = new Int2ObjectOpenHashMap<>();

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
                var player = SocketData.getPlayer(playerId);
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
                        token = Utils.bytesToHex(Crypto.createSessionKey(32));
                    int code = Utils.randomRange(1000, 9999);
                    codeExpireTime.put(playerId, new Date(now.getTime() + config.codeExpirationTime_S * 1000L));
                    codes.put(token, code);
                    data.addClient(new Client(token, playerId, new Date(now.getTime() + config.tempTokenExpirationTime_S * 1000L)));
                    Player.dropMessage(playerId, "[Open Command] Verification code: " + code);
                    context.json(new JsonResponse(token));
                }
                return;
            } else if (req.action.equals("ping")) {
                context.json(new JsonResponse());
                return;
            } else if (req.action.equals("online")) {
                context.json(new JsonResponse(200, "Success", SocketData.getOnlinePlayer()));
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
                    var server = SocketServer.getClientInfoByUuid(req.server);
                    if (server == null) {
                        context.json(new JsonResponse(404, "Server Not Found."));
                        return;
                    }
                    plugin.getLogger().info(String.format("IP: %s run command in console > %s", context.ip(), req.data));
                    var wait = new SocketDataWait<HttpPacket>(2000L) {
                        @Override
                        public void run() {
                        }

                        @Override
                        public HttpPacket initData(HttpPacket data) {
                            return data;
                        }

                        @Override
                        public void timeout() {
                        }
                    };

                    SocketServer.sendPacketAndWait(server.ip, new RunConsoleCommand(req.data.toString()), wait);
                    var packet = wait.getData();
                    if (packet == null) {
                        context.json(new JsonResponse(408, "Timeout"));
                        return;
                    }
                    context.json(new JsonResponse(packet.code, packet.message, packet.data));
                    return;
                } else if (req.action.equals("server")) {
                    context.json(new JsonResponse(200, "Success", SocketServer.getOnlineClient()));
                    return;
                } else if (req.action.equals("runmode")) {
                    context.json(new JsonResponse(200, "Success", 1));
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
                    SocketDataWait<HttpPacket> socketDataWait = new SocketDataWait<>(1000L * 10L) {
                        @Override
                        public void run() {
                        }

                        @Override
                        public HttpPacket initData(HttpPacket data) {
                            return data;
                        }

                        @Override
                        public void timeout() {
                        }
                    };

                    // update token expire time
                    client.tokenExpireTime = new Date(now.getTime() + config.tokenLastUseExpirationTime_H * 60L * 60L * 1000L);
                    var command = req.data.toString();
                    var player = new Player();
                    player.uid = client.playerId;
                    player.type = PlayerEnum.RunCommand;
                    player.data = command;

                    if (!SocketServer.sendUidPacketAndWait(client.playerId, player, socketDataWait)) {
                        context.json(new JsonResponse(404, "Player Not Found."));
                        return;
                    }


                    HttpPacket httpPacket = socketDataWait.getData();
                    if (httpPacket == null) {
                        context.json(new JsonResponse(500, "error", "Wait timeout"));
                        return;
                    }
                    context.json(new JsonResponse(httpPacket.code, httpPacket.message));
                    return;
                }
            }
            context.json(new JsonResponse(403, "forbidden"));
        } catch (Exception ex) {
            plugin.getLogger().error("[OpenCommand] handler error.", ex);
        }
    }

    private static void cleanupExpiredCodes() {
        var now = new Date();
        codeExpireTime.int2ObjectEntrySet().removeIf(entry -> entry.getValue().before(now));
        if (codeExpireTime.isEmpty())
            codes.clear();
    }
}
