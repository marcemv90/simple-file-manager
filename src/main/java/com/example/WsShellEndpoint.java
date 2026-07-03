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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@ServerEndpoint("/ws-shell")
public class WsShellEndpoint {

    private Process process;
    private InputStream ptyOut;
    private OutputStream ptyIn;
    private Thread readerThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private boolean isPtyMode = false;

    @OnOpen
    public void onOpen(Session session) {
        try {
            java.io.File rcFile = java.io.File.createTempFile(".sfm_bashrc_", ".sh");
            String rcContent = "trap - INT TERM QUIT HUP\n" +
                               "stty sane\n" +
                               "if [ -f ~/.bash_profile ]; then source ~/.bash_profile; elif [ -f ~/.profile ]; then source ~/.profile; fi\n" +
                               "if [ -f ~/.bashrc ]; then source ~/.bashrc; fi\n" +
                               "trap - INT TERM QUIT HUP\n" +
                               "stty sane\n" +
                               "export PS1='\\u@\\h:\\w\\$ '\n" +
                               "function get() { if [ -z \"$1\" ]; then echo 'Usage: get <file or folder> [file2...]'; else for f in \"$@\"; do echo -e \"\\033[36mDownloading $f...\\033[0m\"; echo -ne \"\\033]8888;DOWNLOAD;$PWD/$f\\007\"; done; fi; }\n" +
                               "function put() { if [ \"$1\" == \"-d\" ]; then echo -ne \"\\033]8888;UPLOAD_DIR;$PWD\\007\"; elif [ -z \"$1\" ]; then echo -ne \"\\033]8888;UPLOAD;$PWD\\007\"; else echo 'Usage: put [-d] (Use -d to upload a whole directory)'; fi; }\n" +
                               "PROMPT_COMMAND=\"${PROMPT_COMMAND:+$PROMPT_COMMAND;}echo -ne \\\"\\\\033]8889;CWD;\\$PWD\\\\007\\\"\"\n" +
                               "rm -f " + rcFile.getAbsolutePath() + "\n";
            java.nio.file.Files.write(rcFile.toPath(), rcContent.getBytes(StandardCharsets.UTF_8));

            String os = System.getProperty("os.name").toLowerCase();
            String[] cmd;
            if (os.contains("mac")) {
                cmd = new String[] { "script", "-q", "/dev/null", "/bin/bash", "--rcfile", rcFile.getAbsolutePath(), "-i" };
            } else {
                cmd = new String[] { "script", "-q", "-c", "/bin/bash --rcfile " + rcFile.getAbsolutePath() + " -i", "/dev/null" };
            }

            Map<String, String> env = new HashMap<>(System.getenv());
            env.putIfAbsent("TERM", "xterm-256color");
            env.putIfAbsent("SHELL", "/bin/bash");

            // Always use standard java.lang.Process to respect JVM launchMechanism property and avoid native fork memory errors
            isPtyMode = false;
            Map<String, List<String>> params = session.getRequestParameterMap();
            List<String> cwdList = params != null ? params.get("cwd") : null;
            java.io.File workingDir = null;
            if (cwdList != null && !cwdList.isEmpty()) {
                String cwd = cwdList.get(0);
                if (cwd != null && !cwd.isEmpty()) {
                    workingDir = new java.io.File(cwd);
                }
            }
            
            process = startProcessWithSignalReset(cmd, env, workingDir);
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
                    if (!isPtyMode) {
                        text = text.replaceAll("(?<!\\r)\\n", "\r\n");
                    }
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
                        if (process instanceof PtyProcess) {
                            ((PtyProcess) process).setWinSize(new WinSize(cols, rows));
                        } else {
                            resizeTty(process, cols, rows);
                        }
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

    private void resizeTty(Process process, int cols, int rows) {
        if (applyResize(process, cols, rows)) {
            return;
        }
        new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(100);
                    if (applyResize(process, cols, rows)) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private boolean applyResize(Process process, int cols, int rows) {
        try {
            long pid = process.pid();
            System.out.println("[WsShellEndpoint] applyResize for main pid " + pid + " to " + cols + "x" + rows);
            if (tryResize(pid, cols, rows)) {
                System.out.println("[WsShellEndpoint] applyResize succeeded on main pid " + pid);
                return true;
            }
            java.util.concurrent.atomic.AtomicBoolean success = new java.util.concurrent.atomic.AtomicBoolean(false);
            process.descendants().forEach(handle -> {
                if (!success.get()) {
                    long dpid = handle.pid();
                    if (tryResize(dpid, cols, rows)) {
                        System.out.println("[WsShellEndpoint] applyResize succeeded on descendant pid " + dpid);
                        success.set(true);
                    }
                }
            });
            if (!success.get()) {
                System.out.println("[WsShellEndpoint] applyResize failed on main pid and all descendants");
            }
            return success.get();
        } catch (Exception e) {
            System.out.println("[WsShellEndpoint] applyResize exception: " + e.getMessage());
        }
        return false;
    }

    private boolean tryResize(long pid, int cols, int rows) {
        try {
            // Linux /proc filesystem check (does not require ps command)
            java.io.File fdFile = new java.io.File("/proc/" + pid + "/fd/0");
            if (fdFile.exists()) {
                String ptyPath = fdFile.getCanonicalPath();
                if (ptyPath != null && ptyPath.startsWith("/dev/pts/")) {
                    String[] sttyCmd = { "stty", "-F", ptyPath, "cols", String.valueOf(cols), "rows", String.valueOf(rows) };
                    System.out.println("[WsShellEndpoint] tryResize /proc pid " + pid + " tty " + ptyPath + " running: " + java.util.Arrays.toString(sttyCmd));
                    int exitCode = new ProcessBuilder(sttyCmd).start().waitFor();
                    System.out.println("[WsShellEndpoint] tryResize /proc pid " + pid + " exitCode: " + exitCode);
                    return exitCode == 0;
                }
            }
            
            // macOS / standard POSIX fallback using ps command
            String tty = getTtyOfPid(pid);
            if (tty != null && !tty.isEmpty() && !tty.equals("??") && !tty.contains("error")) {
                String ptyPath = tty.startsWith("/") ? tty : "/dev/" + tty;
                String flag = System.getProperty("os.name").toLowerCase().contains("mac") ? "-f" : "-F";
                String[] sttyCmd = { "stty", flag, ptyPath, "cols", String.valueOf(cols), "rows", String.valueOf(rows) };
                System.out.println("[WsShellEndpoint] tryResize fallback pid " + pid + " tty " + ptyPath + " running: " + java.util.Arrays.toString(sttyCmd));
                int exitCode = new ProcessBuilder(sttyCmd).start().waitFor();
                System.out.println("[WsShellEndpoint] tryResize fallback pid " + pid + " exitCode: " + exitCode);
                return exitCode == 0;
            }
        } catch (Exception e) {
            System.out.println("[WsShellEndpoint] tryResize pid " + pid + " exception: " + e.getMessage());
        }
        return false;
    }

    private String getTtyOfPid(long pid) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ps", "-o", "tty=", "-p", String.valueOf(pid));
            Process p = pb.start();
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = r.readLine();
                if (line != null) {
                    return line.trim();
                }
            }
        } catch (Exception ignored) {}
        return "error";
    }

