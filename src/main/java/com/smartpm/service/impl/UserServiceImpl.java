package com.smartpm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartpm.common.exception.BusinessException;
import com.smartpm.common.utils.JWTUtil;
import com.smartpm.common.utils.UserHolder;
import com.smartpm.entity.User;
import com.smartpm.mapper.UserMapper;
import com.smartpm.service.UserService;
import com.smartpm.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final Set<String> VALID_IDENTITIES = Set.of(
            "PROJECT_MANAGER", "FRONTEND_DEV", "BACKEND_DEV", "QA_TESTER", "UI_DESIGNER"
    );

    @Override
    public User register(String username, String password, String nickname, String identity) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        if (identity != null && !identity.isBlank()) {
            String upper = identity.toUpperCase();
            if (!VALID_IDENTITIES.contains(upper)) {
                throw new BusinessException("无效的专业身份，可选值: " + String.join(", ", VALID_IDENTITIES));
            }
            identity = upper;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null ? nickname : username);
        user.setIdentity(identity);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);
        return user;
    }

    @Override
    public LoginVO login(String username, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = JWTUtil.generate(user.getId(), user.getUsername());
        return new LoginVO(token, user.getId(), user.getUsername(), user.getIdentity());
    }

    @Override
    public void updateIdentity(Long userId, String identity) {
        if (identity == null || identity.isBlank()) {
            throw new BusinessException("专业身份不能为空");
        }
        String upper = identity.toUpperCase();
        if (!VALID_IDENTITIES.contains(upper)) {
            throw new BusinessException("无效的专业身份，可选值: " + String.join(", ", VALID_IDENTITIES));
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setIdentity(upper);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
