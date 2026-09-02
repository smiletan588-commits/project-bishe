package com.smartpm.dto;

import lombok.Data;

@Data
public class DragDTO {

    /** 被拖动的任务ID */
    private Long taskId;

    /** 目标状态列: TODO / IN_PROGRESS / DONE */
    private String targetStatus;

    /** 目标列中的排序位置 (0-based, 超出范围则放到末尾) */
    private Integer targetOrderIndex;
}
