package com.smartpm.controller;

import com.smartpm.common.result.R;
import com.smartpm.common.websocket.TaskWebSocketHandler;
import com.smartpm.dto.DragDTO;
import com.smartpm.dto.TaskUpdateDTO;
import com.smartpm.entity.Task;
import com.smartpm.mapper.TaskMapper;
import com.smartpm.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;
    private final TaskWebSocketHandler wsHandler;

    @PostMapping("/create")
    public R<Task> create(@RequestParam Long projectId,
                          @RequestParam String title,
                          @RequestParam(required = false) String description,
                          @RequestParam(required = false) Long assigneeId,
                          @RequestParam(required = false) String dueDate) {
        Task task = taskService.create(projectId, title, description, assigneeId, dueDate);
        wsHandler.broadcast(projectId, "{\"type\":\"TASK_UPDATED\"}");
        return R.ok(task);
    }

    @GetMapping("/list/{projectId}")
    public R<List<Task>> listByProject(@PathVariable Long projectId) {
        List<Task> tasks = taskService.listByProject(projectId);
        return R.ok(tasks);
    }

    @PutMapping("/update")
    public R<Task> update(@RequestBody TaskUpdateDTO dto) {
        Task task = taskService.update(dto);
        wsHandler.broadcast(task.getProjectId(), "{\"type\":\"TASK_UPDATED\"}");
        return R.ok(task);
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        Long projectId = getProjectId(id);
        taskService.delete(id);
        wsHandler.broadcast(projectId, "{\"type\":\"TASK_UPDATED\"}");
        return R.ok();
    }

    @PutMapping("/drag")
    public R<Void> drag(@RequestBody DragDTO dto) {
        Long projectId = getProjectId(dto.getTaskId());
        taskService.drag(dto);
        wsHandler.broadcast(projectId, "{\"type\":\"TASK_UPDATED\"}");
        return R.ok();
    }

    @PostMapping("/{taskId}/ai-decompose")
    public R<List<Task>> aiDecompose(@PathVariable Long taskId) {
        List<Task> subtasks = taskService.decomposeTask(taskId);
        wsHandler.broadcast(getProjectId(taskId), "{\"type\":\"TASK_UPDATED\"}");
        return R.ok(subtasks);
    }

    @GetMapping("/{taskId}/subtasks")
    public R<List<Task>> listSubtasks(@PathVariable Long taskId) {
        return R.ok(taskService.listSubtasks(taskId));
    }

    @PutMapping("/{taskId}/toggle-subtask")
    public R<Task> toggleSubtask(@PathVariable Long taskId) {
        Task task = taskService.toggleSubtask(taskId);
        wsHandler.broadcast(task.getProjectId(), "{\"type\":\"TASK_UPDATED\"}");
        return R.ok(task);
    }

    @PostMapping("/{projectId}/ai-init-tasks")
    public R<List<Task>> aiInitTasks(@PathVariable Long projectId) {
        List<Task> tasks = taskService.initTasks(projectId);
        wsHandler.broadcast(projectId, "{\"type\":\"TASK_UPDATED\"}");
        return R.ok(tasks);
    }

    /** 根据 taskId 获取所属 projectId */
    private Long getProjectId(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        return task != null ? task.getProjectId() : null;
    }
}
