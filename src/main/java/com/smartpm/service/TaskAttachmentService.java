package com.smartpm.service;

import com.smartpm.entity.AttachmentDownloadLog;
import com.smartpm.entity.TaskAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface TaskAttachmentService {
    List<TaskAttachment> list(Long taskId);
    TaskAttachment upload(Long taskId, MultipartFile file);
    Path download(Long attachmentId);
    void delete(Long attachmentId);
    List<AttachmentDownloadLog> listDownloadLogs(Long attachmentId);
    TaskAttachment get(Long attachmentId);
}
