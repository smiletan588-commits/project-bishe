package com.smartpm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartpm.common.exception.BusinessException;
import com.smartpm.common.utils.UserHolder;
import com.smartpm.entity.Project;
import com.smartpm.entity.Wiki;
import com.smartpm.mapper.ProjectMapper;
import com.smartpm.mapper.WikiMapper;
import com.smartpm.service.AIService;
import com.smartpm.service.ProjectService;
import com.smartpm.service.WikiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiServiceImpl implements WikiService {

    private final WikiMapper wikiMapper;
    private final ProjectMapper projectMapper;
    private final AIService aiService;
    private final ProjectService projectService;

    @Override
    public Wiki create(Long projectId, String title, String content) {
        projectService.assertProjectAccess(projectId, true);
        if (title == null || title.isBlank()) {
            throw new BusinessException("文档标题不能为空");
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }

        Wiki wiki = new Wiki();
        wiki.setProjectId(projectId);
        wiki.setTitle(title);
        wiki.setContent(content != null ? content : "");
        wiki.setCreatorId(UserHolder.getUserId());
        wiki.setCreateTime(LocalDateTime.now());
        wiki.setUpdateTime(LocalDateTime.now());

        wikiMapper.insert(wiki);
        return wiki;
    }

    @Override
    public List<Map<String, Object>> listByProject(Long projectId) {
        projectService.assertProjectAccess(projectId, false);
        List<Wiki> wikis = wikiMapper.selectList(
                new LambdaQueryWrapper<Wiki>()
                        .eq(Wiki::getProjectId, projectId)
                        .isNull(Wiki::getDeletedAt)
                        .orderByDesc(Wiki::getUpdateTime));

        return wikis.stream().map(w -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", w.getId());
            m.put("title", w.getTitle());
            m.put("updateTime", w.getUpdateTime());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public Wiki getById(Long id) {
        Wiki wiki = wikiMapper.selectById(id);
        if (wiki == null || wiki.getDeletedAt() != null) {
            throw new BusinessException("文档不存在");
        }
        projectService.assertProjectAccess(wiki.getProjectId(), false);
        return wiki;
    }

    @Override
    public Wiki update(Long id, String title, String content) {
        Wiki wiki = wikiMapper.selectById(id);
        if (wiki == null || wiki.getDeletedAt() != null) {
            throw new BusinessException("文档不存在");
        }
        projectService.assertProjectAccess(wiki.getProjectId(), true);
        if (title != null && !title.isBlank()) {
            wiki.setTitle(title);
        }
        if (content != null) {
            wiki.setContent(content);
        }
        wiki.setUpdateTime(LocalDateTime.now());
        wikiMapper.updateById(wiki);
        return wiki;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Wiki wiki = wikiMapper.selectById(id);
        if (wiki == null || wiki.getDeletedAt() != null) {
            throw new BusinessException("文档不存在");
        }
        projectService.assertProjectAccess(wiki.getProjectId(), true);
        wiki.setDeletedAt(LocalDateTime.now());
        wiki.setDeletedBy(UserHolder.getUserId());
        wiki.setUpdateTime(LocalDateTime.now());
        wikiMapper.updateById(wiki);
    }

    @Override
    public Flux<String> aiCopilot(String prompt, String text) {
        if (prompt == null || prompt.isBlank()) {
            throw new BusinessException("AI 指令不能为空");
        }
        if (text == null || text.isBlank()) {
            throw new BusinessException("待处理的文本不能为空");
        }

        String fullPrompt = buildCopilotPrompt(prompt, text);
        log.info("[AI-Copilot] Prompt 长度: {} 字符，开始调用 AI...", fullPrompt.length());
        return aiService.streamChat(fullPrompt);
    }

    private String buildCopilotPrompt(String userInstruction, String originalText) {
        return "作为一名资深的文档编辑，请根据用户指令【" + userInstruction +
                "】，对以下文本进行处理，直接输出处理后的结果，不要带多余的解释：\n\n" + originalText;
    }
}
