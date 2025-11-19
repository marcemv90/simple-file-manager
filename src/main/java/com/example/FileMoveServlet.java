package com.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/move")
public class FileMoveServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getParameter("path");
        String targetDirPath = request.getParameter("targetDir");

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        if (path == null || path.isEmpty()) {
            out.write("{\"status\":\"error\",\"message\":\"No file or folder specified.\"}");
            out.flush();
            return;
        }

        if (targetDirPath == null || targetDirPath.trim().isEmpty()) {
            out.write("{\"status\":\"error\",\"message\":\"Target directory must not be empty.\"}");
            out.flush();
            return;
        }

        targetDirPath = targetDirPath.trim();

        File source = new File(path);
        if (!source.exists()) {
            out.write("{\"status\":\"error\",\"message\":\"Source file or folder does not exist.\"}");
            out.flush();
            return;
        }

        File targetDir = new File(targetDirPath);
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            out.write("{\"status\":\"error\",\"message\":\"Target directory does not exist or is not a directory.\"}");
            out.flush();
            return;
        }

        File target = new File(targetDir, source.getName());
        if (target.exists()) {
            out.write("{\"status\":\"error\",\"message\":\"A file or folder with the same name already exists in the target directory.\"}");
            out.flush();
            return;
        }

        boolean moved = source.renameTo(target);
        if (moved) {
            out.write("{\"status\":\"success\",\"message\":\"Moved successfully.\"}");
        } else {
            out.write("{\"status\":\"error\",\"message\":\"Failed to move the file or folder.\"}");
        }

        out.flush();
    }
}
