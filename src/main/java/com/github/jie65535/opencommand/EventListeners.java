package com.github.jie65535.opencommand;

import emu.grasscutter.server.event.game.ReceiveCommandFeedbackEvent;
import emu.grasscutter.utils.MessageHandler;

public final class EventListeners {

    private static MessageHandler consoleMessageHandler;
    public static void setConsoleMessageHandler(MessageHandler handler) {
        consoleMessageHandler = handler;
    }
    public static void onCommandResponse(ReceiveCommandFeedbackEvent event) {
        if (consoleMessageHandler != null && event.getPlayer() == null) {
            consoleMessageHandler.setMessage(event.getMessage());
        }
    }
}
