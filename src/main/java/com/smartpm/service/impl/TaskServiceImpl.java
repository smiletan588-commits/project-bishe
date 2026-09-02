package com.smartpm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartpm.common.exception.BusinessException;
import com.smartpm.common.utils.UserHolder;
import com.smartpm.dto.DragDTO;
import com.smartpm.dto.TaskUpdateDTO;
import com.smartpm.entity.Project;
import com.smartpm.entity.ProjectMember;
import com.smartpm.entity.Task;
import com.smartpm.entity.User;
import com.smartpm.mapper.ProjectMapper;
import com.smartpm.mapper.ProjectMemberMapper;
import com.smartpm.mapper.TaskMapper;
import com.smartpm.mapper.UserMapper;
import com.smartpm.service.AIService;
import com.smartpm.service.ProjectService;
import com.smartpm.service.TaskService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserMapper userMapper;
    private final AIService aiService;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── 创建主任务 ──

    @Override
    public Task create(Long projectId, String title, String description, Long assigneeId, String dueDate) {
        projectService.assertProjectAccess(projectId, true);
        if (title == null || title.isBlank()) {
            throw new BusinessException("任务标题不能为空");
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }

        Task task = new Task();
        task.setProjectId(projectId);
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus("TODO");
        task.setAssigneeId(assigneeId);
        if (dueDate != null && !dueDate.isBlank()) {
            task.setDueDate(LocalDate.parse(dueDate));
        }
        task.setCreatorId(UserHolder.getUserId());
        task.setOrderIndex(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        taskMapper.insert(task);
        return task;
    }

    // ── 查询看板任务（仅主任务，parent_id IS NULL）──

    @Override
    public List<Task> listByProject(Long projectId) {
        projectService.assertProjectAccess(projectId, false);
        return taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProjectId, projectId)
                        .isNull(Task::getParentId)
                        .orderByAsc(Task::getOrderIndex)
                        .orderByDesc(Task::getCreatedAt));
    }

    // ── 查询子任务列表 ──

    @Override
    public List<Task> listSubtasks(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        projectService.assertProjectAccess(task.getProjectId(), false);
        return taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getParentId, taskId)
                        .orderByAsc(Task::getCreatedAt));
    }

    // ── 更新任务 ──

    @Override
    public Task update(TaskUpdateDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("任务ID不能为空");
        }

        Task task = taskMapper.selectById(dto.getId());
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        projectService.assertProjectAccess(task.getProjectId(), true);

        if (dto.getTitle() != null) {
            task.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            task.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            String status = dto.getStatus().toUpperCase();
            if (!status.equals("TODO") && !status.equals("IN_PROGRESS") && !status.equals("DONE")) {
                throw new BusinessException("无效的任务状态: " + dto.getStatus());
            }
            task.setStatus(status);
        }
        if (dto.getAssigneeId() != null) {
            task.setAssigneeId(dto.getAssigneeId());
        }
        if (dto.getDueDate() != null) {
            task.setDueDate(dto.getDueDate().isBlank() ? null : LocalDate.parse(dto.getDueDate()));
        }
        if (dto.getOrderIndex() != null) {
            task.setOrderIndex(dto.getOrderIndex());
        }
        task.setUpdatedAt(LocalDateTime.now());

        taskMapper.updateById(task);
        return task;
    }

    // ── 拖拽排序（仅操作主任务，忽略子任务）──

    @Override
    @Transactional
    public void drag(DragDTO dto) {
        if (dto.getTaskId() == null) {
            throw new BusinessException("任务ID不能为空");
        }
        if (dto.getTargetStatus() == null || dto.getTargetStatus().isBlank()) {
            throw new BusinessException("目标状态不能为空");
        }

        String targetStatus = dto.getTargetStatus().toUpperCase();
        if (!targetStatus.equals("TODO") && !targetStatus.equals("IN_PROGRESS") && !targetStatus.equals("DONE")) {
            throw new BusinessException("无效的任务状态: " + dto.getTargetStatus());
        }

        Task task = taskMapper.selectById(dto.getTaskId());
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        Long projectId = task.getProjectId();
        projectService.assertProjectAccess(projectId, true);
        String sourceStatus = task.getStatus();
        int sourceOrder = task.getOrderIndex() != null ? task.getOrderIndex() : 0;

        // 目标列主任务数量（排除子任务）
        Long maxOrder = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProjectId, projectId)
                        .eq(Task::getStatus, targetStatus)
                        .isNull(Task::getParentId));
        int maxIndex = maxOrder.intValue();
        int targetOrder = dto.getTargetOrderIndex() != null ? dto.getTargetOrderIndex() : maxIndex;
        if (targetOrder < 0) targetOrder = 0;
        if (targetOrder > maxIndex) targetOrder = maxIndex;

        if (sourceStatus.equals(targetStatus)) {
            if (sourceOrder == targetOrder) return;

            if (sourceOrder < targetOrder) {
                taskMapper.update(null,
                        new LambdaUpdateWrapper<Task>()
                                .eq(Task::getProjectId, projectId)
                                .eq(Task::getStatus, sourceStatus)
                                .isNull(Task::getParentId)
                                .gt(Task::getOrderIndex, sourceOrder)
                                .le(Task::getOrderIndex, targetOrder)
                                .setSql("order_index = order_index - 1"));
            } else {
                taskMapper.update(null,
                        new LambdaUpdateWrapper<Task>()
                                .eq(Task::getProjectId, projectId)
                                .eq(Task::getStatus, sourceStatus)
                                .isNull(Task::getParentId)
                                .ge(Task::getOrderIndex, targetOrder)
                                .lt(Task::getOrderIndex, sourceOrder)
                                .setSql("order_index = order_index + 1"));
            }
        } else {
            taskMapper.update(null,
                    new LambdaUpdateWrapper<Task>()
                            .eq(Task::getProjectId, projectId)
                            .eq(Task::getStatus, sourceStatus)
                            .isNull(Task::getParentId)
                            .gt(Task::getOrderIndex, sourceOrder)
                            .setSql("order_index = order_index - 1"));

            taskMapper.update(null,
                    new LambdaUpdateWrapper<Task>()
                            .eq(Task::getProjectId, projectId)
                            .eq(Task::getStatus, targetStatus)
                            .isNull(Task::getParentId)
                            .ge(Task::getOrderIndex, targetOrder)
                            .setSql("order_index = order_index + 1"));
        }

        task.setStatus(targetStatus);
        task.setOrderIndex(targetOrder);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    // ── 删除任务（级联删除子任务）──

    @Override
    @Transactional
    public void delete(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        projectService.assertProjectAccess(task.getProjectId(), true);
        // 级联删除所有子任务
        taskMapper.delete(new LambdaQueryWrapper<Task>().eq(Task::getParentId, id));
        taskMapper.deleteById(id);
    }

    // ── 切换子任务完成状态 ──

    @Override
    public Task toggleSubtask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        projectService.assertProjectAccess(task.getProjectId(), true);
        if (task.getParentId() == null) {
            throw new BusinessException("该任务为主任务，不支持此操作");
        }
        String newStatus = "DONE".equals(task.getStatus()) ? "TODO" : "DONE";
        task.setStatus(newStatus);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        return task;
    }

    // ── AI 任务拆解 ──

    @Override
    @Transactional
    public List<Task> decomposeTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        projectService.assertProjectAccess(task.getProjectId(), true);

        // 查询项目成员及其专业身份
        List<ProjectMember> members = projectMemberMapper.selectList(
                new LambdaQueryWrapper<ProjectMember>()
                        .eq(ProjectMember::getProjectId, task.getProjectId()));
        List<Long> memberUserIds = members.stream().map(ProjectMember::getUserId).toList();
        Map<String, Long> identityUserMap = new java.util.HashMap<>();
        if (!memberUserIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(memberUserIds);
            for (User u : users) {
                if (u.getIdentity() != null && !u.getIdentity().isBlank()) {
                    identityUserMap.putIfAbsent(u.getIdentity(), u.getId());
                }
            }
        }

        String prompt = buildDecomposePrompt(task.getTitle(), task.getDescription());
        String aiResponse = aiService.chat(prompt);
        List<Map<String, String>> subtaskMaps = parseSubtaskJson(aiResponse);

        if (subtaskMaps.isEmpty()) {
            throw new BusinessException("AI 未能生成有效的子任务，请重试");
        }

        List<Task> created = new ArrayList<>();
        for (Map<String, String> st : subtaskMaps) {
            Task sub = new Task();
            sub.setProjectId(task.getProjectId());
            sub.setParentId(taskId);
            sub.setTitle(st.get("title"));
            sub.setDescription(st.getOrDefault("description", ""));
            sub.setStatus("TODO");
            sub.setCreatorId(UserHolder.getUserId());
            sub.setOrderIndex(0);
            String recommendedRole = st.get("recommended_role");
            sub.setRecommendedRole(recommendedRole);

            // 自动指派：按推荐角色匹配项目成员
            if (recommendedRole != null && identityUserMap.containsKey(recommendedRole)) {
                sub.setAssigneeId(identityUserMap.get(recommendedRole));
            }

            sub.setCreatedAt(LocalDateTime.now());
            sub.setUpdatedAt(LocalDateTime.now());
            taskMapper.insert(sub);
            created.add(sub);
        }
        return created;
    }

    /**
     * 构造拆解任务的 Prompt。
     * 关键约束：
     * 1. 子任务标题绝对不能与主任务标题相同或高度重叠
     * 2. 子任务必须是具体的技术性行动步骤，而非抽象描述
     * 3. 强调输出格式的严格性
     */
    private String buildDecomposePrompt(String title, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个资深技术项目经理。请将以下任务拆解为3-5个具体的、可独立执行的子任务。\n\n");
        sb.append("【任务标题】\n").append(title).append("\n\n");
        if (description != null && !description.isBlank()) {
            sb.append("【任务描述】\n").append(description).append("\n\n");
        }
        sb.append("【团队配置 — 请根据角色合理分配】\n");
        sb.append("系统支持以下五种专业身份，请在拆解时为每个子任务指定最合适的负责人角色：\n");
        sb.append("- PROJECT_MANAGER  (项目经理)：需求分析、流程规划、进度管控、风险管理\n");
        sb.append("- FRONTEND_DEV    (前端工程师)：UI实现、页面交互、组件开发、前端联调\n");
        sb.append("- BACKEND_DEV     (后端工程师)：数据库设计、API开发、业务逻辑、系统架构\n");
        sb.append("- QA_TESTER       (测试工程师)：测试用例、功能测试、回归测试、质量报告\n");
        sb.append("- UI_DESIGNER     (UI设计师)：视觉设计、交互原型、设计规范、品牌风格\n\n");
        sb.append("【拆解要求 — 请严格遵守】\n");
        sb.append("1. 每个子任务必须是具体的、技术层面的动作，而不是抽象的概念复述\n");
        sb.append("2. 子任务之间要有清晰的逻辑先后顺序（先设计→再开发→最后联调）\n");
        sb.append("3. 子任务数量控制在3-5个，宁少勿滥\n");
        sb.append("4. 【极其重要】子任务的标题绝对不能与主任务标题「").append(title).append("」相同或高度相似！\n");
        sb.append("5. 为每个子任务指定最合适的 recommended_role（必须从上述五种身份中选择）\n\n");
        sb.append("【输出格式】\n");
        sb.append("只返回一个JSON数组，不要加任何其他文字、解释或Markdown标记。每个元素必须包含 title、description、recommended_role 三个字段：\n");
        sb.append("[{\"title\": \"子任务标题\", \"description\": \"具体做什么\", \"recommended_role\": \"角色代码\"}]\n\n");
        sb.append("示例（主任务=实现用户登录）：\n");
        sb.append("[{\"title\": \"设计用户表结构\", \"description\": \"确定用户表的字段、索引和密码加密方案\", \"recommended_role\": \"BACKEND_DEV\"}, {\"title\": \"编写登录认证接口\", \"description\": \"实现POST /login接口，含参数校验和JWT签发\", \"recommended_role\": \"BACKEND_DEV\"}, {\"title\": \"实现前端登录页面\", \"description\": \"编写登录表单UI及前端表单验证逻辑\", \"recommended_role\": \"FRONTEND_DEV\"}, {\"title\": \"前后端联调登录流程\", \"description\": \"对接登录接口，处理token存储和异常情况\", \"recommended_role\": \"FRONTEND_DEV\"}]");
        return sb.toString();
    }

    /**
     * 从 AI 返回的文本中提取 JSON 数组并解析。
     */
    private List<Map<String, String>> parseSubtaskJson(String raw) {
        if (raw == null || raw.isBlank()) return List.of();

        Pattern fencePattern = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```");
        Matcher fenceMatcher = fencePattern.matcher(raw);
        if (fenceMatcher.find()) {
            raw = fenceMatcher.group(1).trim();
        }

        Pattern arrayPattern = Pattern.compile("\\[[\\s\\S]*\\]");
        Matcher arrayMatcher = arrayPattern.matcher(raw);
        if (!arrayMatcher.find()) {
            throw new BusinessException("AI 返回内容中未找到 JSON 数组，请重试");
        }

        String json = arrayMatcher.group();

        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            throw new BusinessException("AI 返回的 JSON 格式异常: " + e.getMessage());
        }
    }

    // ── AI 一键生成项目初始任务 ──

    @Override
    @Transactional
    public List<Task> initTasks(Long projectId) {
        projectService.assertProjectAccess(projectId, true);
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }

        // 检查是否已有任务
        Long existing = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProjectId, projectId)
                        .isNull(Task::getParentId));
        if (existing > 0) {
            throw new BusinessException("该项目已有任务，初始化仅适用于空项目");
        }

        String prompt = buildInitPrompt(project.getName(), project.getDescription());
        String aiResponse = aiService.chat(prompt);
        List<Map<String, String>> taskMaps = parseSubtaskJson(aiResponse);

        if (taskMaps.isEmpty()) {
            throw new BusinessException("AI 未能生成有效任务，请重试");
        }
        if (taskMaps.size() > 8) {
            throw new BusinessException("AI 生成任务过多，请重试");
        }

        List<Task> created = new ArrayList<>();
        int order = 0;
        for (Map<String, String> tm : taskMaps) {
            String title = tm.get("title");
            if (title == null || title.isBlank()) continue;
            Task task = new Task();
            task.setProjectId(projectId);
            task.setParentId(null);
            task.setTitle(title.trim());
            task.setDescription(tm.getOrDefault("description", ""));
            task.setStatus("TODO");
            task.setCreatorId(UserHolder.getUserId());
            task.setOrderIndex(order++);
            String recommendedRole = tm.get("recommended_role");
            task.setRecommendedRole(recommendedRole);
            task.setCreatedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.insert(task);
            created.add(task);
        }
        if (created.isEmpty()) {
            throw new BusinessException("AI 未生成有效的开发任务，请重试");
        }
        return created;
    }

    /** 根据项目成员的专业身份建立自动指派映射。 */
    private Map<String, Long> buildIdentityUserMap(Long projectId) {
        List<ProjectMember> members = projectMemberMapper.selectList(
                new LambdaQueryWrapper<ProjectMember>()
                        .eq(ProjectMember::getProjectId, projectId));
        Map<String, Long> identityUserMap = new java.util.HashMap<>();
        if (members.isEmpty()) return identityUserMap;

        List<Long> userIds = members.stream().map(ProjectMember::getUserId).toList();
        List<User> users = userMapper.selectBatchIds(userIds);
        for (User user : users) {
            if (user.getIdentity() != null && !user.getIdentity().isBlank()) {
                identityUserMap.putIfAbsent(user.getIdentity(), user.getId());
            }
        }
        return identityUserMap;
    }

    private String buildInitPrompt(String projectName, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个资深技术项目经理和敏捷教练。\n\n");
        sb.append("请分析以下项目，并为其制定一份核心开发路径，包含 3 到 5 个必须优先执行的阶段性大任务。\n\n");
        sb.append("【项目名称】\n").append(projectName).append("\n\n");
        if (description != null && !description.isBlank()) {
            sb.append("【项目描述】\n").append(description).append("\n\n");
        }
        sb.append("【任务规划要求 — 请严格遵守】\n");
        sb.append("1. 每个任务必须是独立完整的开发阶段，有明确的边界和可交付成果\n");
        sb.append("2. 任务之间按依赖关系排序（先基础设施→再核心功能→最后测试验收）\n");
        sb.append("3. 任务数量控制在 3-5 个，每个任务描述控制在 20-60 字\n");
        sb.append("4. 任务标题要简洁有力（8-16 字），一眼能看出要做什么\n");
        sb.append("5. 为每个任务推荐一个最合适的执行岗位：PROJECT_MANAGER、FRONTEND_DEV、BACKEND_DEV、QA_TESTER、UI_DESIGNER\n");
        sb.append("6. 这些是顶层大任务（父任务），后续可被 AI 进一步拆解为具体子任务\n\n");
        sb.append("【输出格式】\n");
        sb.append("只返回一个 JSON 数组，不要加任何其他文字：\n");
        sb.append("[{\"title\": \"任务标题\", \"description\": \"该阶段核心工作内容\", \"recommended_role\": \"BACKEND_DEV\"}]\n\n");
        sb.append("示例（项目=电商平台）：\n");
        sb.append("[{\"title\": \"设计账目数据模型\", \"description\": \"设计收入支出、分类、账户和时间字段，建立必要索引\", \"recommended_role\": \"BACKEND_DEV\"}, {\"title\": \"开发账目管理接口\", \"description\": \"实现账目新增、编辑、删除、查询及统计接口\", \"recommended_role\": \"BACKEND_DEV\"}, {\"title\": \"实现记账操作页面\", \"description\": \"开发记账表单、账目列表、筛选和移动端交互\", \"recommended_role\": \"FRONTEND_DEV\"}, {\"title\": \"设计数据统计界面\", \"description\": \"设计收支趋势、分类占比和余额概览的可视化界面\", \"recommended_role\": \"UI_DESIGNER\"}, {\"title\": \"完成联调与验收测试\", \"description\": \"覆盖核心记账流程、边界条件和权限场景，修复上线问题\", \"recommended_role\": \"QA_TESTER\"}]");
        return sb.toString();
    }
}
