package com.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/mkdir")
public class DirectoryCreateServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String currentPath = request.getParameter("currentPath");
        String folderName = request.getParameter("folderName");

        if (currentPath == null || currentPath.isEmpty()) {
            currentPath = "/tmp";
        }

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        if (folderName == null || folderName.trim().isEmpty()) {
            out.write("{\"status\":\"error\",\"message\":\"Folder name must not be empty.\"}");
            out.flush();
            return;
        }

        folderName = folderName.trim();

        // Basic validation to prevent path traversal within the name
        if (folderName.contains("/") || folderName.contains("\\")) {
            out.write("{\"status\":\"error\",\"message\":\"Folder name must not contain path separators.\"}");
            out.flush();
            return;
        }

        File parentDir = new File(currentPath);
        File newDir = new File(parentDir, folderName);

        if (newDir.exists()) {
            if (newDir.isDirectory()) {
                out.write("{\"status\":\"error\",\"message\":\"The specified directory already exists.\"}");
            } else {
                out.write("{\"status\":\"error\",\"message\":\"A file with the same name already exists.\"}");
            }
            out.flush();
            return;
        }

        if (newDir.mkdirs()) {
            out.write("{\"status\":\"success\",\"message\":\"Directory created successfully.\"}");
        } else {
            out.write("{\"status\":\"error\",\"message\":\"Failed to create directory.\"}");
        }

        out.flush();
    }
}
