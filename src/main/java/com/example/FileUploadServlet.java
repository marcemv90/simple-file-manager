package com.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

@WebServlet("/upload")
@MultipartConfig
public class FileUploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String checkOnlyParam = request.getParameter("checkOnly");
        boolean checkOnly = checkOnlyParam != null && checkOnlyParam.equalsIgnoreCase("true");
        if (checkOnly) {
            String currentPath = request.getParameter("currentPath");
            java.util.List<String> filenames = new java.util.ArrayList<>();
            try {
                for (Part part : request.getParts()) {
                    if (part.getName().equals("currentPath") && (currentPath == null || currentPath.isEmpty())) {
                        try (InputStream is = part.getInputStream()) {
                            currentPath = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        }
                    } else if (part.getName().equals("filenames")) {
                        try (InputStream is = part.getInputStream()) {
                            filenames.add(new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                        }
                    }
                }
            } catch (Exception e) {
                // ignore
            }
            if (currentPath == null || currentPath.isEmpty()) {
                currentPath = "/tmp";
            }

            java.util.List<String> conflicts = new java.util.ArrayList<>();
            File uploadDir = new File(currentPath);
            for (String name : filenames) {
                if (name == null || name.isEmpty()) continue;
                File f = new File(uploadDir, name);
                if (f.exists()) {
                    conflicts.add(name);
                }
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            if (!conflicts.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("{\"status\":\"exists\",\"message\":\"Some files already exist.\",\"conflicts\":[");
                for (int i = 0; i < conflicts.size(); i++) {
                    sb.append("\"").append(conflicts.get(i).replace("\"", "\\\"")).append("\"");
                    if (i < conflicts.size() - 1) {
                        sb.append(",");
                    }
                }
                sb.append("]}");
                out.write(sb.toString());
            } else {
                out.write("{\"status\":\"none\"}");
            }
            return;
        }

        String currentPath = request.getParameter("currentPath");
        String overwriteParam = request.getParameter("overwrite");
        boolean overwrite = overwriteParam != null && overwriteParam.equalsIgnoreCase("true");
        if (currentPath == null || currentPath.isEmpty()) {
            currentPath = "/tmp";
        }
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            // Create the directory if it doesn't exist
            File uploadDir = new File(currentPath);
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (!created) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.write("{\"status\":\"error\",\"message\":\"Failed to create upload directory\"}");
                    return;
                }
            }
            
            // Process all parts
            for (Part part : request.getParts()) {
                if (part.getName().equals("files") && part.getSize() > 0) {
                    String fileName = getSubmittedFileName(part);
                    if (fileName == null || fileName.isEmpty()) {
                        continue;
                    }
                    
                    File file = new File(uploadDir, fileName);
                    
                    // Create intermediate directories if they don't exist
                    File parentDir = file.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }

                    if (file.exists() && !overwrite) {
                        // Signal to the client that the file already exists and confirmation is required
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        out.write(String.format(
                            "{\"status\":\"exists\",\"message\":\"File %s already exists.\"}",
                            fileName));
                        return;
                    }
                    try (InputStream input = part.getInputStream();
                         FileOutputStream output = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                    } catch (IOException e) {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        out.write(String.format(
                            "{\"status\":\"error\",\"message\":\"Failed to save file %s: %s\"}",
                            fileName, e.getMessage()));
                        return;
                    }
                }
            }
            
            out.write("{\"status\":\"success\"}");
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(String.format(
                "{\"status\":\"error\",\"message\":\"Upload failed: %s\"}",
                e.getMessage()));
            e.printStackTrace();
        }
    }
    
    private String getSubmittedFileName(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                // Prevent path traversal
                if (fileName.contains("..")) return null;
                // Strip absolute path prefixes if any (e.g., C:\ or /)
                fileName = fileName.replaceAll("^([A-Za-z]:)?[/\\\\\\\\]+", "");
                return fileName;
            }
        }
        return null;
    }
}