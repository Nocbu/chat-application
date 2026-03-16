package com.example.chat_application.Services;

import com.example.chat_application.Repositories.FileAttachmentRepository;
import com.example.chat_application.model.FileAttachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private Path uploadPath;

    @Autowired
    private FileAttachmentRepository fileRepository;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    public FileAttachment storeFile(MultipartFile file, String uploaderEmail, String uploaderName) throws IOException {
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID().toString() + extension;

        Path targetLocation = uploadPath.resolve(storedName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        FileAttachment attachment = new FileAttachment();
        attachment.setOriginalFileName(originalName);
        attachment.setStoredFileName(storedName);
        attachment.setContentType(file.getContentType());
        attachment.setSize(file.getSize());
        attachment.setUploadedBy(uploaderEmail);
        attachment.setUploadedByName(uploaderName);
        attachment.setFilePath(targetLocation.toString());

        return fileRepository.save(attachment);
    }

    public Resource loadFileAsResource(String fileId) {
        Optional<FileAttachment> attachment = fileRepository.findById(fileId);

        if (attachment.isEmpty()) {
            throw new RuntimeException("File not found: " + fileId);
        }

        try {
            Path filePath = uploadPath.resolve(attachment.get().getStoredFileName()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            }
            throw new RuntimeException("File not found on disk: " + fileId);
        } catch (MalformedURLException e) {
            throw new RuntimeException("File not found: " + fileId, e);
        }
    }

    public Optional<FileAttachment> getFileInfo(String fileId) {
        return fileRepository.findById(fileId);
    }

    public boolean deleteFile(String fileId) {
        Optional<FileAttachment> attachment = fileRepository.findById(fileId);
        if (attachment.isPresent()) {
            try {
                Path filePath = uploadPath.resolve(attachment.get().getStoredFileName());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // ignore
            }
            fileRepository.deleteById(fileId);
            return true;
        }
        return false;
    }
}