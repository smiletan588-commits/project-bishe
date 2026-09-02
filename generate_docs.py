from docx import Document
from docx.shared import Inches, Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
import os

BASE_DIR = r"D:\Project Management\Project_Management"

def set_cell_shading(cell, color):
    shading_elm = cell._element.get_or_add_tcPr()
    shading = shading_elm.makeelement(qn('w:shd'), {
        qn('w:fill'): color,
        qn('w:val'): 'clear',
    })
    shading_elm.append(shading)

def add_styled_table(doc, headers, rows, col_widths=None):
    table = doc.add_table(rows=1 + len(rows), cols=len(headers))
    table.style = 'Table Grid'
    for i, header in enumerate(headers):
        cell = table.rows[0].cells[i]
        cell.text = header
        for p in cell.paragraphs:
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            for run in p.runs:
                run.bold = True
                run.font.size = Pt(10)
                run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
        set_cell_shading(cell, '6366F1')
    for r, row in enumerate(rows):
        for c, val in enumerate(row):
            cell = table.rows[r + 1].cells[c]
            cell.text = str(val)
            for p in cell.paragraphs:
                for run in p.runs:
                    run.font.size = Pt(9)
    if col_widths:
        for i, w in enumerate(col_widths):
            for row in table.rows:
                row.cells[i].width = Cm(w)
    return table


# ===================================================================
# 第一周周报（按日记录任务）
# ===================================================================
doc1 = Document()

style = doc1.styles['Normal']
style.font.name = '微软雅黑'
style.font.size = Pt(10.5)
style.paragraph_format.space_after = Pt(4)
style.paragraph_format.line_spacing = 1.35
style.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')

title = doc1.add_heading('SmartPM 智能协同看板系统 — 第一周周报', level=0)
title.alignment = WD_ALIGN_PARAGRAPH.CENTER
doc1.add_paragraph('周期：2026年6月29日 — 2026年7月3日', style='Normal').alignment = WD_ALIGN_PARAGRAPH.CENTER
doc1.add_paragraph('项目：智能项目管理与看板系统（SmartPM）', style='Normal').alignment = WD_ALIGN_PARAGRAPH.CENTER
doc1.add_paragraph('')

# ==================== 6月29日 ====================
doc1.add_heading('一、2026年6月29日（周一）— 项目初始化与用户认证模块', level=1)

doc1.add_heading('1. 项目基础类编写', level=2)
add_styled_table(doc1,
    ['序号', '任务', '产出文件', '说明'],
    [
        ['1', '统一返回结果封装', 'common/result/R.java', '泛型类，code/msg/data 三字段，静态工厂 ok()/error()'],
        ['2', '自定义业务异常', 'common/exception/BusinessException.java', '继承 RuntimeException，携带业务状态码 code'],
        ['3', '全局异常处理', 'common/exception/GlobalExceptionHandler.java', '@RestControllerAdvice 拦截 BusinessException 和 Exception'],
        ['4', 'JWT 工具类', 'common/utils/JWTUtil.java', 'HMAC-SHA256 签名，24h 过期，generate/parse/validate'],
        ['5', 'ThreadLocal 用户上下文', 'common/utils/UserHolder.java', 'set/get/getUserId/remove 封装'],
        ['6', '登录拦截器', 'common/interceptor/LoginInterceptor.java', '校验 Authorization: Bearer <token>，解析后设置 UserHolder'],
        ['7', 'Web 配置', 'common/config/WebConfig.java', '注册拦截器（排除 /register、/login）+ BCryptPasswordEncoder Bean'],
    ],
    [1.2, 3.5, 5.5, 8.8]
)
doc1.add_paragraph('')

doc1.add_heading('2. 用户认证全链路', level=2)
add_styled_table(doc1,
    ['序号', '任务', '产出文件', '说明'],
    [
        ['1', 'User 实体', 'entity/User.java', '@TableName("sys_user")，字段：id/username/password/nickname/createdAt/updatedAt'],
        ['2', 'UserMapper', 'mapper/UserMapper.java', '继承 BaseMapper<User>'],
        ['3', 'UserService 接口', 'service/UserService.java', 'register() / login() 方法签名'],
        ['4', 'UserServiceImpl', 'service/impl/UserServiceImpl.java', '用户名唯一校验 + BCrypt 加密 + JWT 生成'],
        ['5', 'UserController', 'controller/UserController.java', 'POST /api/user/register + POST /api/user/login'],
        ['6', 'LoginVO', 'vo/LoginVO.java', '返回 token + userId + username'],
    ],
    [1.2, 3.5, 5, 9.3]
)
doc1.add_paragraph('')

