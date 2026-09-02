package com.smartpm.controller;

import com.smartpm.common.result.R;
import com.smartpm.common.utils.UserHolder;
import com.smartpm.entity.User;
import com.smartpm.service.UserService;
import com.smartpm.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public R<User> register(@RequestParam String username,
                            @RequestParam String password,
                            @RequestParam(required = false) String nickname,
                            @RequestParam(required = false) String identity) {
        User user = userService.register(username, password, nickname, identity);
        user.setPassword(null);
        return R.ok(user);
    }

    @PostMapping("/login")
    public R<LoginVO> login(@RequestParam String username,
                            @RequestParam String password) {
        LoginVO vo = userService.login(username, password);
        return R.ok(vo);
    }

    @PutMapping("/identity")
    public R<Void> updateIdentity(@RequestParam String identity) {
        userService.updateIdentity(UserHolder.getUserId(), identity);
        return R.ok();
    }
}
