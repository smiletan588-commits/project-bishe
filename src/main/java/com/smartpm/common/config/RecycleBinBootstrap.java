package com.smartpm.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class RecycleBinBootstrap {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    ApplicationRunner initializeRecycleBinFields() {
        return args -> {
            addColumnIfMissing("ALTER TABLE sys_project ADD COLUMN deleted_at DATETIME NULL COMMENT '移入回收站时间'");
            addColumnIfMissing("ALTER TABLE sys_project ADD COLUMN deleted_by BIGINT NULL COMMENT '删除人ID'");
            addColumnIfMissing("ALTER TABLE sys_task ADD COLUMN deleted_at DATETIME NULL COMMENT '移入回收站时间'");
            addColumnIfMissing("ALTER TABLE sys_task ADD COLUMN deleted_by BIGINT NULL COMMENT '删除人ID'");
            addColumnIfMissing("ALTER TABLE pm_wiki ADD COLUMN deleted_at DATETIME NULL COMMENT '移入回收站时间'");
            addColumnIfMissing("ALTER TABLE pm_wiki ADD COLUMN deleted_by BIGINT NULL COMMENT '删除人ID'");
            addColumnIfMissing("ALTER TABLE pm_task_attachment ADD COLUMN deleted_at DATETIME NULL COMMENT '移入回收站时间'");
            addColumnIfMissing("ALTER TABLE pm_task_attachment ADD COLUMN deleted_by BIGINT NULL COMMENT '删除人ID'");
        };
    }

    private void addColumnIfMissing(String sql) {
        try { jdbcTemplate.execute(sql); } catch (Exception ignored) { /* 字段已存在时可安全忽略 */ }
    }
}
