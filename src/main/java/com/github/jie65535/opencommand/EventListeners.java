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

public final class EventListeners {

//    private static StringBuilder consoleMessageHandler;
//    private static final Int2ObjectMap<StringBuilder> playerMessageHandlers = new Int2ObjectOpenHashMap<>();
//
//    public static void setConsoleMessageHandler(StringBuilder handler) {
//        consoleMessageHandler = handler;
//    }
//
//    /**
//     * 获取新的玩家消息处理类
//     * 获取时将创建或清空消息处理器并返回实例，**在执行命令前获取！**
//     * @param uid 玩家uid
//     * @return 新的玩家消息处理类
//     */
//    public static StringBuilder getPlayerMessageHandler(int uid) {
//        var handler = playerMessageHandlers.get(uid);
//        if (handler == null) {
//            handler = new StringBuilder();
//            playerMessageHandlers.put(uid, handler);
//        }
//        return handler;
//    }
//
//    /**
//     * 命令执行反馈事件处理
//     */
//    public static void onCommandResponse(ReceiveCommandFeedbackEvent event) {
//        StringBuilder handler;
//        if (event.getPlayer() == null) {
//            handler = consoleMessageHandler;
//        } else {
//            handler = playerMessageHandlers.get(event.getPlayer().getUid());
//        }
//
//        if (handler != null) {
//            if (!handler.isEmpty()) {
//                // New line
//                handler.append(System.lineSeparator());
//            }
//            handler.append(event.getMessage());
//        }
//    }
}
