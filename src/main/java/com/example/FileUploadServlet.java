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
    
    // Helper method to get the submitted file name
    private String getSubmittedFileName(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                return fileName.substring(fileName.lastIndexOf('/') + 1)
                             .substring(fileName.lastIndexOf('\\') + 1); // MSIE fix
            }
        }
        return null;
    }
}