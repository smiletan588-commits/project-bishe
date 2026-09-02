package com.smartpm.service;

import com.smartpm.entity.Project;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ProjectService {

    Project create(String name, String description);

    List<Project> list();

    Project update(Long id, String name, String description);

    void delete(Long id);

    /**
     * 生成项目周报：查询近7天完成/进行中/逾期的任务，
     * 组织为 Prompt 后流式调用 AI，返回 Markdown 格式的总结。
     */
    Flux<String> generateSummary(Long projectId);
}
