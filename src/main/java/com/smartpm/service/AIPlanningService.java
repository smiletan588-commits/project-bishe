package com.smartpm.service;

import com.smartpm.dto.AIProjectPlanDTO;
import com.smartpm.dto.AITaskOptimizationVO;
import com.smartpm.entity.Task;

import java.util.List;

public interface AIPlanningService {
    AIProjectPlanDTO generateProjectPlan(Long projectId);
    List<Task> applyProjectPlan(Long projectId, AIProjectPlanDTO plan);
    AITaskOptimizationVO optimizeTask(Long taskId);
}
