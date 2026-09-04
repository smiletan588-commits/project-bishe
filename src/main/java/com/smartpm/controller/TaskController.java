package com.smartpm.controller;

import com.smartpm.common.result.R;
import com.smartpm.common.websocket.TaskWebSocketHandler;
import com.smartpm.dto.DragDTO;
import com.smartpm.dto.TaskUpdateDTO;
import com.smartpm.dto.AITaskOptimizationVO;
import com.smartpm.entity.AttachmentDownloadLog;
import com.smartpm.entity.Task;
import com.smartpm.entity.TaskAttachment;
import com.smartpm.mapper.TaskMapper;
import com.smartpm.service.TaskAttachmentService;
import com.smartpm.service.TaskService;
import com.smartpm.service.AIPlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final AIPlanningService aiPlanningService;
    private final TaskAttachmentService attachmentService;
    private final TaskMapper taskMapper;
    private final TaskWebSocketHandler wsHandler;

    @PostMapping("/create")
    public R<Task> create(@RequestParam Long projectId,
                          @RequestParam String title,
                          @RequestParam(required = false) String description,
                          @RequestParam(required = false) Long assigneeId,
                          @RequestParam(required = false) String dueDate,
                          @RequestParam(required = false) String startDate,
                          @RequestParam(required = false) String priority,
                          @RequestParam(required = false) String tags,
                          @RequestParam(required = false) String dependencyIds,
                          @RequestParam(required = false) Integer estimatedHours,
                          @RequestParam(required = false) Integer actualHours,
                          @RequestParam(required = false) String acceptanceCriteria) {
        Task task = taskService.create(projectId, title, description, assigneeId, dueDate,
                startDate, priority, tags, dependencyIds, estimatedHours, actualHours, acceptanceCriteria);
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

    @PostMapping("/{taskId}/ai-optimize")
    public R<AITaskOptimizationVO> aiOptimize(@PathVariable Long taskId) {
        return R.ok(aiPlanningService.optimizeTask(taskId));
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

    @GetMapping("/{taskId}/attachments")
    public R<List<TaskAttachment>> listAttachments(@PathVariable Long taskId) {
        return R.ok(attachmentService.list(taskId));
    }

    @PostMapping(value = "/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<TaskAttachment> uploadAttachment(@PathVariable Long taskId, @RequestParam("file") MultipartFile file) {
        TaskAttachment attachment = attachmentService.upload(taskId, file);
        wsHandler.broadcast(attachment.getProjectId(), "{\"type\":\"TASK_UPDATED\"}");
        return R.ok(attachment);
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<FileSystemResource> downloadAttachment(@PathVariable Long attachmentId) {
        TaskAttachment attachment = attachmentService.get(attachmentId);
        Path path = attachmentService.download(attachmentId);
        MediaType mediaType;
        try {
            mediaType = attachment.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(attachment.getContentType());
        } catch (IllegalArgumentException ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(path.toFile().length())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(attachment.getOriginalName(), StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(path));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public R<Void> deleteAttachment(@PathVariable Long attachmentId) {
        Long projectId = attachmentService.get(attachmentId).getProjectId();
        attachmentService.delete(attachmentId);
        wsHandler.broadcast(projectId, "{\"type\":\"TASK_UPDATED\"}");
        return R.ok();
    }

    @GetMapping("/attachments/{attachmentId}/download-logs")
    public R<List<AttachmentDownloadLog>> attachmentDownloadLogs(@PathVariable Long attachmentId) {
        return R.ok(attachmentService.listDownloadLogs(attachmentId));
    }

    /** 根据 taskId 获取所属 projectId */
    private Long getProjectId(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        return task != null ? task.getProjectId() : null;
    }
}
