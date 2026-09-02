package com.smartpm.service;

import com.smartpm.entity.Project;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ProjectService {

    Project create(String name, String description);

    List<Project> list();

    Project update(Long id, String name, String description);

    void delete(Long id);

    /** 校验当前用户是否属于项目，并按需校验是否具备写权限。 */
    void assertProjectAccess(Long projectId, boolean write);

    boolean canManageMembers(Long projectId);

    Project getByIdForAccess(Long projectId);

    void inviteMember(Long projectId, String username, String identity, String permission);

    void updateMember(Long projectId, Long userId, String identity, String permission);

    void removeMember(Long projectId, Long userId);

    void transferOwner(Long projectId, Long userId);

    /**
     * 生成项目周报：查询近7天完成/进行中/逾期的任务，
     * 组织为 Prompt 后流式调用 AI，返回 Markdown 格式的总结。
     */
    Flux<String> generateSummary(Long projectId);
}