doc1.add_heading('3. 编译错误修复与项目入口建立', level=2)
add_styled_table(doc1,
    ['序号', '任务', '产出文件', '说明'],
    [
        ['1', '重写 pom.xml', 'pom.xml', '添加 Spring Boot 3.2.0 父工程 + starter-web、MyBatis-Plus 3.5.5、MySQL、Lombok、jjwt 0.12.5、spring-security-crypto'],
        ['2', '创建启动入口', 'Application.java', '@SpringBootApplication + @MapperScan("com.smartpm.mapper")'],
        ['3', '应用配置文件', 'application.yml', '端口 8080、MySQL 连接(smartpm 库)、MyBatis-Plus 驼峰映射+控制台日志'],
        ['4', '修复 import 路径', 'GlobalExceptionHandler.java', '修正 R 类 import：com.smartpm.common.R → com.smartpm.common.result.R'],
        ['5', '清理无效代码', '删除 com.syt 包', '删除 IDEA 生成的 Hello World 模板 Main.java'],
    ],
    [1.2, 3.5, 5, 9.3]
)
doc1.add_paragraph('')

doc1.add_heading('4. 数据库初始化', level=2)
items = [
    '编写 sql/init.sql：创建 smartpm 数据库 + sys_user 表（id/username/password/nickname/created_at/updated_at）',
    '定位本地 MySQL 8.0 服务并测试连接（root/123456）',
    '执行建库建表脚本，验证数据库连接正常',
    '更新 application.yml 中数据库密码为实际密码',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_paragraph('')
doc1.add_paragraph('本日成果：项目从零搭建，完成基础架构 + 用户认证模块，编译通过，数据库连通。共创建 13 个 Java 源文件 + 3 个配置/资源文件。')

# ==================== 6月30日 ====================
doc1.add_heading('二、2026年6月30日（周二）— 调试测试与异常处理完善', level=1)

doc1.add_heading('1. 数据库连接与 API 全链路测试', level=2)
items = [
    '验证 MySQL80 服务运行状态，确认 smartpm 库和 sys_user 表存在',
    'Maven 编译通过，应用启动耗时 ~2 秒，HikariCP 连接池正常',
    'POST /api/user/register — 注册成功，BCrypt 加密存储验证通过',
    'POST /api/user/register（重复用户名）— 500 "用户名已存在" 验证通过',
    'POST /api/user/login（错误密码）— 500 "用户名或密码错误" 验证通过',
    'POST /api/user/login（正确凭证）— 200 返回 JWT token 验证通过',
    '结论：数据库连接、MyBatis-Plus、BCrypt、JWT 全链路正常工作',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_heading('2. GlobalExceptionHandler 完善', level=2)
add_styled_table(doc1,
    ['序号', '任务', '说明'],
    [
        ['1', '新增 NoResourceFoundException 处理', '返回 404 "资源不存在"（如 GET / 访问不存在的路径）'],
        ['2', '新增 HttpRequestMethodNotSupportedException 处理', '返回 405 "请求方法不允许"（如 GET 访问仅支持 POST 的接口）'],
        ['3', '验证', 'GET / → 404 ✅ | GET /api/user/login → 405 ✅ | POST /api/user/login → 200 ✅'],
    ],
    [1, 5, 13]
)
doc1.add_paragraph('')
doc1.add_paragraph('本日成果：完成全链路测试验证，完善全局异常处理的 HTTP 状态码区分（404/405/500），无新增文件。')

# ==================== 7月1日 ====================
doc1.add_heading('三、2026年7月1日（周三）— 核心业务 CRUD + 前端骨架 + 拖拽看板 + WebSocket', level=1)

doc1.add_heading('1. 浏览项目代码（准备工作）', level=2)
doc1.add_paragraph('完整浏览 14 个 Java 源文件，确认当前状态：用户注册/登录/JWT认证全链路已就绪，为后续开发做准备。')

doc1.add_heading('2. Project 与 Task 业务模块（新增 11 个文件）', level=2)
add_styled_table(doc1,
    ['序号', '任务', '产出文件', '说明'],
    [
        ['1', 'Project 实体', 'entity/Project.java', 'sys_project 表实体（id/name/description/creator_id）'],
        ['2', 'Task 实体', 'entity/Task.java', 'sys_task 表实体（id/project_id/title/description/status/assignee_id/creator_id）'],
        ['3', 'ProjectMapper', 'mapper/ProjectMapper.java', '继承 BaseMapper<Project>'],
        ['4', 'TaskMapper', 'mapper/TaskMapper.java', '继承 BaseMapper<Task>'],
        ['5', 'TaskUpdateDTO', 'dto/TaskUpdateDTO.java', '任务更新请求体（id/title/description/status/assigneeId）'],
        ['6', 'ProjectService 接口', 'service/ProjectService.java', 'create/list/delete 方法签名'],
        ['7', 'TaskService 接口', 'service/TaskService.java', 'create/listByProject/update/delete 方法签名'],
        ['8', 'ProjectServiceImpl', 'service/impl/ProjectServiceImpl.java', '项目业务逻辑 + 创建者权限校验'],
        ['9', 'TaskServiceImpl', 'service/impl/TaskServiceImpl.java', '任务业务逻辑 + 状态枚举校验'],
        ['10', 'ProjectController', 'controller/ProjectController.java', 'POST /api/project/create + GET /api/project/list + DELETE /api/project/{id}'],
        ['11', 'TaskController', 'controller/TaskController.java', 'POST /api/task/create + GET /api/task/list/{projectId} + PUT /api/task/update + DELETE /api/task/{id}'],
    ],
    [1, 3.5, 5.5, 9]
)
doc1.add_paragraph('')

doc1.add_heading('3. Vue 3 + Vite 前端项目搭建（13 个文件）', level=2)
add_styled_table(doc1,
    ['序号', '任务', '产出文件', '说明'],
    [
        ['1', '项目脚手架', 'package.json / index.html / vite.config.js', 'Vue 3 + Vite 5 + Element Plus + Axios + Pinia + Vue Router 4'],
        ['2', '入口文件', 'main.js / App.vue', '挂载 Vue/Router/Pinia/ElementPlus（中文国际化）'],
        ['3', '路由配置', 'router/index.js', '路由表 + beforeEach 守卫（未登录→/login, 已登录→/dashboard）'],
        ['4', '状态管理', 'store/user.js', 'Pinia: token/userInfo 状态 + login/logout/register actions'],
        ['5', 'Axios 封装', 'utils/request.js', '请求拦截器(Bearer Token) + 响应拦截器(401/404/405/500)'],
        ['6', 'API 模块', 'api/user.js, project.js, task.js', 'login/register, createProject/listProjects/deleteProject, CRUD task'],
        ['7', '登录页', 'views/Login.vue', '登录/注册 Tab 切换页面'],
        ['8', '仪表盘', 'views/Dashboard.vue', '顶部栏 + 退出登录'],
        ['9', '项目列表', 'components/ProjectList.vue', '表格 + 新建 + 删除'],
    ],
    [1, 2.5, 5, 10.5]
)
doc1.add_paragraph('')

doc1.add_heading('4. 截止日期字段 + 收尾页面', level=2)
items = [
    'Task.entity 新增 dueDate (LocalDate) 字段',
    'TaskUpdateDTO / TaskService / TaskController 同步增加 dueDate 支持',
    'sql/init.sql 中 sys_task 表新增 due_date DATE 列',
    'Dashboard.vue 重写为卡片网格布局（el-card + hover 浮起效果 + 点击跳转 /project/:id）',
    '新增 TaskList.vue 页面（el-table 展示 + 内联编辑状态/截止日期 + 新建任务弹窗 + 删除确认）',
    '路由配置：/login → Login.vue（仅游客）、/dashboard → Dashboard.vue（需登录）、/project/:id → TaskList.vue（需登录）',
    '编译验证：mvn compile ✅ | vite build ✅',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_heading('5. 拖拽排序 API + 视觉重设计 + 拖拽看板集成', level=2)
items = [
    '新增 DragDTO（taskId + targetStatus + targetOrderIndex）',
    '新增 PUT /api/task/drag 接口：同列移动（区间 ±1）+ 跨列移动（源列收口 + 目标列让位），@Transactional 保护',
    '新增 order_index 字段到 Task.entity 和 sys_task 表',
    'CSS 设计令牌 theme.css：品牌色 Indigo #6366F1 + 三列状态色（待办琥珀/进行中蓝/已完成翠绿）',
    'Login.vue 重写：左侧品牌渐变面板 + 右侧表单布局',
    'TaskList.vue 改为三列看板布局，安装 vuedraggable@next',
    '三列 draggable 组件：group="tasks" 跨列拖拽，@change 处理 added/moved，自动调用 drag API',
    '卡片 UI：6点拖拽手柄 + 幽灵态半透明虚线 + 空列虚线占位',
    '前后端端口合并尝试（vite build 到 static/ + SpaController），后因 bug 过多恢复前后端分离',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_heading('6. WebSocket 实时协同', level=2)
items = [
    '新增 WebSocketConfig：注册 /ws/project/{projectId} 端点',
    '新增 TaskWebSocketHandler：ConcurrentHashMap<Long, Set<WebSocketSession>> 按 projectId 分组管理',
    'TaskController 的 drag/create/update/delete 操作后自动 broadcast 通知同项目在线用户',
    'pom.xml 新增 spring-boot-starter-websocket 依赖',
    '前端 TaskList.vue：onMounted 连接 WebSocket，收到 TASK_UPDATED 消息自动 fetchTasks() 刷新',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_paragraph('')
doc1.add_paragraph('本日成果：完成 11 个后端文件 + 13 个前端文件 + 3 个新增功能（拖拽/设计/WebSocket），前后端完整闭环。')

# ==================== 7月2日 ====================
doc1.add_heading('四、2026年7月2日（周四）— LLM 大模型接入与 AI 智能功能', level=1)

doc1.add_heading('1. WebSocket 鉴权与前端健壮性增强', level=2)
items = [
    '新增 WebSocketHandshakeInterceptor：从 ?token= 查询参数提取 JWT 并校验，拒绝无效连接',
    '前端指数退避重连：1s → 2s → 4s → 8s → 16s，封顶 30s',
    '最大重试 5 次限制 + onopen 重置计数器 + 防重复连接 + 彻底清理回调',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_heading('2. LLM 大模型接入（通用流式对话）', level=2)
add_styled_table(doc1,
    ['序号', '任务', '产出文件', '说明'],
    [
        ['1', '添加 WebFlux 依赖', 'pom.xml', 'spring-boot-starter-webflux（仅用 WebClient，运行时仍 Servlet）'],
        ['2', 'AI 配置属性', 'common/config/AIConfigProperties.java', '@ConfigurationProperties(prefix = "ai") 读取 api-key/base-url/model'],
        ['3', 'WebClient Bean', 'common/config/AIWebClientConfig.java', 'WebClient.Builder Bean 配置'],
        ['4', 'AI 服务接口', 'service/AIService.java', 'Flux<String> streamChat(prompt) + String chat(prompt)'],
        ['5', 'AI 服务实现', 'service/impl/AIServiceImpl.java', 'WebClient POST /v1/chat/completions，stream:true→Flux SSE，false→同步'],
        ['6', 'AI 控制器', 'controller/AIController.java', 'GET /api/ai/stream-chat?prompt=xxx → SseEmitter (120s 超时)'],
        ['7', '配置更新', 'application.yml + WebConfig', 'ai 配置块 + /api/ai/stream-chat 加入拦截器白名单'],
    ],
    [1, 3.5, 5.5, 9]
)
doc1.add_paragraph('')

doc1.add_heading('3. AI 任务智能拆解', level=2)
items = [
    '新增 POST /api/task/{taskId}/ai-decompose：调用大模型将任务拆解为 3-5 个子任务并批量写入 DB',
    'Prompt 设计：角色设定（资深项目管理专家）+ 约束（3-5个、具体可执行、有逻辑顺序）+ 格式强制（只返回 JSON 数组）+ 示例引导',
    'JSON 解析三级防御：正则提取 ```json``` 代码块 → 正则匹配 [...] 数组 → Jackson 解析，任一失败抛 BusinessException',
    '拆解完成后 WebSocket 广播通知同项目用户刷新',
    '前端 TaskList.vue：卡片新增"AI 拆解"按钮，loading 态绑定 decomposingTaskId，成功后自动刷新子任务清单',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_heading('4. AI 项目周报（流式 Markdown）', level=2)
items = [
    '新增 GET /api/project/{projectId}/ai-summary → SseEmitter：收集近 7 天任务数据，AI 生成 Markdown 周报并 SSE 流式推送',
    '数据分类：已完成（近7天 DONE）→ "本周成果" / 进行中（IN_PROGRESS）→ "进行中工作" / 已逾期（dueDate < today 且 != DONE）→ "风险与逾期提醒"',
    '前端：新增 api/summary.js（fetch + ReadableStream 逐块读取 SSE），新增 markdown-it 依赖',
    'TaskList.vue 顶栏增加"生成项目总结"按钮，弹窗内以打字机效果实时渲染 Markdown',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_paragraph('')
doc1.add_paragraph('本日成果：完成 LLM 大模型全链路接入（3 个 AI 接口），前端 AI UI 交互完成。新增 5 个后端文件 + 1 个前端 API 模块。')

# ==================== 7月3日 ====================
doc1.add_heading('五、2026年7月3日（周五）— 部署优化 + 任务层级重构 + Bug 修复', level=1)

doc1.add_heading('1. 环境变量配置', level=2)
items = [
    '新增 .env.development（VITE_API_BASE_URL=/api）和 .env.production（VITE_API_BASE_URL=/api）',
    'request.js 中 baseURL 改为读取 import.meta.env.VITE_API_BASE_URL，fallback /api',
    '构建时 Vite 自动加载对应环境文件，静态替换变量值',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_heading('2. SPA History 路由回退', level=2)
items = [
    'WebConfig.addResourceHandlers + 自定义 PathResourceResolver',
    '实体文件直接返回 → /api/** 跳过回退 → 其余路由返回 index.html（Vue Router 接管）',
    '同时提供 Nginx try_files 备用方案',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_heading('3. Vite 生产构建配置', level=2)
items = [
    'manualChunks 代码分包：vue-vendor (~207KB) / element-plus (~799KB) / draggable (~45KB) / markdown (~71KB) / vendor (~183KB)',
    'sourcemap 关闭，chunkSizeWarningLimit 放宽至 1000KB',
    'vite build ✅（8.29s，16 个产出文件）',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_heading('4. Docker 容器化部署', level=2)
add_styled_table(doc1,
    ['序号', '任务', '产出文件', '说明'],
    [
        ['1', '后端 Dockerfile', 'Dockerfile', 'eclipse-temurin:17-jre-alpine，非 root 用户 app:app，JVM -Xms256m -Xmx512m +G1GC，curl 健康检查'],
        ['2', 'Nginx 配置', 'nginx.conf', '静态资源 1y 缓存 + /api 反向代理 + /ws WebSocket 代理(Upgrade) + SPA try_files 回退'],
        ['3', '服务编排', 'docker-compose.yml', '4 服务：mysql:8.0 + redis:7-alpine + backend(本地构建) + nginx:alpine，健康检查 + depends_on 启动顺序'],
        ['4', '构建排除', '.dockerignore', '排除 node_modules/.git/IDE 文件'],
        ['5', '配置适配', 'pom.xml + application.yml', '新增 actuator/redis 依赖，MySQL/Redis host 改用 ${MYSQL_HOST:localhost} 占位符'],
    ],
    [1, 3.5, 4.5, 10]
)
doc1.add_paragraph('')

doc1.add_heading('5. 任务层级重构（主任务 + 子任务）', level=2)
items = [
    'sys_task 表新增 parent_id 字段（parent_id IS NULL = 主任务，NOT NULL = 子任务）',
    'TaskServiceImpl 六处改动：listByProject 只查主任务 / delete 级联删除子任务 / drag 仅操作主任务 / decomposeTask 写入 parentId / 新增 listSubtasks + toggleSubtask',
    '新增 API：GET /api/task/{taskId}/subtasks（查子任务列表）、PUT /api/task/{taskId}/toggle-subtask（切换 TODO↔DONE）',
    '前端 TaskList.vue 重构：卡片底部 4px 渐变进度条 + "2/5" 数字、详情弹窗（el-dialog）含子任务清单、子任务 el-checkbox 乐观更新、删除提示"将同时删除其所有子任务"、WebSocket 刷新时同步更新子任务',
    'Prompt 优化：禁止子任务标题与主任务高度重叠，提供正确/错误示例对照',
    'mvn compile ✅ | vite build ✅（TaskList chunk 12.70KB → 15.85KB）',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_heading('6. 关键 Bug 修复', level=2)
add_styled_table(doc1,
    ['序号', '问题', '根因', '修复'],
    [
        ['1', 'AI 总结弹窗空白', 'Spring SseEmitter 发送 data:内容（冒号后无空格），前端检查 data: 内容（有空格），所有 chunk 被静默丢弃', 'extractData() 函数同时兼容两种格式 + 后端错误推送 [ERROR] 消息 + 全链路控制台日志'],
        ['2', 'Dashboard 无限加载', 'ProjectController @CrossOrigin(origins="*") + WebConfig allowCredentials(true) 叠加导致 CORS 预检失败', '移除 Controller 级别 @CrossOrigin，CORS 从 /** 收紧为 /api/**，Dashboard.vue 增加完整 try/catch/finally + console 日志'],
        ['3', '项目列表返回异常', 'Dashboard.vue 未校验 API 返回数据格式', '增加 Array.isArray 校验 + data 为空时赋空数组 + catch 分网络断开/业务异常两种提示'],
    ],
    [1, 4, 7, 7]
)
doc1.add_paragraph('')

doc1.add_paragraph('本日成果：完成部署优化（环境变量/SPA/Docker）、任务层级重构、3 个关键 Bug 修复。新增 5 个配置/部署文件。')

# ==================== 汇总 ====================
doc1.add_heading('六、本周项目文件统计', level=1)
doc1.add_paragraph('截至 7月3日，项目累计产出：')
add_styled_table(doc1,
    ['类别', '文件数', '明细'],
    [
        ['Java 源文件', '34 个', '入口 1 + common 13 + controller 4 + dto 2 + entity 3 + mapper 3 + service 7 + vo 1'],
        ['前端源文件', '16 个', 'views 3 + api 4 + store 1 + router 1 + utils 1 + 配置文件 6'],
        ['配置/部署文件', '8 个', 'pom.xml, application.yml, Dockerfile, nginx.conf, docker-compose.yml, .dockerignore, .env.development, .env.production'],
        ['SQL 脚本', '1 个', 'sql/init.sql'],
        ['文档', '2 个', 'project_history.md, phase1_guide.md'],
    ],
    [3, 2, 14]
)
doc1.add_paragraph('')

doc1.add_heading('七、下周计划', level=1)
items = [
    '完善 Redis 会话管理功能（依赖已添加，待编写业务代码）',
    '增加项目成员管理与权限控制',
    '前端性能优化与体验打磨',
    '补充单元测试与集成测试',
    'CI/CD 流水线配置',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.save(os.path.join(BASE_DIR, '第一周周报.docx'))
print("OK: 第一周周报.docx generated")


# ===================================================================
# 第一周AI使用记录
# ===================================================================
doc2 = Document()

style2 = doc2.styles['Normal']
style2.font.name = '微软雅黑'
style2.font.size = Pt(10.5)
style2.paragraph_format.space_after = Pt(4)
style2.paragraph_format.line_spacing = 1.35
style2.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')

title2 = doc2.add_heading('SmartPM 项目 — 第一周 AI 使用记录', level=0)
title2.alignment = WD_ALIGN_PARAGRAPH.CENTER
doc2.add_paragraph('记录周期：2026年6月29日 — 2026年7月3日', style='Normal').alignment = WD_ALIGN_PARAGRAPH.CENTER
doc2.add_paragraph('')

doc2.add_heading('一、AI 使用概述', level=1)
doc2.add_paragraph(
    '本项目在开发过程中使用了两个层面的 AI 能力：\n'
    '1) 开发辅助层面：通过 Claude Code（大语言模型编程助手）进行代码编写、调试、架构设计等开发工作；\n'
    '2) 产品功能层面：在系统中接入了 DeepSeek 大模型（deepseek-chat），为用户提供智能任务拆解和项目周报生成功能。'
)

# ==================== 6月29日 ====================
doc2.add_heading('二、2026年6月29日 — 项目初始化与用户认证', level=1)

doc2.add_paragraph('Claude Code 开发辅助任务：', style='Normal')
items = [
    '根据口头需求创建 project_history.md 项目历史记录文件',
    '生成项目基础类：R.java、BusinessException、GlobalExceptionHandler、JWTUtil（共 4 个文件）',
    '生成认证全链路：User.java → UserMapper → UserService → UserServiceImpl → UserController → LoginInterceptor → UserHolder → LoginVO（共 8 个文件）',
    '生成配置类：WebConfig.java（拦截器注册 + BCrypt Bean）',
    '诊断并修复编译错误：pom.xml 依赖缺失、GlobalExceptionHandler import 路径错误、com.syt.Main 无效代码',
    '重写 pom.xml（Spring Boot 3.2.0 + MyBatis-Plus 3.5.5 + jjwt 0.12.5 + Lombok + BCrypt）',
    '创建 Application.java 启动入口 + application.yml 配置文件',
    '编写 sql/init.sql 数据库初始化脚本',
    '通过 Bash 执行 MySQL 命令创建数据库和表',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')

doc2.add_paragraph('')
doc2.add_paragraph('AI 交互统计（估算）：代码生成 ~15 轮 | 配置编写 ~5 轮 | 诊断修复 ~3 轮 | 文档生成 ~2 轮')

# ==================== 6月30日 ====================
doc2.add_heading('三、2026年6月30日 — 调试测试与异常处理完善', level=1)
doc2.add_paragraph('Claude Code 开发辅助任务：', style='Normal')
items = [
    '执行数据库连接诊断：检查 MySQL 服务状态、数据库和表是否就绪',
    'API 全链路测试：逐一验证注册/重复注册/错误密码登录/正确登录 4 个场景',
    '增强 GlobalExceptionHandler：新增 NoResourceFoundException（404）和 HttpRequestMethodNotSupportedException（405）两种异常处理器',
    '验证三种 HTTP 状态码的返回结果（404/405/200）',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')
doc2.add_paragraph('')
doc2.add_paragraph('AI 交互统计：测试诊断 ~4 轮 | 代码修改 ~1 轮')

# ==================== 7月1日 ====================
doc2.add_heading('四、2026年7月1日 — 核心业务 + 前端骨架 + 拖拽看板 + WebSocket', level=1)

doc2.add_paragraph('Claude Code 开发辅助任务：', style='Normal')
items = [
    '浏览全部 14 个 Java 文件，分析代码状态，为后续任务做准备',
    '定义数据库表结构（sys_project、sys_task），生成建表 SQL',
    '生成 Project 和 Task 完整 CRUD（实体/Mapper/Service/Controller，共 11 个新文件），包含：创建者权限校验、@Transactional 级联删除、状态枚举校验',
    '搭建 Vue 3 + Vite 前端项目（package.json + vite.config.js + main.js + App.vue + 全部配置，共 13 个文件）',
    '生成 Vue Router 路由表 + beforeEach 守卫、Pinia user store（localStorage 持久化）、Axios 拦截器封装',
    '编写 Login.vue（左侧品牌渐变面板 + 右侧登录/注册表单）',
    '编写 Dashboard.vue（卡片网格 + 彩色顶边 + hover 浮起 + 空态插画引导 + 新建/删除弹窗）',
    '编写 TaskList.vue（el-table 任务列表 + 内联状态编辑 + 截止日期选择器 + 新建弹窗）',
    'CSS 视觉重设计：theme.css 设计令牌系统（Indigo 品牌 + 三列状态色 + 阴影/圆角/字体）',
    '集成 vuedraggable 拖拽看板（三列 draggable + 跨列移动 + drag API 同步排序）',
    '实现后端拖拽排序算法（DragDTO + PUT /api/task/drag + @Transactional 同列/跨列排序逻辑）',
    'Task 增加 dueDate（截止日期）和 orderIndex（排序字段），前后端同步修改',
    '实现 WebSocket 实时协同（WebSocketConfig + TaskWebSocketHandler + broadcast 机制）',
    '端口合并尝试与回退（SpaController → 因 bug 过多恢复前后端分离）',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')
doc2.add_paragraph('')
doc2.add_paragraph('AI 交互统计：代码生成 ~20 轮 | 架构设计 ~5 轮 | 配置 ~3 轮 | 文档更新 ~1 轮')

# ==================== 7月2日 ====================
doc2.add_heading('五、2026年7月2日 — LLM 大模型接入与 AI 功能实现', level=1)

doc2.add_paragraph('一、Claude Code 开发辅助任务：', style='Normal')
items = [
    'WebSocket 鉴权升级：生成 WebSocketHandshakeInterceptor（从 ?token= 参数解析 JWT，拒绝无效连接）',
    '前端 WebSocket 健壮性：指数退避重连算法、最大重试限制、防重复连接、彻底清理回调',
    'AI 配置方案设计：AIConfigProperties（@ConfigurationProperties）+ AIWebClientConfig + application.yml ai 配置块',
    '生成 AIController + AIService + AIServiceImpl：WebClient 调用 DeepSeek API + SseEmitter SSE 推送',
    'AI 任务拆解功能：设计 Prompt 模板（角色设定 + 约束 + 格式强制 + 示例引导）+ JSON 解析三级防御机制',
    'AI 项目周报功能：设计数据分类逻辑（已完成/进行中/逾期）+ Prompt 拼接 + SSE 流式 Markdown 推送',
    '前端 AI UI：api/summary.js（fetch + ReadableStream 逐块解析 SSE）+ markdown-it 实时渲染',
    'TaskList.vue 增加"AI 拆解"按钮和"生成项目总结"流式弹窗',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')

doc2.add_paragraph('')
doc2.add_paragraph('二、DeepSeek 大模型产品功能调用（测试期间）：', style='Normal')
add_styled_table(doc2,
    ['功能', 'API 接口', '调用方式', '测试次数', '估算 Token/次'],
    [
        ['通用流式对话', 'GET /api/ai/stream-chat', 'WebClient SSE 流式', '~5 次', '~200 tokens'],
        ['任务智能拆解', 'POST /api/task/{id}/ai-decompose', 'WebClient 非流式 (stream:false)', '~10 次', '~800 tokens（Prompt + JSON 返回）'],
        ['项目周报生成', 'GET /api/project/{id}/ai-summary', 'WebClient SSE 流式', '~5 次', '~1500 tokens（任务数据 + Markdown 输出）'],
    ],
    [3, 5, 4, 2.5, 4.5]
)
doc2.add_paragraph('')

doc2.add_paragraph('三、Prompt 工程设计要点：', style='Normal')
items = [
    '任务拆解 Prompt：角色设定（"你是一位资深的项目管理专家"）→ 约束条件（"拆解为 3-5 个子任务"、"具体可执行"、"有逻辑顺序"）→ 格式强制（"只返回 JSON 数组，不要任何其他文字"）→ 示例引导（提供正确/错误示例对照）',
    '项目周报 Prompt：数据收集（查询近 7 天三类任务）→ 结构化拼接（已完成/进行中/逾期分别列出）→ 格式要求（"输出 Markdown 格式报告"）→ 章节指定（本周成果 / 进行中工作 / 风险与逾期提醒 / 下周建议）',
    'JSON 解析三级防御：第1步 正则提取 ```json...``` 代码块 → 第2步 正则匹配 [...] 数组字面量 → 第3步 Jackson ObjectMapper 解析 → 任一失败抛 BusinessException("AI 返回格式异常")',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')

doc2.add_paragraph('')
doc2.add_paragraph('AI 交互统计：代码生成 ~10 轮 | Prompt 设计 ~3 轮 | AI API 测试 ~20 次')

# ==================== 7月3日 ====================
doc2.add_heading('六、2026年7月3日 — 部署优化 + 任务层级重构 + Bug 修复', level=1)

doc2.add_paragraph('Claude Code 开发辅助任务：', style='Normal')
items = [
    '环境变量方案设计：.env.development / .env.production 分环境配置 VITE_API_BASE_URL',
    'SPA History 路由回退方案：WebConfig ResourceHandler + 自定义 PathResourceResolver（区分静态文件/API/前端路由三种场景）',
    'Vite 生产构建优化：manualChunks 分包策略（按 vue/element-plus/draggable/markdown 拆分 5 个 chunk）',
    'Docker 完整方案：Dockerfile（非 root 用户 + JVM 优化参数 + 健康检查）+ nginx.conf（动静分离 + WebSocket 代理）+ docker-compose.yml（4 服务编排 + 健康检查依赖）',
    'pom.xml 新增 actuator + redis 依赖，application.yml 适配 Docker 环境变量占位符',
    '任务层级重构方案设计：parent_id 字段 + 六处后端改动（listByProject/delete/drag/decomposeTask/listSubtasks/toggleSubtask）',
    '前端层级看板：子任务进度条（4px 渐变 + 分数标识）+ 详情弹窗（状态标签/全文描述/子任务清单/el-checkbox 乐观更新）',
    'SSE 空白修复：诊断 data: 冒号后空格不匹配问题 → 重写 extractData() 兼容两种格式 + 全链路日志',
    'Dashboard 无限加载修复：诊断 CORS 冲突（@CrossOrigin + allowCredentials）→ 统一 CORS 管理 + 完整错误处理',
    '清理无效文件（SpaController、static 目录残留）、项目代码整理',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')
doc2.add_paragraph('')
doc2.add_paragraph('AI 交互统计：代码生成 ~12 轮 | Bug 诊断 ~4 轮 | 架构设计 ~3 轮 | 文档更新 ~1 轮')

# ==================== 汇总 ====================
doc2.add_heading('七、AI 使用汇总', level=1)

doc2.add_heading('1. Claude Code 开发辅助统计', level=2)
add_styled_table(doc2,
    ['统计指标', '数值', '说明'],
    [
        ['总交互轮次', '~80+ 轮', '涵盖代码生成、配置编写、调试诊断、架构设计、文档撰写'],
        ['生成 Java 源文件', '34 个', 'entity/controller/service/mapper/dto/vo/config/websocket/interceptor 全覆盖'],
        ['生成前端源文件', '16 个', 'views(3) + api(4) + store(1) + router(1) + utils(1) + 配置文件(6)'],
        ['生成配置/部署文件', '8 个', 'pom.xml, application.yml, .env(2), vite.config.js, Dockerfile, nginx.conf, docker-compose.yml'],
        ['SQL 脚本', '1 个', 'sql/init.sql（持续更新，含 3 张表 + 完整字段定义）'],
        ['Bug 诊断修复', '~8 个', '编译错误、数据库连接、SSE 空白、CORS 冲突、Dashboard 无限加载等'],
        ['文档输出', '3 个', 'project_history.md（持续更新）、phase1_guide.md（参考已有）、本文档'],
    ],
    [4, 2.5, 12.5]
)
doc2.add_paragraph('')

doc2.add_heading('2. DeepSeek 大模型产品功能统计', level=2)
add_styled_table(doc2,
    ['AI 功能', '技术方案', '后端文件', '前端文件', '状态'],
    [
        ['通用流式对话', 'WebClient + SseEmitter', 'AIController / AIService / AIServiceImpl / AIConfigProperties / AIWebClientConfig', '—（后端直接测试）', '可用'],
        ['任务智能拆解', 'WebClient 非流式 + JSON 解析三级防御', 'TaskService.decomposeTask() + TaskController', 'api/task.js + TaskList.vue', '可用'],
        ['项目周报生成', 'WebClient SSE 流式 + Markdown 模板', 'ProjectService.generateSummary() + ProjectController', 'api/summary.js + TaskList.vue', '可用'],
    ],
    [3, 4.5, 5.5, 3.5, 1.5]
)
doc2.add_paragraph('')

doc2.add_heading('3. AI API 配置信息', level=2)
add_styled_table(doc2,
    ['配置项', '值/方案', '说明'],
    [
        ['AI 服务商', 'DeepSeek', '兼容 OpenAI 协议 (/v1/chat/completions)'],
        ['Base URL', 'https://api.deepseek.com', '通过 application.yml ai.base-url 配置'],
        ['模型', 'deepseek-chat', '通用对话模型，通过 ai.model 配置'],
        ['API Key', '环境变量 ${AI_API_KEY}', '不硬编码，支持 .env / docker-compose / k8s secret 注入'],
        ['HTTP 客户端', 'WebClient (spring-webflux)', '非阻塞异步 HTTP，stream:true→SSE 流式，false→同步阻塞获取'],
        ['SSE 推送', 'SseEmitter (120s-180s 超时)', '基于 Servlet 3.1 异步，不阻塞 Tomcat 线程池'],
        ['前端 SSE 消费', 'fetch + ReadableStream', '手动逐行解析 data: 前缀，兼容冒号后有无空格两种格式'],
        ['Markdown 渲染', 'markdown-it', '轻量 JS Markdown 解析器，含表格/代码块/引用块完整样式'],
    ],
    [3, 6, 10]
)
doc2.add_paragraph('')

doc2.add_heading('4. AI 使用成效评估', level=2)
doc2.add_paragraph('成效优势：')
items = [
    '开发速度：5 天从零完成全栈项目（后端 34 + 前端 16 + 部署 8 文件），传统方式预估 2-3 周，效率提升约 3 倍',
    '全栈覆盖：AI 辅助跨后端（Java/Spring Boot/MyBatis-Plus/WebSocket）、前端（Vue3/Element Plus/Vite/vuedraggable）、部署（Docker/Nginx）三大领域',
    '产品创新：成功将大模型嵌入业务（任务拆解 + 周报生成），用户可直接使用，非花瓶功能',
    '代码安全：AI 生成代码遵循最佳实践（LambdaQueryWrapper 防 SQL 注入、BCrypt 加密、JWT 认证、@Transactional 事务保护）',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')

doc2.add_paragraph('待改进：')
items = [
    'SSE 格式兼容（data: 冒号后空格差异）暴露了前后端约定未提前文档化的问题，建议后续先定接口规范再实现',
    'CORS 配置在多次迭代中出现冲突（@CrossOrigin + WebConfig 全局 CORS），需建立统一的配置管理规范文档',
    'AI Prompt 效果需在真实业务数据上持续调优，当前基于少量测试场景设计',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')

doc2.add_heading('5. 后续 AI 使用计划', level=2)
items = [
    'Prompt 优化：根据真实用户反馈持续调优任务拆解和周报生成的提示词',
    'AI 功能扩展：自然语言创建任务（"明天完成登录页面"→自动解析为 Task）、智能排期建议',
    'Token 消耗监控：在 AIServiceImpl 中增加 usage 字段日志，便于成本分析和优化',
    '错误体验优化：AI 调用失败时返回更细粒度的错误信息（如 API Key 无效 vs 网络超时 vs 模型繁忙）',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')

doc2.save(os.path.join(BASE_DIR, '第一周ai使用记录.docx'))
print("OK: 第一周ai使用记录.docx generated")
