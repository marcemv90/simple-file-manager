package com.example;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;

import java.util.HashMap;

public class TestPty {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting PTY...");
        String[] cmd = { "/bin/bash", "-c", "echo hello" };
        PtyProcess process = new PtyProcessBuilder()
                .setCommand(cmd)
                .setEnvironment(new HashMap<>())
                .start();
        
        int exitCode = process.waitFor();
        System.out.println("Exited with " + exitCode);
    }
}
