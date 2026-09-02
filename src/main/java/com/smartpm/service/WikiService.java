package com.smartpm.service;

import com.smartpm.entity.Wiki;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface WikiService {

    Wiki create(Long projectId, String title, String content);

    List<Map<String, Object>> listByProject(Long projectId);

    Wiki getById(Long id);

    Wiki update(Long id, String title, String content);

    void delete(Long id);

    Flux<String> aiCopilot(String prompt, String text);
}
