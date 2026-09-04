package com.smartpm.vo;

import com.smartpm.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVO {
    private Long id;
    private String username;
    private String nickname;
    private String identity;
    private String systemRole;
    private String status;
    private LocalDateTime createdAt;

    public static AdminUserVO from(User user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setIdentity(user.getIdentity());
        vo.setSystemRole(user.getSystemRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
