package com.ericlam.plugin.protect.gate;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

final class OpenConnect {
    private ServerSocket socket;
    private static OpenConnect instance;

    static OpenConnect getInstance(int port) throws IOException {
        if (instance == null) instance = new OpenConnect(port);
        return instance;
    }


    private OpenConnect(int port) throws IOException {
        socket = new ServerSocket(port);
    }


    Socket getSocket() {
        try {
            Socket server = this.socket.accept();
            return server;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
