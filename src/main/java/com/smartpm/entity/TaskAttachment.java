package com.smartpm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_task_attachment")
public class TaskAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long projectId;
    private String originalName;
    private String storedName;
    private String contentType;
    private Long size;
    private Long uploaderId;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private Long deletedBy;
}
