package com.example.chat_application.Repositories;



import com.example.chat_application.model.FileAttachment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface FileAttachmentRepository extends MongoRepository<FileAttachment, String> {
    List<FileAttachment> findByUploadedBy(String uploadedBy);
}
