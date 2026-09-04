package com.smartpm.dto;

import lombok.Data;

@Data
public class TaskUpdateDTO {

    private Long id;

    private String title;

    private String description;

    private String status;

    private Long assigneeId;

    private String dueDate;

    private String startDate;

    private String priority;

    /** 标签代码，使用逗号分隔。空字符串可清空标签。 */
    private String tags;

    /** 前置任务 ID，使用逗号分隔。空字符串可清空依赖。 */
    private String dependencyIds;

    private Integer estimatedHours;

    private Integer actualHours;

    private String acceptanceCriteria;

    private Integer orderIndex;
}
