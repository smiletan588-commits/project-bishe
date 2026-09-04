package com.smartpm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String inviteCode;

    private Long creatorId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 非空表示已移入回收站。 */
    private LocalDateTime deletedAt;

    private Long deletedBy;
}
