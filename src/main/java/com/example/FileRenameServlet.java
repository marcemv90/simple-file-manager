package com.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/rename")
public class FileRenameServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getParameter("path");
        String newName = request.getParameter("newName");

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        if (path == null || path.isEmpty()) {
            out.write("{\"status\":\"error\",\"message\":\"No file or folder specified.\"}");
            out.flush();
            return;
        }

        if (newName == null || newName.trim().isEmpty()) {
            out.write("{\"status\":\"error\",\"message\":\"New name must not be empty.\"}");
            out.flush();
            return;
        }

        newName = newName.trim();

        File source = new File(path);
        if (!source.exists()) {
            out.write("{\"status\":\"error\",\"message\":\"Source file or folder does not exist.\"}");
            out.flush();
            return;
        }

        File parent = source.getParentFile();
        if (parent == null) {
            out.write("{\"status\":\"error\",\"message\":\"Cannot rename root path.\"}");
            out.flush();
            return;
        }

        File target = new File(parent, newName);
        if (target.exists()) {
            out.write("{\"status\":\"error\",\"message\":\"A file or folder with the new name already exists.\"}");
            out.flush();
            return;
        }

        boolean renamed = source.renameTo(target);
        if (renamed) {
            out.write("{\"status\":\"success\",\"message\":\"Renamed successfully.\"}");
        } else {
            out.write("{\"status\":\"error\",\"message\":\"Failed to rename the file or folder.\"}");
        }

        out.flush();
    }
}
