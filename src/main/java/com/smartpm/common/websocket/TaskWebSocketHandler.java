package com.smartpm.common.websocket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartpm.entity.Project;
import com.smartpm.entity.ProjectMember;
import com.smartpm.mapper.ProjectMapper;
import com.smartpm.mapper.ProjectMemberMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 按 projectId 分组的 WebSocket 广播处理器。
 * <p>
 * 连接路径：/ws/project/{projectId}
 * 客户端连接后即被归入对应项目的广播组。
 * 当该项目下发生任务变更时，Controller 调用 {@link #broadcast(Long, String)}
 * 向组内所有在线 Session 推送消息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskWebSocketHandler extends TextWebSocketHandler {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;

    /** projectId → 该项目的所有在线 WebSocket 会话 */
    private final Map<Long, Set<WebSocketSession>> projectSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long projectId = extractProjectId(session);
        if (projectId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        Long userId = (Long) session.getAttributes().get("userId");
        Project project = projectMapper.selectById(projectId);
        boolean member = project != null && (project.getCreatorId().equals(userId)
                || projectMemberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getUserId, userId)) > 0);
        if (!member) {
            log.warn("WebSocket 拒绝非项目成员: projectId={}, userId={}", projectId, userId);
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        session.getAttributes().put("projectId", projectId);
        projectSessions.computeIfAbsent(projectId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("WebSocket 连接: projectId={}, sessionId={}, 当前在线={}",
                projectId, session.getId(), projectSessions.get(projectId).size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long projectId = (Long) session.getAttributes().get("projectId");
        if (projectId != null) {
            Set<WebSocketSession> sessions = projectSessions.get(projectId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    projectSessions.remove(projectId);
                }
            }
            log.info("WebSocket 断开: projectId={}, sessionId={}", projectId, session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 客户端上行消息暂不处理，功能由 REST API 驱动
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 传输异常: sessionId={}, error={}", session.getId(), exception.getMessage());
        Long projectId = (Long) session.getAttributes().get("projectId");
        if (projectId != null) {
            Set<WebSocketSession> sessions = projectSessions.get(projectId);
            if (sessions != null) {
                sessions.remove(session);
            }
        }
    }

    /**
     * 向指定项目下的所有在线客户端广播消息。
     *
     * @param projectId 目标项目 ID
     * @param message   JSON 格式的消息字符串
     */
    public void broadcast(Long projectId, String message) {
        Set<WebSocketSession> sessions = projectSessions.get(projectId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.warn("广播失败: sessionId={}, projectId={}", session.getId(), projectId);
                }
            }
        }
    }

    /** 从 URI /ws/project/{projectId} 中提取 projectId */
    private Long extractProjectId(WebSocketSession session) {
        String path = session.getUri().getPath();
        // path: /ws/project/123
        try {
            String[] parts = path.split("/");
            return Long.valueOf(parts[parts.length - 1]);
        } catch (Exception e) {
            log.warn("无法从 URI 解析 projectId: {}", path);
            return null;
        }
    }
}
