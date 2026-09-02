package com.smartpm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartpm.common.exception.BusinessException;
import com.smartpm.common.utils.UserHolder;
import com.smartpm.entity.Project;
import com.smartpm.entity.Task;
import com.smartpm.mapper.ProjectMapper;
import com.smartpm.mapper.TaskMapper;
import com.smartpm.service.AIService;
import com.smartpm.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;
    private final AIService aiService;

    @Override
    public Project create(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("项目名称不能为空");
        }

        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setCreatorId(UserHolder.getUserId());
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        projectMapper.insert(project);
        return project;
    }

    @Override
    public List<Project> list() {
        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getCreatorId, UserHolder.getUserId())
                        .orderByDesc(Project::getCreatedAt));
    }

    @Override
    public Project update(Long id, String name, String description) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        if (!project.getCreatorId().equals(UserHolder.getUserId())) {
            throw new BusinessException("无权修改此项目");
        }
        if (name != null && !name.isBlank()) {
            project.setName(name);
        }
        if (description != null) {
            project.setDescription(description);
        }
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        return project;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        if (!project.getCreatorId().equals(UserHolder.getUserId())) {
            throw new BusinessException("无权删除此项目");
        }

        List<Long> taskIds = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProjectId, id)
                        .isNull(Task::getParentId))
                .stream().map(Task::getId).collect(Collectors.toList());
        if (!taskIds.isEmpty()) {
            taskMapper.delete(new LambdaQueryWrapper<Task>().in(Task::getParentId, taskIds));
        }
        taskMapper.delete(new LambdaQueryWrapper<Task>().eq(Task::getProjectId, id));
        projectMapper.deleteById(id);
    }

    // ── AI 项目周报 ──

    @Override
    public Flux<String> generateSummary(Long projectId) {
        try {
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                throw new BusinessException("项目不存在");
            }
            log.info("[AI-Summary] 项目: id={}, name={}", projectId, project.getName());

            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

            // 查询全部任务，MyBatis-Plus 返回空列表而非 null，此处显式兜底
            List<Task> allTasks = taskMapper.selectList(
                    new LambdaQueryWrapper<Task>().eq(Task::getProjectId, projectId));
            if (allTasks == null) {
                allTasks = new ArrayList<>();
            }
            log.info("[AI-Summary] 查询到 {} 个任务（含子任务）", allTasks.size());

            // 近 7 天完成的任务 — 逐个判空，防止 NPE
            List<Task> doneTasks = new ArrayList<>();
            List<Task> inProgressTasks = new ArrayList<>();
            List<Task> overdueTasks = new ArrayList<>();

            LocalDate today = LocalDate.now();
            for (Task t : allTasks) {
                if (t == null) continue;
                String status = t.getStatus();
                if (status == null) continue;

                if ("DONE".equals(status) && t.getUpdatedAt() != null
                        && t.getUpdatedAt().isAfter(sevenDaysAgo)) {
                    doneTasks.add(t);
                } else if ("IN_PROGRESS".equals(status)) {
                    inProgressTasks.add(t);
                } else if (!"DONE".equals(status) && t.getDueDate() != null
                        && t.getDueDate().isBefore(today)) {
                    overdueTasks.add(t);
                }
            }

            log.info("[AI-Summary] 数据分类 — 已完成:{} | 进行中:{} | 逾期:{}",
                    doneTasks.size(), inProgressTasks.size(), overdueTasks.size());

            // 无任务数据时，生成项目启动寄语而非硬编码文案
            if (doneTasks.isEmpty() && inProgressTasks.isEmpty() && overdueTasks.isEmpty()) {
                log.info("[AI-Summary] 无任务数据，调用 AI 生成项目启动寄语");
                String kickoffPrompt = buildKickoffPrompt(project);
                return aiService.streamChat(kickoffPrompt);
            }

            String prompt = buildSummaryPrompt(project.getName(), doneTasks, inProgressTasks, overdueTasks);
            log.info("[AI-Summary] Prompt 长度: {} 字符，开始调用 AI...", prompt.length());
            return aiService.streamChat(prompt);

        } catch (Exception e) {
            log.error("[AI-Summary] 生成项目总结失败，具体原因为：", e);
            return Flux.error(e);
        }
    }

    /**
     * 无历史数据时的项目启动寄语 Prompt。
     * 调用 AI 生成专业、激励性的启动文案和开发规划。
     */
    private String buildKickoffPrompt(Project project) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个资深的项目经理和敏捷教练。\n\n");
        sb.append("你面前有一个刚刚启动的全新项目，目前还没有任何历史完工记录。\n");
        sb.append("请为团队撰写一份高质量的项目启动文档（Markdown 格式），包含以下内容：\n\n");
        sb.append("【项目名称】").append(project.getName()).append("\n");
        if (project.getDescription() != null && !project.getDescription().isBlank()) {
            sb.append("【项目描述】").append(project.getDescription()).append("\n");
        }
        sb.append("\n");
        sb.append("【输出要求】\n");
        sb.append("1. 项目启动寄语：一段温暖又充满力量的开场白，欢迎团队开启新项目\n");
        sb.append("2. 核心目标展望：根据项目名称和描述，推测并列出 3-5 个关键目标\n");
        sb.append("3. 推荐的开发规划：按阶段给出合理的开发路线图（建议 3-4 个阶段，每阶段给出关键任务建议）\n");
        sb.append("4. 敏捷实践建议：给出团队协作、站会节奏、看板使用等方面的实用建议\n");
        sb.append("5. 结尾金句：一句鼓舞人心的名言或团队格言\n\n");
        sb.append("【格式要求】\n");
        sb.append("- 使用专业的 Markdown 格式，层次分明\n");
        sb.append("- 总字数控制在 500-800 字\n");
        sb.append("- 语气专业但不失亲切，适合在团队内部会议上展示\n");
        sb.append("- 直接输出 Markdown，不要加任何开场白或结束语");
        return sb.toString();
    }

    /**
     * 构造项目周报 Prompt。
     */
    private String buildSummaryPrompt(String projectName,
                                      List<Task> doneTasks,
                                      List<Task> inProgressTasks,
                                      List<Task> overdueTasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个专业的项目经理。请根据以下项目数据，生成一份简洁、专业的项目周报（Markdown格式）。\n\n");
        sb.append("【项目名称】").append(projectName).append("\n\n");

        if (!doneTasks.isEmpty()) {
            sb.append("## 本周已完成的任务\n");
            for (Task t : doneTasks) {
                if (t == null) continue;
                sb.append("- ").append(t.getTitle());
                if (t.getDescription() != null && !t.getDescription().isBlank()) {
                    sb.append("（").append(t.getDescription()).append("）");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (!inProgressTasks.isEmpty()) {
            sb.append("## 进行中的任务\n");
            for (Task t : inProgressTasks) {
                if (t == null) continue;
                sb.append("- ").append(t.getTitle());
                if (t.getDueDate() != null) {
                    sb.append(" | 截止: ").append(t.getDueDate());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (!overdueTasks.isEmpty()) {
            sb.append("## 已逾期的任务\n");
            for (Task t : overdueTasks) {
                if (t == null) continue;
                sb.append("- ").append(t.getTitle());
                if (t.getDueDate() != null) {
                    sb.append(" | 截止: ").append(t.getDueDate());
                }
                sb.append(" | 当前状态: ")
                        .append("IN_PROGRESS".equals(t.getStatus()) ? "进行中" : "待办");
                sb.append("\n");
            }
            sb.append("\n");
        }

        sb.append("【周报要求】\n");
        sb.append("1. 使用 Markdown 格式，包含：总体概述、本周成果、进行中工作、风险与逾期提醒、下周展望\n");
        sb.append("2. 语言专业但不过于正式，适合团队内部传阅\n");
        sb.append("3. 对于逾期的任务要温和地指出风险\n");
        sb.append("4. 字数控制在500字以内，重点突出\n");
        sb.append("5. 直接输出 Markdown，不要加任何开场白或结束语");
        return sb.toString();
    }
}
