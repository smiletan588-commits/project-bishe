-- ============================================================
-- SmartPM 测试数据脚本
-- 体验账号: demo / 123456
-- 3个项目 + 22条主任务 + 16条子任务
-- 覆盖: 看板拖拽 / 任务层级 / AI拆解 / 项目周报 / 统计大屏
-- 使用方法:
--   mysql -uroot -p123456 < sql/test_data.sql
-- ============================================================

USE smartpm;

-- ============ 1. 测试用户 ============
INSERT IGNORE INTO sys_user (username, password, nickname, created_at, updated_at)
VALUES ('demo', '$2a$12$f/HzKdp1mdsbtm4Gm6FfRuFDIUM9P2xAUNltxwwIy9aHw58yWQWGm', '体验用户', NOW(), NOW());

SET @uid = (SELECT id FROM sys_user WHERE username = 'demo');

-- ============ 2. 三个示例项目 ============
INSERT INTO sys_project (name, description, creator_id, created_at, updated_at) VALUES
('SmartPM 核心功能开发', '智能项目管理平台的核心功能迭代，包含看板拖拽、AI 拆解、项目周报等模块', @uid, NOW(), NOW());
SET @p1 = LAST_INSERT_ID();

INSERT INTO sys_project (name, description, creator_id, created_at, updated_at) VALUES
('移动端适配优化', '对 SmartPM 进行移动端响应式适配，优化触屏交互体验和小屏布局', @uid, NOW(), NOW());
SET @p2 = LAST_INSERT_ID();

INSERT INTO sys_project (name, description, creator_id, created_at, updated_at) VALUES
('数据分析平台', '构建项目数据可视化大屏，提供多维度统计图表和趋势分析', @uid, NOW(), NOW());
SET @p3 = LAST_INSERT_ID();

-- ============ 3. 项目1 主任务 (12条) ============
INSERT INTO sys_task (parent_id, project_id, title, description, status, assignee_id, creator_id, due_date, order_index, created_at, updated_at) VALUES
(NULL, @p1, '搭建项目基础架构',    'Spring Boot 3 + Vue 3 项目骨架搭建', 'DONE', NULL, @uid, '2026-07-03', 0, '2026-06-28 10:00:00', '2026-06-30 14:00:00'),
(NULL, @p1, '实现用户注册登录',    'BCrypt加密 + JWT令牌 + 登录拦截器', 'DONE', NULL, @uid, '2026-07-03', 1, '2026-06-29 09:00:00', '2026-07-01 11:20:00'),
(NULL, @p1, '实现看板拖拽排序',    'vuedraggable 三列拖拽 + WebSocket 实时同步', 'DONE', NULL, @uid, '2026-07-04', 2, '2026-07-01 14:00:00', '2026-07-02 18:45:00'),
(NULL, @p1, '接入 AI 大模型',      '对接 DeepSeek API，实现流式对话和 SSE 推送', 'DONE', NULL, @uid, '2026-07-05', 3, '2026-07-02 10:00:00', '2026-07-03 09:15:00'),
(NULL, @p1, 'AI 任务拆解功能',     '调用大模型将主任务拆解为 3-5 个子任务', 'DONE', NULL, @uid, '2026-07-05', 4, '2026-07-02 15:00:00', '2026-07-04 14:30:00'),
(NULL, @p1, 'AI 项目周报生成',     'SSE 流式生成 Markdown 周报，含本周成果和风险提醒', 'DONE', NULL, @uid, '2026-07-06', 5, '2026-07-03 08:00:00', '2026-07-05 10:00:00'),
(NULL, @p1, '数据统计大屏',        'ECharts 图表：项目进度、任务趋势、状态分布', 'IN_PROGRESS', NULL, @uid, '2026-07-08', 6, '2026-07-04 09:00:00', NOW()),
(NULL, @p1, 'Docker 部署方案',      'Dockerfile + docker-compose 一键部署', 'IN_PROGRESS', NULL, @uid, '2026-07-09', 7, '2026-07-03 11:00:00', NOW()),
(NULL, @p1, '编写单元测试',        '覆盖 Service 层核心逻辑，目标覆盖率 80%', 'TODO', NULL, @uid, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 8, NOW(), NOW()),
(NULL, @p1, '性能优化与压测',      '索引优化 + N+1 查询消除 + JMeter 压测', 'TODO', NULL, @uid, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 9, NOW(), NOW()),
(NULL, @p1, '用户权限与角色管理',  'RBAC 权限模型：管理员/普通用户角色', 'TODO', NULL, @uid, '2026-07-18', 10, NOW(), NOW()),
(NULL, @p1, '国际化多语言支持',    '前后端 i18n，首期中英文切换', 'TODO', NULL, @uid, NULL, 11, NOW(), NOW());

