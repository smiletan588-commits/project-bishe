package com.smartpm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sys_task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父任务ID — NULL=主任务，非NULL=子任务 */
    private Long parentId;

    private Long projectId;

    private String title;

    private String description;

    private String status;

    private Long assigneeId;

    /** AI 推荐的专业身份角色 */
    private String recommendedRole;

    private Long creatorId;

    private LocalDate dueDate;

    private Integer orderIndex;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
