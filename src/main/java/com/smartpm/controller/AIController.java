package com.smartpm.controller;

import com.smartpm.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @GetMapping(value = "/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestParam String prompt) {
        log.info("[AI-Chat] 收到请求: promptLength={}", prompt.length());
        SseEmitter emitter = new SseEmitter(300_000L);

        aiService.streamChat(prompt).subscribe(
                chunk -> {
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (IOException e) {
                        log.warn("[AI-Chat] SSE 发送失败，客户端可能已断开", e);
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("[AI-Chat] 流处理异常: {}", error.getMessage(), error);
                    try {
                        emitter.send(SseEmitter.event()
                                .data("[ERROR] " + (error.getMessage() != null ? error.getMessage() : "未知错误")));
                    } catch (IOException ignored) {}
                    emitter.completeWithError(error);
                },
                () -> {
                    log.info("[AI-Chat] 流式响应完成");
                    emitter.complete();
                }
        );

        emitter.onTimeout(() -> {
            log.warn("[AI-Chat] SSE 超时");
            emitter.complete();
        });
        emitter.onError(err -> {
            log.error("[AI-Chat] SSE 连接异常", err);
        });

        return emitter;
    }
}