-- ============ 4. 项目1 子任务 ============

-- 子任务: 实现看板拖拽排序 (通过标题查ID)
SET @t_drag = (SELECT id FROM sys_task WHERE title = '实现看板拖拽排序' AND project_id = @p1 LIMIT 1);
INSERT INTO sys_task (parent_id, project_id, title, description, status, assignee_id, creator_id, due_date, order_index, created_at, updated_at) VALUES
(@t_drag, @p1, '设计看板三列布局',       'TODO/IN_PROGRESS/DONE 三列 UI 结构与响应式断点', 'DONE', NULL, @uid, NULL, 0, NOW(), NOW()),
(@t_drag, @p1, '集成 vuedraggable 组件',  '安装 vuedraggable@next 并实现卡片拖拽交互', 'DONE', NULL, @uid, NULL, 1, NOW(), NOW()),
(@t_drag, @p1, '实现拖拽排序算法',       '@Transactional 保证同列/跨列排序原子性', 'DONE', NULL, @uid, NULL, 2, NOW(), NOW()),
(@t_drag, @p1, 'WebSocket 实时广播',     '多用户拖拽时通知其他客户端自动刷新', 'DONE', NULL, @uid, NULL, 3, NOW(), NOW());

-- 子任务: 接入 AI 大模型
SET @t_ai = (SELECT id FROM sys_task WHERE title = '接入 AI 大模型' AND project_id = @p1 LIMIT 1);
INSERT INTO sys_task (parent_id, project_id, title, description, status, assignee_id, creator_id, due_date, order_index, created_at, updated_at) VALUES
(@t_ai, @p1, '配置 WebClient Bean',      'AIWebClientConfig: base-url + api-key 注入', 'DONE', NULL, @uid, NULL, 0, NOW(), NOW()),
(@t_ai, @p1, '实现流式对话接口',         'Flux<String> streamChat() — bodyToFlux 解析 SSE', 'DONE', NULL, @uid, NULL, 1, NOW(), NOW()),
(@t_ai, @p1, '编写 SseEmitter 控制器',   'Controller 层将 Flux 逐 token 推送到前端', 'DONE', NULL, @uid, NULL, 2, NOW(), NOW()),
(@t_ai, @p1, 'SSE 格式兼容性修复',       '兼容 data: 和 data: (有/无空格)两种 SSE 格式', 'DONE', NULL, @uid, NULL, 3, NOW(), NOW()),
(@t_ai, @p1, 'API 错误重试机制',         '调用失败自动重试最多 3 次，含指数退避', 'TODO', NULL, @uid, NULL, 4, NOW(), NOW());

-- 子任务: 数据统计大屏
SET @t_chart = (SELECT id FROM sys_task WHERE title = '数据统计大屏' AND project_id = @p1 LIMIT 1);
INSERT INTO sys_task (parent_id, project_id, title, description, status, assignee_id, creator_id, due_date, order_index, created_at, updated_at) VALUES
(@t_chart, @p1, '设计后端统计接口',      'GET /api/analytics/overview 数据结构设计', 'DONE', NULL, @uid, NULL, 0, NOW(), NOW()),
(@t_chart, @p1, '实现统计 Service 层',   '任务状态分布 + 项目排行 + 7天趋势查询', 'DONE', NULL, @uid, NULL, 1, NOW(), NOW()),
(@t_chart, @p1, 'ECharts 图表集成',      '饼图/柱状图/折线图布局与品牌配色', 'IN_PROGRESS', NULL, @uid, NULL, 2, NOW(), NOW()),
(@t_chart, @p1, '移动端响应式适配',      '小屏自动切换为纵向堆叠布局', 'TODO', NULL, @uid, NULL, 3, NOW(), NOW());