    private Process startProcessWithSignalReset(String[] cmd, Map<String, String> env, java.io.File workingDir) throws IOException {
        // Try perl wrapper first
        try {
            String[] perlCmd = new String[cmd.length + 4];
            perlCmd[0] = "perl";
            perlCmd[1] = "-e";
            perlCmd[2] = "$SIG{INT}='DEFAULT'; $SIG{TERM}='DEFAULT'; $SIG{QUIT}='DEFAULT'; $SIG{HUP}='DEFAULT'; exec @ARGV";
            perlCmd[3] = "--";
            System.arraycopy(cmd, 0, perlCmd, 4, cmd.length);
            
            ProcessBuilder pb = new ProcessBuilder(perlCmd);
            pb.environment().putAll(env);
            if (workingDir != null) {
                pb.directory(workingDir);
            }
            pb.redirectErrorStream(true);
            return pb.start();
        } catch (IOException e) {
            // Perl not available, try python wrapper
            try {
                String[] pyCmd = new String[cmd.length + 4];
                pyCmd[0] = "python";
                pyCmd[1] = "-c";
                pyCmd[2] = "import signal, os, sys; signal.signal(signal.SIGINT, signal.SIG_DFL); signal.signal(signal.SIGTERM, signal.SIG_DFL); signal.signal(signal.SIGQUIT, signal.SIG_DFL); signal.signal(signal.SIGHUP, signal.SIG_DFL); os.execvp(sys.argv[1], sys.argv[1:])";
                pyCmd[3] = "script";
                System.arraycopy(cmd, 0, pyCmd, 4, cmd.length);
                
                ProcessBuilder pb = new ProcessBuilder(pyCmd);
                pb.environment().putAll(env);
                if (workingDir != null) {
                    pb.directory(workingDir);
                }
                pb.redirectErrorStream(true);
                return pb.start();
            } catch (IOException e2) {
                // Try python3 wrapper
                try {
                    String[] pyCmd = new String[cmd.length + 4];
                    pyCmd[0] = "python3";
                    pyCmd[1] = "-c";
                    pyCmd[2] = "import signal, os, sys; signal.signal(signal.SIGINT, signal.SIG_DFL); signal.signal(signal.SIGTERM, signal.SIG_DFL); signal.signal(signal.SIGQUIT, signal.SIG_DFL); signal.signal(signal.SIGHUP, signal.SIG_DFL); os.execvp(sys.argv[1], sys.argv[1:])";
                    pyCmd[3] = "script";
                    System.arraycopy(cmd, 0, pyCmd, 4, cmd.length);
                    
                    ProcessBuilder pb = new ProcessBuilder(pyCmd);
                    pb.environment().putAll(env);
                    if (workingDir != null) {
                        pb.directory(workingDir);
                    }
                    pb.redirectErrorStream(true);
                    return pb.start();
                } catch (IOException e3) {
                    // Fall back to direct execution
                    ProcessBuilder pb = new ProcessBuilder(cmd);
                    pb.environment().putAll(env);
                    if (workingDir != null) {
                        pb.directory(workingDir);
                    }
                    pb.redirectErrorStream(true);
                    return pb.start();
                }
            }
        }
    }
}
