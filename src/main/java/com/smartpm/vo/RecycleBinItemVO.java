package com.smartpm.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecycleBinItemVO {
    /** PROJECT / TASK / WIKI / ATTACHMENT */
    private String type;
    private Long id;
    private String title;
    private Long projectId;
    private String projectName;
    private LocalDateTime deletedAt;
    private Long deletedBy;
    private String deletedByName;
}