-- ============ 5. 项目2 主任务 (6条) ============
INSERT INTO sys_task (parent_id, project_id, title, description, status, assignee_id, creator_id, due_date, order_index, created_at, updated_at) VALUES
(NULL, @p2, '首页响应式重构',        'Dashboard 卡片网格移动端自动单列布局', 'DONE', NULL, @uid, '2026-07-03', 0, '2026-07-01 09:00:00', '2026-07-03 17:00:00'),
(NULL, @p2, '看板触屏拖拽优化',      '优化 vuedraggable 在触屏设备上的拖拽灵敏度', 'DONE', NULL, @uid, '2026-07-05', 1, '2026-07-02 10:00:00', '2026-07-05 15:30:00'),
(NULL, @p2, '弹窗与表单适配',        'Dialog/DatePicker/Select 小屏弹出位置优化', 'IN_PROGRESS', NULL, @uid, '2026-07-08', 2, '2026-07-04 14:00:00', NOW()),
(NULL, @p2, '底部导航栏设计',        '移动端底部 TabBar 替代顶部导航', 'TODO', NULL, @uid, '2026-07-10', 3, NOW(), NOW()),
(NULL, @p2, '手势操作支持',          '列表项左滑删除 + 长按拖拽', 'TODO', NULL, @uid, '2026-07-12', 4, NOW(), NOW()),
(NULL, @p2, '离线缓存与 PWA',        'Service Worker + 离线可用 + 添加到主屏幕', 'TODO', NULL, @uid, NULL, 5, NOW(), NOW());

-- 项目2子任务: 看板触屏拖拽优化
SET @t_touch = (SELECT id FROM sys_task WHERE title = '看板触屏拖拽优化' AND project_id = @p2 LIMIT 1);
INSERT INTO sys_task (parent_id, project_id, title, description, status, assignee_id, creator_id, due_date, order_index, created_at, updated_at) VALUES
(@t_touch, @p2, '调研 SortableJS 触屏配置',  'delay / touchStartThreshold 等参数', 'DONE', NULL, @uid, NULL, 0, NOW(), NOW()),
(@t_touch, @p2, '添加触屏视觉反馈',          '拖拽时卡片放大 + 阴影增强', 'DONE', NULL, @uid, NULL, 1, NOW(), NOW()),
(@t_touch, @p2, '多浏览器兼容性测试',        'Safari / Chrome / 微信内置浏览器', 'TODO', NULL, @uid, NULL, 2, NOW(), NOW());

-- ============ 6. 项目3 主任务 (4条) ============
INSERT INTO sys_task (parent_id, project_id, title, description, status, assignee_id, creator_id, due_date, order_index, created_at, updated_at) VALUES
(NULL, @p3, '数据统计接口设计',      'RESTful API: 项目/任务多维度统计', 'DONE', NULL, @uid, '2026-07-04', 0, '2026-07-02 09:00:00', '2026-07-04 16:00:00'),
(NULL, @p3, '前端图表页面开发',      'ECharts 实现 Analytics.vue 数据大屏', 'DONE', NULL, @uid, '2026-07-06', 1, '2026-07-05 10:00:00', '2026-07-06 11:00:00'),
(NULL, @p3, '实时数据刷新机制',      'WebSocket 推送 + 自动更新统计大屏', 'IN_PROGRESS', NULL, @uid, '2026-07-09', 2, NOW(), NOW()),
(NULL, @p3, '导出报表功能',          'Excel/PDF 导出 + 邮件定时发送', 'TODO', NULL, @uid, '2026-07-14', 3, NOW(), NOW());

-- ============ 7. 验证 ============
SELECT '========================================' AS '';
SELECT '  测试数据创建完成！' AS '';
SELECT '  登录账号: demo / 123456' AS '';
SELECT '========================================' AS '';
SELECT
  (SELECT COUNT(*) FROM sys_user WHERE username = 'demo') AS 用户数,
  (SELECT COUNT(*) FROM sys_project WHERE creator_id = @uid) AS 项目数,
  (SELECT COUNT(*) FROM sys_task WHERE project_id IN (SELECT id FROM sys_project WHERE creator_id = @uid)) AS 任务总数,
  (SELECT COUNT(*) FROM sys_task WHERE project_id IN (SELECT id FROM sys_project WHERE creator_id = @uid) AND parent_id IS NULL) AS 主任务,
  (SELECT COUNT(*) FROM sys_task WHERE project_id IN (SELECT id FROM sys_project WHERE creator_id = @uid) AND parent_id IS NOT NULL) AS 子任务;
