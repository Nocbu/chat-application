package com.example.chat_application.controller;

import com.example.chat_application.Services.FileStorageService;
import com.example.chat_application.model.FileAttachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    //uploads
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploaderEmail") String uploaderEmail,
            @RequestParam("uploaderName") String uploaderName) {

        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "File is empty");
            return ResponseEntity.badRequest().body(response);
        }

        //file szie 50mb
        if (file.getSize() > 50 * 1024 * 1024) {
            response.put("success", false);
            response.put("message", "File too large. Max 50MB.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            FileAttachment saved = fileStorageService.storeFile(file, uploaderEmail, uploaderName);

            response.put("success", true);
            response.put("fileId", saved.getId());
            response.put("fileName", saved.getOriginalFileName());
            response.put("fileType", saved.getContentType());
            response.put("fileSize", saved.getSize());
            response.put("message", "File uploaded successfully");

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    //file download
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        Resource resource = fileStorageService.loadFileAsResource(fileId);
        Optional<FileAttachment> fileInfo = fileStorageService.getFileInfo(fileId);

        String fileName = fileInfo.map(FileAttachment::getOriginalFileName).orElse("download");
        String contentType = fileInfo.map(FileAttachment::getContentType).orElse("application/octet-stream");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    //file info
    @GetMapping("/info/{fileId}")
    public ResponseEntity<FileAttachment> getFileInfo(@PathVariable String fileId) {
        return fileStorageService.getFileInfo(fileId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
