package com.smartpm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("pm_project_milestone")
public class ProjectMilestone {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String name;
    private String description;
    private LocalDate targetDate;
    /** PLANNED / COMPLETED */
    private String status;
    /** 关联任务 ID，使用逗号分隔保存 */
    private String taskIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
