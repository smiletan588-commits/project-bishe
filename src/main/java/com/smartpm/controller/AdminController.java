package com.smartpm.controller;

import com.smartpm.common.result.R;
import com.smartpm.service.UserService;
import com.smartpm.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public R<List<AdminUserVO>> listUsers() {
        return R.ok(userService.listUsersForAdmin());
    }

    @PutMapping("/users/{userId}/status")
    public R<Void> updateStatus(@PathVariable Long userId, @RequestParam String status) {
        userService.updateUserStatus(userId, status);
        return R.ok();
    }

    @PutMapping("/users/{userId}/role")
    public R<Void> updateRole(@PathVariable Long userId, @RequestParam String systemRole) {
        userService.updateSystemRole(userId, systemRole);
        return R.ok();
    }

    @PutMapping("/users/{userId}/password")
    public R<Void> resetPassword(@PathVariable Long userId, @RequestParam String password) {
        userService.resetUserPassword(userId, password);
        return R.ok();
    }
}
