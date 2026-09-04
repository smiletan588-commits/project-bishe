package com.smartpm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    /** 任务优先级：HIGH / MEDIUM / LOW */
    private String priority;

    /** 任务标签，使用英文代码逗号分隔保存 */
    private String tags;

    private Long creatorId;

    private LocalDate dueDate;

    private LocalDate startDate;

    /** 预计工时（小时） */
    private Integer estimatedHours;

    /** 实际工时（小时） */
    private Integer actualHours;

    /** 前置依赖任务 ID，使用逗号分隔保存 */
    private String dependencyIds;

    /** 可验证的任务完成条件 */
    private String acceptanceCriteria;

    /** 查询时计算，不落库：是否被未完成前置任务阻塞 */
    @TableField(exist = false)
    private Boolean blocked;

    /** 查询时计算，不落库：阻塞当前任务的前置任务标题 */
    @TableField(exist = false)
    private java.util.List<String> blockedByTaskTitles;

    private Integer orderIndex;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
