package com.smartpm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_attachment_download_log")
public class AttachmentDownloadLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long attachmentId;
    private Long taskId;
    private Long projectId;
    private Long downloaderId;
    private LocalDateTime downloadedAt;
}
