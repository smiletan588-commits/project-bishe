package com.smartpm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartpm.common.exception.BusinessException;
import com.smartpm.dto.MilestoneDTO;
import com.smartpm.entity.ProjectMilestone;
import com.smartpm.entity.Task;
import com.smartpm.mapper.ProjectMilestoneMapper;
import com.smartpm.mapper.TaskMapper;
import com.smartpm.service.MilestoneService;
import com.smartpm.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MilestoneServiceImpl implements MilestoneService {

    private final ProjectMilestoneMapper milestoneMapper;
    private final TaskMapper taskMapper;
    private final ProjectService projectService;

    @Override
    public List<ProjectMilestone> list(Long projectId) {
        projectService.assertProjectAccess(projectId, false);
        return milestoneMapper.selectList(new LambdaQueryWrapper<ProjectMilestone>()
                .eq(ProjectMilestone::getProjectId, projectId)
                .orderByAsc(ProjectMilestone::getTargetDate)
                .orderByDesc(ProjectMilestone::getCreatedAt));
    }

    @Override
    public ProjectMilestone create(Long projectId, MilestoneDTO dto) {
        projectService.assertProjectAccess(projectId, true);
        ProjectMilestone milestone = new ProjectMilestone();
        milestone.setProjectId(projectId);
        apply(dto, milestone);
        milestone.setCreatedAt(LocalDateTime.now());
        milestone.setUpdatedAt(LocalDateTime.now());
        milestoneMapper.insert(milestone);
        return milestone;
    }

    @Override
    public ProjectMilestone update(Long projectId, MilestoneDTO dto) {
        if (dto.getId() == null) throw new BusinessException("里程碑 ID 不能为空");
        projectService.assertProjectAccess(projectId, true);
        ProjectMilestone milestone = milestoneMapper.selectById(dto.getId());
        if (milestone == null || !projectId.equals(milestone.getProjectId())) {
            throw new BusinessException("里程碑不存在");
        }
        apply(dto, milestone);
        milestone.setUpdatedAt(LocalDateTime.now());
        milestoneMapper.updateById(milestone);
        return milestone;
    }

    @Override
    public void delete(Long projectId, Long milestoneId) {
        projectService.assertProjectAccess(projectId, true);
        ProjectMilestone milestone = milestoneMapper.selectById(milestoneId);
        if (milestone == null || !projectId.equals(milestone.getProjectId())) {
            throw new BusinessException("里程碑不存在");
        }
        milestoneMapper.deleteById(milestoneId);
    }

    private void apply(MilestoneDTO dto, ProjectMilestone milestone) {
        if (dto.getName() == null || dto.getName().isBlank()) throw new BusinessException("里程碑名称不能为空");
        milestone.setName(dto.getName().trim());
        milestone.setDescription(dto.getDescription());
        milestone.setTargetDate(dto.getTargetDate() == null || dto.getTargetDate().isBlank()
                ? null : LocalDate.parse(dto.getTargetDate()));
        String status = dto.getStatus() == null || dto.getStatus().isBlank()
                ? "PLANNED" : dto.getStatus().toUpperCase(Locale.ROOT);
        if (!List.of("PLANNED", "COMPLETED").contains(status)) throw new BusinessException("无效的里程碑状态");
        milestone.setStatus(status);
        milestone.setTaskIds(normalizeTaskIds(milestone.getProjectId(), dto.getTaskIds()));
    }

    private String normalizeTaskIds(Long projectId, String taskIds) {
        if (taskIds == null || taskIds.isBlank()) return null;
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        try {
            Arrays.stream(taskIds.split(",")).filter(value -> !value.isBlank())
                    .forEach(value -> ids.add(Long.valueOf(value.trim())));
        } catch (NumberFormatException e) {
            throw new BusinessException("关联任务格式无效");
        }
        if (ids.isEmpty()) return null;
        List<Task> tasks = taskMapper.selectBatchIds(ids);
        if (tasks.size() != ids.size() || tasks.stream().anyMatch(task -> !projectId.equals(task.getProjectId()))) {
            throw new BusinessException("关联任务不存在或不属于当前项目");
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
