package com.smartpm.controller;

import com.smartpm.common.result.R;
import com.smartpm.common.websocket.TaskWebSocketHandler;
import com.smartpm.service.RecycleBinService;
import com.smartpm.vo.RecycleBinItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recycle-bin")
@RequiredArgsConstructor
public class RecycleBinController {
    private final RecycleBinService recycleBinService;
    private final TaskWebSocketHandler wsHandler;

    @GetMapping
    public R<List<RecycleBinItemVO>> list() { return R.ok(recycleBinService.list()); }

    @PutMapping("/{type}/{id}/restore")
    public R<Void> restore(@PathVariable String type, @PathVariable Long id) {
        Long projectId = recycleBinService.restore(type, id); wsHandler.broadcast(projectId, "{\"type\":\"TASK_UPDATED\"}"); return R.ok();
    }

    @DeleteMapping("/{type}/{id}/permanent")
    public R<Void> permanentlyDelete(@PathVariable String type, @PathVariable Long id) {
        Long projectId = recycleBinService.permanentlyDelete(type, id); wsHandler.broadcast(projectId, "{\"type\":\"TASK_UPDATED\"}"); return R.ok();
    }
}
