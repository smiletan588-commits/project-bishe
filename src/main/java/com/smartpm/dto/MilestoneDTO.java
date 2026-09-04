package com.smartpm.dto;

import lombok.Data;

@Data
public class MilestoneDTO {
    private Long id;
    private String name;
    private String description;
    private String targetDate;
    private String status;
    /** 关联任务 ID，使用逗号分隔。 */
    private String taskIds;
}
