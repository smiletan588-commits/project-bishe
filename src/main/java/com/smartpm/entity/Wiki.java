package com.smartpm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_wiki")
public class Wiki {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String title;

    private String content;

    private Long creatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private LocalDateTime deletedAt;

    private Long deletedBy;
}
