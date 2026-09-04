package com.smartpm.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** AI 生成的完整项目计划。仅在用户确认后才会被写入项目。 */
@Data
public class AIProjectPlanDTO {
    private String overview;
    private List<Stage> stages = new ArrayList<>();
    private List<PlanTask> tasks = new ArrayList<>();
    private List<PlanMilestone> milestones = new ArrayList<>();
    private List<Risk> risks = new ArrayList<>();

    @Data
    public static class Stage {
        private String name;
        private String startDate;
        private String endDate;
        private String goal;
    }

    @Data
    public static class PlanTask {
        private String stage;
        private String title;
        private String description;
        private String recommendedRole;
        private String priority;
        private String tags;
        private String startDate;
        private String dueDate;
        private Integer estimatedHours;
        private String acceptanceCriteria;
    }

    @Data
    public static class PlanMilestone {
        private String name;
        private String description;
        private String targetDate;
        /** 对应 tasks 数组的从 0 开始索引。 */
        private List<Integer> taskIndexes = new ArrayList<>();
    }

    @Data
    public static class Risk {
        private String level;
        private String title;
        private String description;
        private String mitigation;
    }
}
