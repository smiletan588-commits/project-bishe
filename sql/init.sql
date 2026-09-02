-- 创建数据库
CREATE DATABASE IF NOT EXISTS smartpm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE smartpm;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(64)  NOT NULL COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    nickname    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    identity    VARCHAR(50)  DEFAULT NULL COMMENT '专业身份: PROJECT_MANAGER/FRONTEND_DEV/BACKEND_DEV/QA_TESTER/UI_DESIGNER',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 项目表
CREATE TABLE IF NOT EXISTS sys_project (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    name        VARCHAR(255) NOT NULL COMMENT '项目名称',
    description TEXT         DEFAULT NULL COMMENT '项目描述',
    creator_id  BIGINT       NOT NULL COMMENT '创建者ID',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 任务表
CREATE TABLE IF NOT EXISTS sys_task (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    parent_id   BIGINT       DEFAULT NULL COMMENT '父任务ID(NULL=主任务, 非NULL=子任务)',
    project_id  BIGINT       NOT NULL COMMENT '所属项目ID',
    title       VARCHAR(255) NOT NULL COMMENT '任务标题',
    description TEXT         DEFAULT NULL COMMENT '任务描述',
    status      VARCHAR(32)  NOT NULL DEFAULT 'TODO' COMMENT '状态: TODO/IN_PROGRESS/DONE',
    assignee_id BIGINT       DEFAULT NULL COMMENT '负责人ID',
    creator_id  BIGINT       NOT NULL COMMENT '创建者ID',
    due_date    DATE         DEFAULT NULL COMMENT '截止日期',
    order_index INT          NOT NULL DEFAULT 0 COMMENT '排序权重',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_project (project_id),
    INDEX idx_assignee (assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- 给任务表加 recommended_role 字段
ALTER TABLE sys_task ADD COLUMN IF NOT EXISTS recommended_role varchar(50) DEFAULT NULL COMMENT 'AI推荐角色' AFTER assignee_id;

-- 项目成员表
CREATE TABLE IF NOT EXISTS pm_project_member (
    id          BIGINT  NOT NULL AUTO_INCREMENT COMMENT '成员关联ID',
    project_id  BIGINT  NOT NULL COMMENT '项目ID',
    user_id     BIGINT  NOT NULL COMMENT '用户ID',
    identity    VARCHAR(50) DEFAULT NULL COMMENT '项目内身份',
    permission  VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT 'PROJECT_ADMIN/MEMBER/VIEWER',
    joined_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_user (project_id, user_id),
    INDEX idx_project (project_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员表';

-- 已有数据库升级：补充项目内身份与权限
ALTER TABLE pm_project_member ADD COLUMN IF NOT EXISTS identity VARCHAR(50) DEFAULT NULL COMMENT '项目内身份';
ALTER TABLE pm_project_member ADD COLUMN IF NOT EXISTS permission VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT 'PROJECT_ADMIN/MEMBER/VIEWER';
