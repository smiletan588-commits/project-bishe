package com.smartpm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    /** 专业身份：PROJECT_MANAGER / FRONTEND_DEV / BACKEND_DEV / QA_TESTER / UI_DESIGNER */
    private String identity;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
