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

    private Integer orderIndex;
}
