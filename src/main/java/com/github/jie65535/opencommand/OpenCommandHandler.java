package com.github.jie65535.opencommand;

import com.github.jie65535.opencommand.json.JsonRequest;
import com.github.jie65535.opencommand.json.JsonResponse;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.command.CommandMap;
import emu.grasscutter.utils.Crypto;
import emu.grasscutter.utils.MessageHandler;
import emu.grasscutter.utils.Utils;
import express.http.HttpContextHandler;
import express.http.Request;
import express.http.Response;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class OpenCommandHandler implements HttpContextHandler {

    private static final Map<String, Integer> clients = new HashMap<>();
    private static final Map<String, Date> tokenExpireTime = new HashMap<>();
    private static final Map<String, Integer> codes = new HashMap<>();
    private static final Int2ObjectMap<Date> codeExpireTime = new Int2ObjectOpenHashMap<>();

    @Override
    public void handle(Request request, Response response) throws IOException {
        // Trigger cleanup action
        cleanupExpiredData();
        var now = new Date();

        var req = request.body(JsonRequest.class);
        response.type("application/json");
        if (req.action.equals("sendCode")) {
            int playerId = (int) req.data;
            var player = Grasscutter.getGameServer().getPlayerByUid(playerId);
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
                codeExpireTime.put(playerId, new Date(now.getTime() + 60 * 1000));
                tokenExpireTime.put(token, new Date(now.getTime() + 5 * 60 * 1000));
                codes.put(token, code);
                clients.put(token, playerId);
                player.dropMessage("[Open Command] Verification code: {code}".replace("{code}", Integer.toString(code)));

                response.json(new JsonResponse(token));
                return;
            }
        } else if (req.action.equals("ping")) {
            response.json(new JsonResponse());
            return;
        }

        // token is required
        if (!clients.containsKey(req.token)) {
            response.json(new JsonResponse(401, "Unauthorized"));
            return;
        }
        if (codes.containsKey(req.token)) {
            if (req.action.equals("verify")) {
                if (codes.get(req.token).equals(req.data)) {
                    codes.remove(req.token);
                    // update token expire time
                    tokenExpireTime.put(req.token, new Date(now.getTime() + 60 * 60 * 1000));
                    response.json(new JsonResponse());
                } else {
                    response.json(new JsonResponse(400, "Verification failed"));
                }
                return;
            }
        } else {
            if (req.action.equals("command")) {
                // update token expire time
                tokenExpireTime.put(req.token, new Date(now.getTime() + 60 * 60 * 1000));
                var playerId = clients.get(req.token);
                var player = Grasscutter.getGameServer().getPlayerByUid(playerId);
                var command = req.data.toString();
                try {
                    var resultCollector = new MessageHandler();
                    player.setMessageHandler(resultCollector);
                    CommandMap.getInstance().invoke(player, player, command);
                    response.json(new JsonResponse(resultCollector.getMessage()));
                } catch (Exception e) {
                    response.json(new JsonResponse(500, "error", e.getLocalizedMessage()));
                } finally {
                    player.setMessageHandler(null);
                }
                return;
            }
        }
        response.json(new JsonResponse(403, "forbidden"));
    }

    private void cleanupExpiredData() {
        var now = new Date();
        codeExpireTime.int2ObjectEntrySet().removeIf(entry -> entry.getValue().before(now));
        tokenExpireTime.entrySet().removeIf(entry -> entry.getValue().before(now));
    }
}
