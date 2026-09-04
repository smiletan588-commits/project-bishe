package com.smartpm.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/** AI 对单个任务的优化建议，前端确认后再通过普通更新接口保存。 */
@Data
public class AITaskOptimizationVO {
    private String title;
    private String description;
    @JsonAlias({"acceptance_criteria", "criteria"})
    private String acceptanceCriteria;

    @JsonAlias({"isOversized", "is_oversized", "tooLarge", "too_large"})
    private Boolean oversized;

    @JsonAlias({"split_advice", "splitSuggestion", "split_suggestion"})
    private String splitAdvice;
}
