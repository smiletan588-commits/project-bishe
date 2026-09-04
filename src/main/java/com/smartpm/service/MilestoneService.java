package com.smartpm.service;

import com.smartpm.dto.MilestoneDTO;
import com.smartpm.entity.ProjectMilestone;

import java.util.List;

public interface MilestoneService {
    List<ProjectMilestone> list(Long projectId);
    ProjectMilestone create(Long projectId, MilestoneDTO dto);
    ProjectMilestone update(Long projectId, MilestoneDTO dto);
    void delete(Long projectId, Long milestoneId);
}
