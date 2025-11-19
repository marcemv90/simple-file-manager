package com.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/delete")
public class FileDeleteServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String filePath = request.getParameter("file");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // Validate the file path
        if (filePath == null || filePath.isEmpty()) {
            out.write("{\"status\":\"error\",\"message\":\"No file specified.\"}");
            out.flush();
            return;
        }

        File file = new File(filePath);

        // Ensure the target exists
        if (!file.exists()) {
            out.write("{\"status\":\"error\",\"message\":\"File or folder not found.\"}");
            out.flush();
            return;
        }

        boolean deleted = deleteRecursively(file);
        if (deleted) {
            if (file.isDirectory()) {
                out.write("{\"status\":\"success\",\"message\":\"Folder deleted successfully.\"}");
            } else {
                out.write("{\"status\":\"success\",\"message\":\"File deleted successfully.\"}");
            }
        } else {
            out.write("{\"status\":\"error\",\"message\":\"Failed to delete the file or folder.\"}");
        }

        out.flush();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Redirect to doPost for handling delete requests via GET (if needed)
        doPost(request, response);
    }

    // Recursively delete files and directories
    private boolean deleteRecursively(File target) {
        if (target.isDirectory()) {
            File[] contents = target.listFiles();
            if (contents != null) {
                for (File child : contents) {
                    if (!deleteRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        return target.delete();
    }
}
