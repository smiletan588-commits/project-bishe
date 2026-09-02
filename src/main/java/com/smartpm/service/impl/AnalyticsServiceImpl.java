package com.smartpm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartpm.common.utils.UserHolder;
import com.smartpm.entity.Project;
import com.smartpm.entity.Task;
import com.smartpm.mapper.ProjectMapper;
import com.smartpm.mapper.TaskMapper;
import com.smartpm.service.AnalyticsService;
import com.smartpm.vo.AnalyticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;

    @Override
    public AnalyticsVO getOverview() {
        Long userId = UserHolder.getUserId();

        List<Project> projects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>().eq(Project::getCreatorId, userId));
        List<Long> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());

        AnalyticsVO vo = new AnalyticsVO();
        vo.setTotalProjects(projects.size());

        if (projectIds.isEmpty()) {
            vo.setTotalTasks(0);
            vo.setCompletedTasks(0);
            vo.setInProgressTasks(0);
            vo.setStatusDistribution(List.of());
            vo.setProjectTaskRanking(List.of());
            vo.setDailyCompletedTrend(buildEmptyTrend());
            return vo;
        }

        List<Task> allTasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().in(Task::getProjectId, projectIds));

        // 基础概览
        int total = allTasks.size();
        long completed = allTasks.stream().filter(t -> "DONE".equals(t.getStatus())).count();
        long inProgress = allTasks.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).count();
        vo.setTotalTasks(total);
        vo.setCompletedTasks((int) completed);
        vo.setInProgressTasks((int) inProgress);

        // 任务状态分布
        Map<String, Long> statusMap = allTasks.stream()
                .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
        List<AnalyticsVO.StatusItem> statusDistribution = new ArrayList<>();
        for (String s : List.of("TODO", "IN_PROGRESS", "DONE")) {
            statusDistribution.add(new AnalyticsVO.StatusItem(s, statusMap.getOrDefault(s, 0L)));
        }
        vo.setStatusDistribution(statusDistribution);

        // 项目任务量排行
        Map<Long, Long> projectTaskCount = allTasks.stream()
                .collect(Collectors.groupingBy(Task::getProjectId, Collectors.counting()));
        Map<Long, String> projectNames = projects.stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));
        List<AnalyticsVO.ProjectRankItem> ranking = projectTaskCount.entrySet().stream()
                .map(e -> new AnalyticsVO.ProjectRankItem(
                        projectNames.getOrDefault(e.getKey(), "未知项目"), e.getValue()))
                .sorted((a, b) -> Long.compare(b.getTaskCount(), a.getTaskCount()))
                .collect(Collectors.toList());
        vo.setProjectTaskRanking(ranking);

        // 近7天完成趋势
        vo.setDailyCompletedTrend(buildDailyTrend(allTasks));

        return vo;
    }

    private List<AnalyticsVO.DailyTrendItem> buildDailyTrend(List<Task> tasks) {
        LocalDate today = LocalDate.now();
        List<AnalyticsVO.DailyTrendItem> trend = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = tasks.stream()
                    .filter(t -> "DONE".equals(t.getStatus())
                            && t.getUpdatedAt() != null
                            && t.getUpdatedAt().toLocalDate().equals(date))
                    .count();
            trend.add(new AnalyticsVO.DailyTrendItem(date.toString(), count));
        }
        return trend;
    }

    private List<AnalyticsVO.DailyTrendItem> buildEmptyTrend() {
        LocalDate today = LocalDate.now();
        List<AnalyticsVO.DailyTrendItem> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            trend.add(new AnalyticsVO.DailyTrendItem(today.minusDays(i).toString(), 0));
        }
        return trend;
    }
}
