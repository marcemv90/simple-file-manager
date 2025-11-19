package com.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;


@WebServlet("/")
public class FileManagerServlet extends HttpServlet {

    private static final String DEFAULT_DIR = "/tmp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String contextPath = request.getContextPath() + "/";
        String pathParam = request.getParameter("path");
        String path = (pathParam == null || pathParam.isEmpty()) ? DEFAULT_DIR : pathParam;

        File dir = new File(path);
        File[] files = dir.listFiles();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm");
        // Define the format for each line: Adjust column widths as necessary
        String format = "%-11s %-7s %-7s %-15s %-20s %s%n";

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html lang='en'>");
        out.println("<head>");
        out.println("    <meta charset='UTF-8'>");
        out.println("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("    <title>Simple File Manager</title>");
        out.println("    <!-- Material Icons -->");
        out.println("    <link href='https://fonts.googleapis.com/icon?family=Material+Icons' rel='stylesheet'>");
        out.println("    <!-- Materialize CSS -->");
        out.println("    <link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/materialize/1.0.0/css/materialize.min.css'>");
        out.println("    <!-- Custom CSS -->");
        out.println("    <link rel='stylesheet' href='/css/style.css'>");
        out.println("</head>");
        out.println("<body>");
        
        // Navigation Bar
        out.println("<nav class='light-blue darken-2'>");
        out.println("    <div class='nav-wrapper'>");
        out.println("        <a href='" + contextPath + "/' class='brand-logo' style='padding-left: 20px;'>File Manager</a>");
        out.println("    </div>");
        out.println("</nav>");
        
        // Main Content
        out.println("<main class='container'>");
        
        // Breadcrumb Navigation (no colored container, custom link colors)
        out.println("    <div style='margin: 10px 0;'>");
        out.println("        <div class='col s12'>");
        out.println("            <a href='" + contextPath + "?path=/' class='breadcrumb' style='color: #000000;'>/</a>");
        
        String[] parts = path.split("/");
        int lastIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                lastIndex = i;
            }
        }

        StringBuilder currentPath = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                currentPath.append("/").append(parts[i]);
                String color = (i == lastIndex) ? "#1976d2" : "#000000";
                out.println("            <a href='" + contextPath + "/?path=" + currentPath.toString() + "' class='breadcrumb' style='color: " + color + ";'>" + parts[i] + "</a>");
            }
        }
        
        out.println("        </div>");
        out.println("    </div>");
        
        // Upload Form (no card container to allow full width)
        out.println("    <div>");
        out.println("        <form id='uploadForm' action='upload' method='post' enctype='multipart/form-data' class='col s12'>");
        out.println("            <div class='file-field input-field'>");
        out.println("                <div class='btn light-blue darken-2'>");
        out.println("                    <span>CHOOSE FILES TO UPLOAD</span>");
        out.println("                    <input type='file' id='fileInput' name='file' multiple>");
        out.println("                </div>");
        out.println("                <div class='file-path-wrapper'>");
        out.println("                    <input class='file-path validate' type='text' placeholder='Upload one or more files'>");
        out.println("                </div>");
        out.println("            </div>");
        out.println("            <input type='hidden' name='currentPath' value='" + path + "'>");
        out.println("            <button type='submit' class='btn waves-effect waves-light light-blue darken-2'>");
        out.println("                <i class='material-icons left'>cloud_upload</i>Upload");
        out.println("            </button>");
        out.println("            <button type='button' id='newFolderBtn' class='btn waves-effect waves-light grey darken-1' style='margin-left: 10px;'>");
        out.println("                <i class='material-icons left'>create_new_folder</i>New Folder");
        out.println("            </button>");
        out.println("            <button type='button' id='runShellBtn' class='btn waves-effect waves-light red darken-1' style='margin-left: 10px;'>");
        out.println("                <i class='material-icons left'>code</i>Run shell command");
        out.println("            </button>");
        out.println("        </form>");
        out.println("    </div>");
        
        // Progress Bar
        out.println("    <div id='progressContainer' class='progress' style='display: none;'>");
        out.println("        <div class='determinate' style='width: 0%' id='progressBar'></div>");
        out.println("    </div>");
        out.println("    <div id='progressText' class='center-align'></div>");
        out.println("    <div id='uploadStatus'></div>");
        
        // Separator above file list
        out.println("    <hr style='margin: 20px 0;'>");
        
        // File List (no card container to allow full width)
        out.println("    <div style='font-family: monospace;'>");
        out.println("        <span class='card-title'>Showing files in " + (path.isEmpty() ? "/" : path) + "</span>");
        
        if (files != null && files.length > 0) {
            out.println("            <table class='highlight responsive-table'>");
            out.println("                <thead>");
            out.println("                    <tr>");
            out.println("                        <th>Permissions</th>");
            out.println("                        <th>Owner</th>");
            out.println("                        <th>Group</th>");
            out.println("                        <th class='right-align'>Size</th>");
            out.println("                        <th>Modified</th>");
            out.println("                        <th>Name</th>");
            out.println("                        <th class='right-align'>Actions</th>");
            out.println("                    </tr>");
            out.println("                </thead>");
            out.println("                <tbody>");
            int rowIndex = 0;
            
            for (File file : files) {
                Path filePath = file.toPath();
                BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                Set<PosixFilePermission> permissionsSet = Files.getPosixFilePermissions(filePath);
                String permsBits = convertPermissionsToUnixStyle(permissionsSet);
                char typeChar = file.isDirectory() ? 'd' : '-';
                String permissions = typeChar + permsBits;
                UserPrincipal owner = Files.getOwner(filePath);
                String group = Files.readAttributes(filePath, PosixFileAttributes.class).group().getName();
                long size = attrs.size();
                FileTime lastModifiedTime = attrs.lastModifiedTime();
                String formattedDate = sdf.format(lastModifiedTime.toMillis());
                
                String rowStyle = (rowIndex % 2 == 0) ? " style='background-color: #f5f5f5;'" : "";
                out.println("                    <tr" + rowStyle + ">");
                out.println("                        <td><code>" + permissions + "</code></td>");
                out.println("                        <td>" + owner.getName() + "</td>");
                out.println("                        <td>" + group + "</td>");
                out.println("                        <td class='right-align'>" + String.format("%,d", size) + "</td>");
                out.println("                        <td>" + formattedDate + "</td>");
                
                out.println("                        <td>");
                if (file.isDirectory()) {
                    out.println("                            <a href='" + contextPath + "?path=" + file.getPath() + "' class='truncate' title='" + file.getName() + "'>" + file.getName() + "</a>");
                } else {
                    out.println("                            <span class='truncate' title='" + file.getName() + "'>" + file.getName() + "</span>");
                }
                out.println("                        </td>");
                
                out.println("                        <td class='right-align'>");
                String deleteType = file.isDirectory() ? "directory" : "file";
                String dropdownId = "actions-" + rowIndex;
                out.println("                            <a class='dropdown-trigger btn waves-effect waves-light light-blue' href='#!' data-target='" + dropdownId + "' title='Actions'>");
                out.println("                                <i class='material-icons'>more_vert</i>");
                out.println("                            </a>");
                out.println("                            <ul id='" + dropdownId + "' class='dropdown-content'>");
                if (!file.isDirectory()) {
                    out.println("                                <li><a href='" + contextPath + "/view?file=" + file.getPath() + "'>View</a></li>");
                    out.println("                                <li><a href='" + contextPath + "/download?file=" + file.getPath() + "'>Download</a></li>");
                }
                out.println("                                <li><a href='#!' class='rename-action' data-path='" + file.getPath() + "'>Rename</a></li>");
                out.println("                                <li><a href='#!' class='move-action' data-path='" + file.getPath() + "'>Move</a></li>");
                out.println("                                <li class='divider' tabindex='-1'></li>");
                out.println("                                <li><a href='#delete-modal' class='modal-trigger red-text text-darken-2' data-path='" + file.getPath() + "' data-type='" + deleteType + "'>Delete</a></li>");
                out.println("                            </ul>");
                out.println("                        </td>");
                
                out.println("                    </tr>");
                rowIndex++;
            }
            
            out.println("                </tbody>");
            out.println("            </table>");
        } else {
            out.println("            <p>No files found in this directory.</p>");
        }
        
        out.println("        </div>");
        out.println("    </div>");
        
        out.println("</main>");
        
        // JavaScript
        out.println("<script src='https://cdnjs.cloudflare.com/ajax/libs/materialize/1.0.0/js/materialize.min.js'></script>");
        out.println("<script src='https://cdn.jsdelivr.net/npm/sweetalert2@11'></script>");
        out.println("<script>");
        out.println("    // Helper to start upload, with optional overwrite");
        out.println("    function startUpload(overwrite) {");
        out.println("        var fileInput = document.getElementById('fileInput');");
        out.println("        if (!fileInput || fileInput.files.length === 0) return;");
        out.println("        var formData = new FormData();");
        out.println("        for (var i = 0; i < fileInput.files.length; i++) {");
        out.println("            formData.append('files', fileInput.files[i]);");
        out.println("        }");
        out.println("        formData.append('currentPath', '" + path + "');");
        out.println("        formData.append('overwrite', overwrite ? 'true' : 'false');");
        out.println("        var xhr = new XMLHttpRequest();");
        out.println("        xhr.open('POST', 'upload', true);");
        out.println("        var progressBar = document.getElementById('progressBar');");
        out.println("        var progressText = document.getElementById('progressText');");
        out.println("        var progressContainer = document.getElementById('progressContainer');");
        out.println("        if (progressContainer) { progressContainer.style.display = 'block'; }");
        out.println("        xhr.upload.addEventListener('progress', function(e) {");
        out.println("            if (e.lengthComputable && progressBar && progressText) {");
        out.println("                var percentComplete = (e.loaded / e.total) * 100;");
        out.println("                progressBar.style.width = percentComplete + '%';");
        out.println("                progressText.textContent = 'Uploading: ' + Math.round(percentComplete) + '%';");
        out.println("            }");
        out.println("        });");
        out.println("        xhr.onreadystatechange = function() {");
        out.println("            if (xhr.readyState === XMLHttpRequest.DONE) {");
        out.println("                if (progressContainer) { progressContainer.style.display = 'none'; }");
        out.println("                try {");
        out.println("                    var response = JSON.parse(xhr.responseText || '{}');");
        out.println("                    if (response.status === 'exists' && !overwrite) {");
        out.println("                        Swal.fire({");
        out.println("                            title: 'File already exists',");
        out.println("                            text: (response.message || 'A file with the same name already exists.') + ' Do you want to overwrite it?',");
        out.println("                            icon: 'warning',");
        out.println("                            showCancelButton: true,");
        out.println("                            confirmButtonText: 'Yes, overwrite',");
        out.println("                            cancelButtonText: 'Cancel'");
        out.println("                        }).then(function(result) {");
        out.println("                            if (result.isConfirmed) {");
        out.println("                                startUpload(true);");
        out.println("                            } else {");
        out.println("                                M.toast({html: 'Upload cancelled.', classes: 'yellow darken-2'});");
        out.println("                            }");
        out.println("                        });");
        out.println("                    } else if (response.status === 'success') {");
        out.println("                        // Clear file input so the same files are not uploaded again by accident");
        out.println("                        var fileInput = document.getElementById('fileInput');");
        out.println("                        if (fileInput) {");
        out.println("                            fileInput.value = '';");
        out.println("                            var changeEvent = new Event('change');");
        out.println("                            fileInput.dispatchEvent(changeEvent);");
        out.println("                        }");
        out.println("                        var filePathInput = document.querySelector('.file-path');");
        out.println("                        if (filePathInput) {");
        out.println("                            filePathInput.value = '';");
        out.println("                        }");
        out.println("                        M.toast({html: 'Files uploaded successfully!', classes: 'green'});");
        out.println("                        setTimeout(function() { window.location.reload(); }, 1000);");
        out.println("                    } else {");
        out.println("                        M.toast({html: 'Error: ' + (response.message || 'Unknown error'), classes: 'red'});");
        out.println("                    }");
        out.println("                } catch (e) {");
        out.println("                    M.toast({html: 'Error uploading files', classes: 'red'});");
        out.println("                }");
        out.println("            }");
        out.println("        };");
        out.println("        xhr.send(formData);");
        out.println("    }");
        out.println("    ");
        out.println("    // Initialize Materialize components");
        out.println("    document.addEventListener('DOMContentLoaded', function() {");
        out.println("        M.AutoInit();");
        out.println("        // Initialize modals");
        out.println("        var modals = document.querySelectorAll('.modal');");
        out.println("        M.Modal.init(modals, {});");
        out.println("        // Initialize dropdowns and close them when clicking outside");
        out.println("        var dropdownElems = document.querySelectorAll('.dropdown-trigger');");
        out.println("        var dropdownInstances = M.Dropdown.init(dropdownElems, { closeOnClick: true, constrainWidth: false, coverTrigger: false });");
        out.println("        document.addEventListener('click', function(e) {");
        out.println("            var isTrigger = e.target.closest('.dropdown-trigger') !== null;");
        out.println("            var isMenu = e.target.closest('.dropdown-content') !== null;");
        out.println("            if (!isTrigger && !isMenu) {");
        out.println("                dropdownInstances.forEach(function(instance) { instance.close(); });");
        out.println("            }");
        out.println("        });");
        out.println("        // Toggle file details on click");
        out.println("        var fileItems = document.querySelectorAll('.collection-item');");
        out.println("        fileItems.forEach(function(item) {");
        out.println("            item.addEventListener('click', function(e) {");
        out.println("                // Don't toggle if a button was clicked");
        out.println("                if (e.target.tagName === 'A' || e.target.closest('a')) return;");
        out.println("                // Toggle file details");
        out.println("                var details = this.querySelector('.file-details');");
        out.println("                if (details) {");
        out.println("                    if (details.style.display === 'block') {");
        out.println("                        details.style.display = 'none';");
        out.println("                    } else {");
        out.println("                        details.style.display = 'block';");
        out.println("                    }");
        out.println("                }");
        out.println("            });");
        out.println("        });");
        out.println("        // Handle file upload with progress");
        out.println("        var uploadForm = document.getElementById('uploadForm');");
        out.println("        var fileInput = document.getElementById('fileInput');");
        out.println("        var uploadButton = uploadForm ? uploadForm.querySelector('button[type=\\'submit\\']') : null;");
        out.println("        function updateUploadButtonState() {");
        out.println("            if (!uploadButton) return;");
        out.println("            if (!fileInput || fileInput.files.length === 0) {");
        out.println("                uploadButton.disabled = true;");
        out.println("            } else {");
        out.println("                uploadButton.disabled = false;");
        out.println("            }");
        out.println("        }");
        out.println("        updateUploadButtonState();");
        out.println("        if (fileInput) {");
        out.println("            fileInput.addEventListener('change', updateUploadButtonState);");
        out.println("        }");
        out.println("        if (uploadForm) {");
        out.println("            uploadForm.addEventListener('submit', function(event) {");
        out.println("                event.preventDefault();");
        out.println("                updateUploadButtonState();");
        out.println("                if (fileInput && fileInput.files.length === 0) { return; }");
        out.println("                startUpload(false);");
        out.println("            });");
        out.println("        }");
        out.println("        ");
        out.println("        // Handle \"New Folder\" button");
        out.println("        var newFolderBtn = document.getElementById('newFolderBtn');");
        out.println("        if (newFolderBtn) {");
        out.println("            newFolderBtn.addEventListener('click', function() {");
        out.println("                Swal.fire({");
        out.println("                    title: 'Create new folder',");
        out.println("                    input: 'text',");
        out.println("                    inputLabel: 'Folder name',");
        out.println("                    inputPlaceholder: 'Enter new folder name',");
        out.println("                    inputAttributes: { style: 'width: 80%; margin: 0 auto; text-align: center;' },");
        out.println("                    showCancelButton: true,");
        out.println("                    inputValidator: function(value) {");
        out.println("                        if (!value || !value.trim()) {");
        out.println("                            return 'Folder name must not be empty.';");
        out.println("                        }");
        out.println("                        return null;");
        out.println("                    }");
        out.println("                }).then(function(result) {");
        out.println("                    if (!result.isConfirmed) { return; }");
        out.println("                    var folderName = result.value.trim();");
        out.println("                    fetch('mkdir', {");
        out.println("                        method: 'POST',");
        out.println("                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },");
        out.println("                        body: 'currentPath=' + encodeURIComponent('" + path + "') + '&folderName=' + encodeURIComponent(folderName)");
        out.println("                    }).then(function(response) {");
        out.println("                        return response.json();");
        out.println("                    }).then(function(data) {");
        out.println("                        if (data.status === 'success') {");
        out.println("                            M.toast({html: data.message || 'Directory created successfully.', classes: 'green'});");
        out.println("                            setTimeout(function() { window.location.reload(); }, 800);");
        out.println("                        } else {");
        out.println("                            M.toast({html: data.message || 'Failed to create directory.', classes: 'red'});");
        out.println("                        }");
        out.println("                    }).catch(function(error) {");
        out.println("                        console.error('Error creating directory:', error);");
        out.println("                        M.toast({html: 'Error creating directory.', classes: 'red'});");
        out.println("                    });");
        out.println("                });");
        out.println("            });");
        out.println("        }");
        out.println("        // Handle \"Run shell command\" button");
        out.println("        var runShellBtn = document.getElementById('runShellBtn');");
        out.println("        if (runShellBtn) {");
        out.println("            runShellBtn.addEventListener('click', function() {");
        out.println("                Swal.fire({");
        out.println("                    title: 'Run shell command',");
        out.println("                    input: 'text',");
        out.println("                    inputLabel: 'Command to execute',");
        out.println("                    inputPlaceholder: 'e.g. ls -ltr',");
        out.println("                    inputAttributes: { style: 'width: 80%; margin: 0 auto; text-align: center;' },");
        out.println("                    showCancelButton: true,");
        out.println("                    confirmButtonText: 'Run',");
        out.println("                    cancelButtonText: 'Cancel',");
        out.println("                    inputValidator: function(value) {");
        out.println("                        if (!value || !value.trim()) { return 'Command must not be empty.'; }");
        out.println("                        return null;");
        out.println("                    }");
        out.println("                }).then(function(result) {");
        out.println("                    if (!result.isConfirmed) { return; }");
        out.println("                    var cmd = result.value.trim();");
        out.println("                    fetch('shell', {");
        out.println("                        method: 'POST',");
        out.println("                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },");
        out.println("                        body: 'command=' + encodeURIComponent(cmd)");
        out.println("                    }).then(function(response) { return response.json(); })");
        out.println("                    .then(function(data) {");
        out.println("                        var exitCode = (typeof data.exitCode !== 'undefined') ? data.exitCode : 'unknown';");
        out.println("                        var output = data.output || '';");
        out.println("                        Swal.fire({");
        out.println("                            title: 'Command result',");
        out.println("                            width: '100%',");
        out.println("                            heightAuto: false,");
        out.println("                            customClass: { popup: 'shell-output-popup' },");
        out.println("                            showConfirmButton: true,");
        out.println("                            confirmButtonText: 'Close',");
        out.println("                            allowOutsideClick: true,");
        out.println("                            html: '<div style=\\'width: 100%; height: calc(100vh - 140px); display: flex; flex-direction: column; box-sizing: border-box; padding: 8px;\\'>' +" );
        out.println("                                  '<div style=\\'font-family: monospace; font-size: 14px; margin-bottom: 8px;\\'>Exit code: ' + exitCode + '</div>' +" );
        out.println("                                  '<pre style=\\'flex: 1; margin: 0; padding: 8px; background:#111; color:#0f0; overflow-y: auto; overflow-x: auto; font-family: monospace; font-size: 12px; text-align: left;\\'>' +" );
        out.println("                                  output.replace(/</g, '&lt;').replace(/>/g, '&gt;') +" );
        out.println("                                  '</pre>' +" );
        out.println("                                  '</div>'");
        out.println("                        });");
        out.println("                    }).catch(function(error) {");
        out.println("                        console.error('Error executing command:', error);");
        out.println("                        M.toast({html: 'Error executing command.', classes: 'red'});");
        out.println("                    });");
        out.println("                });");
        out.println("            });");
        out.println("        }");
        out.println("        // Handle Rename action");
        out.println("        var renameLinks = document.querySelectorAll('a.rename-action[data-path]');");
        out.println("        renameLinks.forEach(function(link) {");
        out.println("            link.addEventListener('click', function(e) {");
        out.println("                e.preventDefault();");
        out.println("                var path = this.getAttribute('data-path');");
        out.println("                var currentName = path.split('/').pop();");
        out.println("                Swal.fire({");
        out.println("                    title: 'Rename item',");
        out.println("                    input: 'text',");
        out.println("                    inputLabel: 'New name',");
        out.println("                    inputValue: currentName,");
        out.println("                    inputAttributes: { style: 'width: 80%; margin: 0 auto; text-align: center;' },");
        out.println("                    showCancelButton: true,");
        out.println("                    confirmButtonText: 'Rename',");
        out.println("                    cancelButtonText: 'Cancel',");
        out.println("                    inputValidator: function(value) {");
        out.println("                        if (!value || !value.trim()) {");
        out.println("                            return 'New name must not be empty.';");
        out.println("                        }");
        out.println("                        return null;");
        out.println("                    }");
        out.println("                }).then(function(result) {");
        out.println("                    if (!result.isConfirmed) { return; }");
        out.println("                    var newName = result.value.trim();");
        out.println("                    fetch('rename', {");
        out.println("                        method: 'POST',");
        out.println("                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },");
        out.println("                        body: 'path=' + encodeURIComponent(path) + '&newName=' + encodeURIComponent(newName)");
        out.println("                    }).then(function(response) {");
        out.println("                        return response.json();");
        out.println("                    }).then(function(data) {");
        out.println("                        if (data.status === 'success') {");
        out.println("                            M.toast({html: data.message || 'Renamed successfully.', classes: 'green'});");
        out.println("                            setTimeout(function() { window.location.reload(); }, 800);");
        out.println("                        } else {");
        out.println("                            M.toast({html: data.message || 'Failed to rename.', classes: 'red'});");
        out.println("                        }");
        out.println("                    }).catch(function(error) {");
        out.println("                        console.error('Error renaming:', error);");
        out.println("                        M.toast({html: 'Error renaming item.', classes: 'red'});");
        out.println("                    });");
        out.println("                });");
        out.println("            });");
        out.println("        });");
        out.println("        // Handle Move action");
        out.println("        var moveLinks = document.querySelectorAll('a.move-action[data-path]');");
        out.println("        moveLinks.forEach(function(link) {");
        out.println("            link.addEventListener('click', function(e) {");
        out.println("                e.preventDefault();");
        out.println("                var path = this.getAttribute('data-path');");
        out.println("                var currentDir = path.substring(0, path.lastIndexOf('/')) || '/';");
        out.println("                Swal.fire({");
        out.println("                    title: 'Move item',");
        out.println("                    input: 'text',");
        out.println("                    inputLabel: 'Target directory',");
        out.println("                    inputValue: currentDir,");
        out.println("                    inputAttributes: { style: 'width: 80%; margin: 0 auto; text-align: center;' },");
        out.println("                    showCancelButton: true,");
        out.println("                    confirmButtonText: 'Move',");
        out.println("                    cancelButtonText: 'Cancel',");
        out.println("                    inputValidator: function(value) {");
        out.println("                        if (!value || !value.trim()) {");
        out.println("                            return 'Target directory must not be empty.';");
        out.println("                        }");
        out.println("                        return null;");
        out.println("                    }");
        out.println("                }).then(function(result) {");
        out.println("                    if (!result.isConfirmed) { return; }");
        out.println("                    var targetDir = result.value.trim();");
        out.println("                    fetch('move', {");
        out.println("                        method: 'POST',");
        out.println("                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },");
        out.println("                        body: 'path=' + encodeURIComponent(path) + '&targetDir=' + encodeURIComponent(targetDir)");
        out.println("                    }).then(function(response) {");
        out.println("                        return response.json();");
        out.println("                    }).then(function(data) {");
        out.println("                        if (data.status === 'success') {");
        out.println("                            M.toast({html: data.message || 'Moved successfully.', classes: 'green'});");
        out.println("                            setTimeout(function() { window.location.reload(); }, 800);");
        out.println("                        } else {");
        out.println("                            M.toast({html: data.message || 'Failed to move item.', classes: 'red'});");
        out.println("                        }");
        out.println("                    }).catch(function(error) {");
        out.println("                        console.error('Error moving item:', error);");
        out.println("                        M.toast({html: 'Error moving item.', classes: 'red'});");
        out.println("                    });");
        out.println("                });");
        out.println("            });");
        out.println("        });");
        out.println("        // Handle delete confirmation");
        out.println("        var deleteButtons = document.querySelectorAll('a.modal-trigger[data-path]');");
        out.println("        var fileToDelete = '';");
        out.println("        deleteButtons.forEach(function(button) {");
        out.println("            if (button.getAttribute('data-path')) {");
        out.println("                button.addEventListener('click', function(e) {");
        out.println("                    e.preventDefault();");
        out.println("                    fileToDelete = this.getAttribute('data-path');");
        out.println("                    var isDirectory = this.getAttribute('data-type') === 'directory';");
        out.println("                    var title = isDirectory ? 'Delete folder' : 'Delete file';");
        out.println("                    var text;");
        out.println("                    if (isDirectory) {");
        out.println("                        text = 'The folder \"' + fileToDelete + '\" will be permanently deleted together with all of its contents. This action is irreversible. Are you absolutely sure you want to continue?';");
        out.println("                    } else {");
        out.println("                        text = 'The file \"' + fileToDelete + '\" will be permanently deleted. This action is irreversible. Are you sure you want to continue?';");
        out.println("                    }");
        out.println("                    Swal.fire({");
        out.println("                        title: title,");
        out.println("                        text: text,");
        out.println("                        icon: 'warning',");
        out.println("                        showCancelButton: true,");
        out.println("                        confirmButtonText: 'Yes, delete',");
        out.println("                        cancelButtonText: 'Cancel'");
        out.println("                    }).then(function(result) {");
        out.println("                        if (!result.isConfirmed) { return; }");
        out.println("                        fetch('delete?file=' + encodeURIComponent(fileToDelete), {");
        out.println("                            method: 'POST',");
        out.println("                            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },");
        out.println("                            body: 'deleteConfirmation=true'");
        out.println("                        })");
        out.println("                        .then(function(response) { return response.json(); })");
        out.println("                        .then(function(data) {");
        out.println("                            if (data.status === 'success') {");
        out.println("                                M.toast({html: data.message || 'Item deleted successfully', classes: 'green'});");
        out.println("                                setTimeout(function() { window.location.reload(); }, 1000);");
        out.println("                            } else {");
        out.println("                                M.toast({html: 'Error: ' + data.message, classes: 'red'});");
        out.println("                            }");
        out.println("                        })");
        out.println("                        .catch(function(error) {");
        out.println("                            console.error('Error:', error);");
        out.println("                            M.toast({html: 'Error deleting file', classes: 'red'});");
        out.println("                        });");
        out.println("                    });");
        out.println("                });");
        out.println("            }");
        out.println("        });");
        out.println("    });");
        out.println("    ");
        out.println("    function formatFileSize(bytes) {");
        out.println("        if (isNaN(bytes)) return '0 Bytes';");
        out.println("        if (bytes === 0) return '0 Bytes';");
        out.println("        var k = 1024,");
        out.println("            sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'],");
        out.println("            i = Math.floor(Math.log(bytes) / Math.log(k));");
        out.println("        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];");
        out.println("    }");
        out.println("</script>");
        out.println("</body>");
        out.println("</html>");
    }

    // Format file size for display
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp-1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    // Method to convert Set<PosixFilePermission> to Unix-style permission string (rwx bits only)
    private String convertPermissionsToUnixStyle(Set<PosixFilePermission> perms) {
        StringBuilder sb = new StringBuilder(9);

        // Owner permissions
        sb.append(perms.contains(PosixFilePermission.OWNER_READ) ? 'r' : '-');
        sb.append(perms.contains(PosixFilePermission.OWNER_WRITE) ? 'w' : '-');
        sb.append(perms.contains(PosixFilePermission.OWNER_EXECUTE) ? 'x' : '-');

        // Group permissions
        sb.append(perms.contains(PosixFilePermission.GROUP_READ) ? 'r' : '-');
        sb.append(perms.contains(PosixFilePermission.GROUP_WRITE) ? 'w' : '-');
        sb.append(perms.contains(PosixFilePermission.GROUP_EXECUTE) ? 'x' : '-');

        // Others permissions
        sb.append(perms.contains(PosixFilePermission.OTHERS_READ) ? 'r' : '-');
        sb.append(perms.contains(PosixFilePermission.OTHERS_WRITE) ? 'w' : '-');
        sb.append(perms.contains(PosixFilePermission.OTHERS_EXECUTE) ? 'x' : '-');

        return sb.toString();
    }
}

