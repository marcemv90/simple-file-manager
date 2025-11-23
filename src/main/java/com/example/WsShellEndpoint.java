package com.example;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@ServerEndpoint("/ws-shell")
public class WsShellEndpoint {

    private PtyProcess process;
    private InputStream ptyOut;
    private OutputStream ptyIn;
    private Thread readerThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @OnOpen
    public void onOpen(Session session) {
        try {
            // Start an interactive login bash so that readline, history, and
            // completion are enabled just like in a normal terminal.
            String[] cmd = { "/bin/bash", "-il" };
            Map<String, String> env = new HashMap<>(System.getenv());
            env.putIfAbsent("TERM", "xterm-256color");
            env.putIfAbsent("SHELL", "/bin/bash");

            process = new PtyProcessBuilder()
                    .setCommand(cmd)
                    .setEnvironment(env)
                    .start();
            ptyOut = process.getInputStream();
            ptyIn = process.getOutputStream();
            running.set(true);

            readerThread = new Thread(() -> readLoop(session));
            readerThread.setDaemon(true);
            readerThread.start();
        } catch (IOException e) {
            try {
                session.close();
            } catch (IOException ignored) { }
        }
    }

    private void readLoop(Session session) {
        byte[] buffer = new byte[4096];
        try {
            while (running.get() && session.isOpen()) {
                int len = ptyOut.read(buffer);
                if (len == -1) {
                    break;
                }
                if (len > 0) {
                    String text = new String(buffer, 0, len, StandardCharsets.UTF_8);
                    synchronized (session) {
                        if (session.isOpen()) {
                            session.getBasicRemote().sendText(text);
                        }
                    }
                }
            }
        } catch (IOException ignored) {
            // connection or process closed
        } finally {
            cleanup();
            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (IOException ignored) { }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        if (!running.get() || ptyIn == null) {
            return;
        }
        // Handle special resize control messages of the form: "RESIZE <cols> <rows>"
        if (message != null && message.startsWith("RESIZE ")) {
            String[] parts = message.split(" ");
            if (parts.length == 3 && process != null) {
                try {
                    int cols = Integer.parseInt(parts[1]);
                    int rows = Integer.parseInt(parts[2]);
                    if (cols > 0 && rows > 0) {
                        process.setWinSize(new WinSize(cols, rows));
                    }
                } catch (NumberFormatException ignored) {
                    // ignore invalid resize
                }
            }
            return;
        }

        try {
            ptyIn.write(message.getBytes(StandardCharsets.UTF_8));
            ptyIn.flush();
        } catch (IOException ignored) {
            // ignore, will be cleaned up by read loop
        }
    }

    @OnClose
    public void onClose(Session session) {
        running.set(false);
        cleanup();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        running.set(false);
        cleanup();
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException ignored) { }
    }

    private void cleanup() {
        if (process != null) {
            process.destroy();
            process = null;
        }
        if (ptyOut != null) {
            try { ptyOut.close(); } catch (IOException ignored) { }
            ptyOut = null;
        }
        if (ptyIn != null) {
            try { ptyIn.close(); } catch (IOException ignored) { }
            ptyIn = null;
        }
        if (readerThread != null && readerThread.isAlive()) {
            readerThread.interrupt();
        }
    }
}
