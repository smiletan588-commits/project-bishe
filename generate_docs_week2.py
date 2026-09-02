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
# 第二周周报
# ===================================================================
doc1 = Document()

style = doc1.styles['Normal']
style.font.name = '微软雅黑'
style.font.size = Pt(10.5)
style.paragraph_format.space_after = Pt(4)
style.paragraph_format.line_spacing = 1.35
style.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')

title = doc1.add_heading('SmartPM 智能协同看板系统 — 第二周周报', level=0)
title.alignment = WD_ALIGN_PARAGRAPH.CENTER
doc1.add_paragraph('周期：2026年7月6日 — 2026年7月9日', style='Normal').alignment = WD_ALIGN_PARAGRAPH.CENTER
doc1.add_paragraph('项目：智能项目管理与看板系统（SmartPM）', style='Normal').alignment = WD_ALIGN_PARAGRAPH.CENTER
doc1.add_paragraph('')

# ==================== 7月6日 ====================
doc1.add_heading('一、2026年7月6日（周一）— 项目卡片增强 + 数据统计大屏 + AI 流式修复', level=1)

doc1.add_heading('1. Dashboard 项目卡片编辑/删除功能', level=2)
add_styled_table(doc1,
    ['序号', '任务', '产出/修改文件', '说明'],
    [
        ['1', '卡片布局重设计', 'views/Dashboard.vue', '新增 card-header 弹性布局，标题 + 三点下拉菜单（el-dropdown + MoreFilled 图标）'],
        ['2', '编辑项目弹窗', 'views/Dashboard.vue', 'el-dialog 预填当前名称/描述 → PUT /api/project/update'],
        ['3', '删除确认', 'views/Dashboard.vue', 'ElMessageBox.confirm 二次确认 → DELETE /api/project/{id}'],
        ['4', '下拉菜单防冒泡', 'views/Dashboard.vue', 'el-dropdown 绑定 @click.stop，防止触发卡片跳转'],
        ['5', '项目更新后端接口', 'ProjectController / ProjectService / ProjectServiceImpl', '新增 PUT /api/project/update（存在性校验 + 创建者权限校验）'],
        ['6', '级联删除优化', 'ProjectServiceImpl', '删除顺序：子任务(parent_id) → 主任务 → 项目，四步 @Transactional 保护'],
    ],
    [1.2, 3.5, 5.5, 8.8]
)
doc1.add_paragraph('')

doc1.add_heading('2. 数据可视化统计大屏 (Analytics Dashboard)', level=2)
add_styled_table(doc1,
    ['序号', '任务', '产出文件', '说明'],
    [
        ['1', '统计响应体', 'vo/AnalyticsVO.java', '3 个静态内部类：StatusItem / ProjectRankItem / DailyTrendItem，完整统计数据结构'],
        ['2', '统计服务', 'service/AnalyticsService.java + impl/AnalyticsServiceImpl.java', '计算 6 类统计：项目数/任务数/完成数/进行中/状态分布/项目排行/7天趋势'],
        ['3', '统计接口', 'controller/AnalyticsController.java', 'GET /api/analytics/overview（仅统计当前用户项目数据）'],
        ['4', 'ECharts 安装', 'package.json', 'npm install echarts --save'],
        ['5', '统计大屏页面', 'views/Analytics.vue', '四数据卡片 + 三图表（饼图/柱状图/折线图），ECharts 生命周期管理'],
        ['6', '路由与入口', 'router/index.js + Dashboard.vue', '/analytics 路由 + 顶栏"数据大屏"渐变按钮'],
    ],
    [1.2, 3, 5, 9.8]
)
doc1.add_paragraph('')
doc1.add_paragraph('图表细节：环形饼图(radius 55%-78%) 三色状态占比 + 水平柱状图(渐变紫蓝配色)项目排行 + 平滑折线图(渐变面积填充)近7天完工趋势')
doc1.add_paragraph('')

doc1.add_heading('3. AI 项目总结 SSE 流式空白修复（根因定位与重构）', level=2)
items = [
    '根因分析（精确定位到 AIServiceImpl.streamChat()）：DeepSeek API 返回 data:{...}（冒号后无空格），但 filter 硬编码 "data: "（有空格），导致所有数据行被过滤丢弃，Flux 空完成 → emitter 立即关闭 → 前端空白',
    '修复 1 — AIServiceImpl 流式解析重构：新增 flatMap 换行拆分（多行 chunk 拆为逐行流）、filter 同时兼容 "data: " 和 "data:" 两种格式、extractContent 根据前缀长度自适应截取（6字符 vs 5字符）',
    '修复 2 — ProjectServiceImpl 数据查询加固：allTasks 查询后显式判空初始化、遍历逐字段判空、无历史数据时调用 AI 生成项目启动寄语（含目标展望/开发路线图/敏捷建议）替代硬编码文案',
    '修复 3 — ProjectController 三层异常防护：同步异常 try-catch + SSE [ERROR] 推送、异步 Flux error 回调 + 完整堆栈日志、SSE 超时从 180s 延长至 300s',
    '修复前后对比：修复前 data:{...} → filter("data: ") → 全部丢弃 → 空白页；修复后 data:{...} → flatMap 拆行 → filter("data:" or "data: ") → 通过 → extractContent 自适应截取 → SSE 推送 → 前端逐字渲染',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_heading('4. 测试数据创建', level=2)
add_styled_table(doc1,
    ['序号', '任务', '说明'],
    [
        ['1', '体验账号', 'demo / 123456，昵称"体验用户"'],
        ['2', '测试项目', '3 个项目：SmartPM 核心功能开发(12 主任务+多组子任务) / 移动端适配优化(6 主任务) / 数据分析平台(4 主任务)'],
        ['3', '测试任务', '22 条主任务 + 16 条子任务，覆盖 TODO/IN_PROGRESS/DONE 三列，含 2 条逾期任务'],
        ['4', '可测场景', '看板拖拽、任务层级进度条、AI 拆解、项目周报（近7天每天有完成记录）、统计大屏、逾期提醒、空项目启动寄语'],
        ['5', '脚本文件', 'sql/test_data.sql（一键执行 mysql -uroot -p123456 < sql/test_data.sql）'],
    ],
    [1, 3, 15]
)
doc1.add_paragraph('')

doc1.add_heading('5. Analytics 404 问题排查', level=2)
items = [
    '问题：统计大屏页面弹窗报 404，所有统计数字显示为 0',
    '逐层验证请求链路：前端 request.js → Axios baseURL → Vite 代理 → Controller 映射 → CORS → 拦截器，全部正确',
    '根因定位：AnalyticsController 是新增文件，运行中的 Spring Boot 进程未加载（项目未使用 spring-boot-devtools，不会热加载新类）',
    '解决：重启后端 → Controller 被 Spring 扫描注册 → 端点立即响应 200',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_paragraph('')
doc1.add_paragraph('本日成果：完成项目卡片增强、统计大屏全栈（8 个文件）、AI 流式修复（3 层加固）、测试数据准备。新增 5 个后端文件 + 1 个前端页面 + 1 个 SQL 脚本。')

# ==================== 7月7日 ====================
doc1.add_heading('二、2026年7月7日（周二）— Wiki 文档中心 + AI 写作协同 + SSE 多轮调试', level=1)

doc1.add_heading('1. 数据库 pm_wiki 表创建', level=2)
doc1.add_paragraph('新增 pm_wiki 表（id / project_id / title / content / creator_id / create_time / update_time），支持项目级文档管理。')

doc1.add_heading('2. Wiki 后端 CRUD 模块（新增 5 个文件）', level=2)
add_styled_table(doc1,
    ['序号', '任务', '产出文件', '说明'],
    [
        ['1', 'Wiki 实体', 'entity/Wiki.java', '@TableName("pm_wiki")，字段：id/projectId/title/content/creatorId/createTime/updateTime'],
        ['2', 'WikiMapper', 'mapper/WikiMapper.java', '继承 BaseMapper<Wiki>'],
        ['3', 'WikiService 接口', 'service/WikiService.java', 'create / listByProject / getById / update / delete / aiCopilot'],
        ['4', 'WikiServiceImpl', 'service/impl/WikiServiceImpl.java', '业务实现：权限校验 + Prompt 构造 + 调用 AIService.streamChat()'],
        ['5', 'WikiController', 'controller/WikiController.java', '6 个 API 接口（5 个 CRUD + 1 个 SSE 流式 AI 写作）'],
    ],
    [1, 3, 5, 10]
)
doc1.add_paragraph('')
doc1.add_paragraph('API 接口：POST /api/wiki/create | GET /api/wiki/list/{projectId} | GET /api/wiki/{id} | PUT /api/wiki/update | DELETE /api/wiki/{id} | GET /api/wiki/ai-copilot（SSE 流式）')

doc1.add_heading('3. 前端 Wiki 页面实现', level=2)
add_styled_table(doc1,
    ['序号', '任务', '产出/修改文件', '说明'],
    [
        ['1', 'Markdown 编辑器', 'package.json', '安装 md-editor-v3（中文工具栏、预览、全屏）'],
        ['2', 'Wiki API 层', 'api/wiki.js', 'CRUD 请求 + SSE 流式 fetch（与 summary.js 共用解析模式）'],
        ['3', 'Wiki 页面', 'views/WikiView.vue', '两栏布局：左侧 280px 文档列表 + 右侧 MdEditor + AI 写作面板(380px)'],
        ['4', '路由入口', 'router/index.js', '新增 /project/:id/wiki 路由'],
        ['5', '导航入口(1)', 'views/TaskList.vue', '顶栏新增"文档中心"按钮（Document 图标）'],
        ['6', '导航入口(2)', 'views/Dashboard.vue', '项目卡片下拉菜单新增"文档中心"入口'],
    ],
    [1, 3, 5, 10]
)
doc1.add_paragraph('')
doc1.add_paragraph('AI 写作面板数据流：用户在编辑器划选文字 → mouseup 自动填入"待处理文本" → 输入指令 → 点击"AI 运行" → fetch SSE 流式 → 打字机效果输出 → "替换原文"或"插入末尾"')

doc1.add_heading('4. AI SSE 流式推送 Bug 的多轮排查与修复', level=2)
add_styled_table(doc1,
    ['轮次', '尝试方案', '问题', '结论'],
    [
        ['第1轮', '引入行缓冲区 StringBuilder + concatMap', 'bodyToFlux 按 TCP 缓冲区切分，SSE 行被截断（如 data: {... 和 delta:...} 分属两 chunk），split 后 JSON 解析静默失败', '行缓冲区解决不完整行问题，但 concatMap 内层 Flux 订阅未传播到下游'],
        ['第2轮', 'concatMap → handle 运算符', 'handle 同步发射但仍丢失信号，emitter.send() 日志未出现', 'handle 消除了内层 Flux 订阅环节，但操作符链中仍有信号丢失'],
        ['第3轮', 'Flux.create 彻底重构', 'WebClient subscribe onNext 直接调用 sink.next()，零中间环节', 'Flux.create 简化了链路，但双层嵌套订阅的维护性和调试性较差'],
        ['第4轮', 'Controller 重构对齐已验证模式', 'WikiController 直接注入 AIService，与 ProjectController.aiSummary 模式完全一致', '最终有效方案：AIServiceImpl 保留 concatMap + StringBuilder 管道 + extractContent 用 Jackson path() 安全导航；Controller 层统一 SseEmitter + Flux.subscribe() 模式'],
    ],
    [1, 4.5, 6, 7.5]
)
doc1.add_paragraph('')
doc1.add_paragraph('最终数据流：DeepSeek API → WebClient bodyToFlux → concatMap(行缓冲区 → Flux.fromArray(lines)) → filter(data: 行) → map(extractContent) → filter(非空) → Controller subscriber → emitter.send(chunk) → SSE → 前端 ReadableStream')

doc1.add_paragraph('')
doc1.add_paragraph('本日成果：完成 Wiki 文档中心全栈功能（后端 5 文件 + 前端 4 处新增/修改），经过 4 轮 SSE 调试最终修复流式推送问题。')

# ==================== 7月9日 ====================
doc1.add_heading('三、2026年7月9日（周四）— AI 大模型接入重构 + 用户身份系统 + 智能指派 + 蓝图初始化', level=1)

doc1.add_heading('1. AI 大模型接入全面重构', level=2)
add_styled_table(doc1,
    ['序号', '任务', '产出/修改文件', '说明'],
    [
        ['1', 'AIServiceImpl 流式管道重写', 'AIServiceImpl.java', '从 Flux.create + 行缓冲区重构为 concatMap + StringBuilder 管道，消除双层嵌套订阅的信号丢失风险；extractContent() 用 Jackson path() 安全导航替代逐层判空；增加 null/blank prompt 校验'],
        ['2', 'AIController 补全', 'AIController.java', '补全 onTimeout/onError 回调，超时从 120s → 300s，增加 produces = TEXT_EVENT_STREAM_VALUE'],
        ['3', 'ProjectController 清理', 'ProjectController.java', '移除无意义的 comment:connected 初始事件'],
        ['4', '拦截器统一', 'WebConfig.java', '/api/ai/stream-chat 从拦截器白名单移除，统一要求 JWT 认证'],
        ['5', '前端 SSE 工具抽取', 'utils/sse.js', '新增共享 SSE 流式请求工具，统一处理 Bearer Token、行解析、[ERROR] 检测、缓冲区残留'],
        ['6', '前端 API 简化', 'api/summary.js + api/wiki.js', 'summary.js 98行→4行，wiki.js SSE 部分委托给 sse.js'],
    ],
    [1, 3.5, 5, 9.5]
)
doc1.add_paragraph('')

doc1.add_heading('2. 用户专业身份字段与身份选择功能', level=2)
items = [
    '数据库变更：sys_user 表新增 identity 列（varchar 50），可选值：PROJECT_MANAGER / FRONTEND_DEV / BACKEND_DEV / QA_TESTER / UI_DESIGNER',
    '后端：User.entity 新增 identity 字段，UserServiceImpl 新增 updateIdentity() 含白名单校验，register 增加 identity 参数，login 返回 identity',
    '新增 PUT /api/user/identity 接口（需 JWT 认证）',
    'LoginVO 新增 identity 字段，登录时一并返回',
    '前端：api/user.js 新增 updateIdentity()，store/user.js 新增 needsIdentityPrompt getter',
    'Dashboard.vue 新增身份选择弹窗：5 张卡片（项目经理/前端/后端/测试/UI），3 列网格布局，品牌色 hover，el-dialog 不可手动关闭（show-close=false + close-on-click-modal=false），点击后 loading 态阻止重复点击',
    '编译验证：mvn compile 通过 | vite build 通过（Dashboard chunk 8.58KB → 10.28KB）',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_heading('3. AI 智能分工指派', level=2)
add_styled_table(doc1,
    ['序号', '任务', '产出/修改文件', '说明'],
    [
        ['1', '数据库扩展', 'sql/init.sql', '新增 pm_project_member 表（project_id + user_id）+ sys_task 新增 recommended_role 列'],
        ['2', '成员实体与 Mapper', 'entity/ProjectMember.java + mapper/ProjectMemberMapper.java', '项目成员关联表 CRUD'],
        ['3', 'Task 实体扩展', 'entity/Task.java', '新增 recommendedRole 字段'],
        ['4', 'AI 拆解 Prompt 升级', 'TaskServiceImpl.java', '新增「团队配置」描述 5 种身份、要求 AI 增加 recommended_role 字段、示例 JSON 同步更新'],
        ['5', '自动指派算法', 'TaskServiceImpl.java', '查询项目成员 → 构建 identity→userId 映射 → 遍历 AI 拆解结果 → 匹配 recommended_role → 自动设置 assignee_id'],
        ['6', '项目成员接口', 'ProjectController.java', '新增 GET /api/project/{id}/members（含昵称和身份信息）'],
        ['7', '前端角色标签', 'TaskList.vue', '详情弹窗增加推荐角色彩色标签（PM Indigo/前端 Blue/后端 Emerald/测试 Amber/UI Pink）+ 指派信息显示'],
        ['8', '测试用户', 'sql/test_data.sql', 'demo 设为 BACKEND_DEV，新增 3 个测试用户分属不同身份，所有人加入项目 11'],
    ],
    [1, 3, 5, 10]
)
doc1.add_paragraph('')

doc1.add_heading('4. AI 一键生成初始项目任务/蓝图初始化', level=2)
items = [
    '新增 POST /api/task/{projectId}/ai-init-tasks 接口（@PathVariable）：检查项目是否已有主任务（已有则拒绝覆盖），构造 init Prompt → AI 生成 3-5 个阶段性大任务 → JSON 解析 → @Transactional 批量插入（parent_id=null, status=TODO, order_index 递增）→ WebSocket 广播',
    'initPrompt 设计：角色设定（资深技术项目经理+敏捷教练）、任务约束（独立完整开发阶段、按依赖排序、标题8-16字、描述20-60字）、格式约束（纯 JSON 数组，示例使用「电商平台」场景）、数量约束（3-5个顶层大任务）',
    '前端 TaskList.vue 三处改动：(1) 顶栏新增渐变动画按钮「AI 一键生成任务」(2.5s 循环 shimmer 效果) (2) 看板为空且非加载态时显示 Hero 区引导（插画 + 大标题 + 大按钮）(3) v-loading + 自定义 loading text + handleInitTasks() 调用 API → fetchTasks() 刷新',
    'api/task.js 新增 initProjectTasks(projectId)',
    '编译验证：mvn compile 通过（47 个 Java 文件）| vite build 通过（TaskList chunk 16.52KB → 18.20KB）',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_heading('5. 接口注解 Bug 修复', level=2)
items = [
    '问题：POST /api/task/ai-init-tasks 报 400 错误 "Required request parameter projectId is not present"',
    '根因：TaskController 中 ai-init-tasks 接口使用 @RequestParam 接收 projectId，但路径中未定义该参数，前端以路径变量方式调用',
    '修复：@PostMapping("/ai-init-tasks") → @PostMapping("/{projectId}/ai-init-tasks")，@RequestParam → @PathVariable',
    '同步修复前端调用：api/task.js 中 initProjectTasks() 从 query params 改为路径变量拼接',
    '全面排查：逐一检查 TaskController / ProjectController / WikiController 所有带 {projectId} / {taskId} / {id} 路径的接口，确认其余全部正确使用 @PathVariable，无类似问题',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.add_paragraph('')
doc1.add_paragraph('本日成果：完成 AI 接入全面重构（6 处修改）、用户身份系统全栈（8 处修改）、AI 智能指派（8 处修改）、蓝图初始化 + Bug 修复。项目 Java 文件从 38 个增长至 47 个。')

# ==================== 汇总 ====================
doc1.add_heading('四、本周项目文件统计', level=1)
doc1.add_paragraph('第二周累计产出（7月6日 — 7月9日）：')
add_styled_table(doc1,
    ['类别', '新增文件', '修改文件', '关键成果'],
    [
        ['后端 Java', '7 个（AnalyticsVO / AnalyticsService / AnalyticsServiceImpl / AnalyticsController / Wiki / WikiMapper / WikiService / WikiServiceImpl / WikiController / ProjectMember / ProjectMemberMapper）', '8 个（AIServiceImpl / ProjectController / ProjectServiceImpl / TaskServiceImpl / Task.java / User.java / UserService / UserServiceImpl / LoginVO / WebConfig）', '统计大屏 + Wiki CRUD + AI 重构 + 身份系统 + 智能指派 + 蓝图初始化'],
        ['前端 Vue/JS', '2 个（Analytics.vue / WikiView.vue / utils/sse.js）', '6 个（Dashboard.vue / TaskList.vue / router/index.js / api/task.js / api/project.js / store/user.js / api/summary.js / api/wiki.js）', '统计图表 + Wiki 页面 + SSE 工具 + 身份弹窗 + 指派标签 + 空项目引导'],
        ['SQL 脚本', '1 个', '—', 'test_data.sql（demo 账号 + 3 项目 + 38 任务）'],
        ['数据库变更', '2 张新表 + 3 列新增', '—', 'pm_wiki + pm_project_member 表；identity / parent_id / recommended_role 列'],
    ],
    [3, 3.5, 4.5, 8]
)
doc1.add_paragraph('')

doc1.add_heading('五、当前项目规模总览', level=1)
add_styled_table(doc1,
    ['层级', '文件数', '状态', '说明'],
    [
        ['后端 Java', '47 个', 'mvn compile 通过', '入口 1 + common 13 + controller 5 + dto 2 + entity 5 + mapper 5 + service 13 + vo 2 + websocket 2'],
        ['前端 Vue/JS', '19 个源文件', 'vite build 通过', 'views 4 + api 5 + store 1 + router 1 + utils 2 + 配置文件 6'],
        ['数据库', '5 张表', '运行中', 'sys_user / sys_project / sys_task / pm_wiki / pm_project_member'],
        ['SQL 脚本', '2 个', '就绪', 'init.sql（建表） + test_data.sql（测试数据 3项目+38任务）'],
        ['部署文件', '4 个', '可部署', 'Dockerfile + nginx.conf + docker-compose.yml + .dockerignore'],
    ],
    [3, 2.5, 3.5, 10]
)
doc1.add_paragraph('')

doc1.add_heading('六、本周关键 Bug 修复汇总', level=1)
add_styled_table(doc1,
    ['序号', '问题', '根因', '修复方案', '影响范围'],
    [
        ['1', 'AI 总结弹窗空白', 'SSE data: 冒号后空格格式不匹配，DeepSeek 返回 data:{...} 但代码 filter "data: "（有空格）', 'extractData() 兼容两种格式 + flatMap 换行拆分 + extractContent 自适应截取 + 三层异常防护', 'AIServiceImpl / ProjectServiceImpl / ProjectController'],
        ['2', 'Dashboard 无限加载', '@CrossOrigin(origins="*") + WebConfig allowCredentials(true) 叠加导致 CORS 预检失败', '移除 Controller @CrossOrigin，CORS 收紧为 /api/**，Dashboard.vue 增加 try/catch/finally 防御', 'ProjectController / WebConfig / Dashboard.vue'],
        ['3', 'AI 写作 SSE 无输出（4轮调试）', 'bodyToFlux 按 TCP 切分导致 SSE 行截断 + concatMap 内层 Flux 订阅丢失 + 操作符链信号传播失败', 'Flux.create 重构 → Controller 对齐已验证模式 → 最终 concatMap + StringBuilder 管道 + Jackson path() 安全导航', 'AIServiceImpl / WikiController'],
        ['4', 'Analytics 页面 404', '新增 Controller 未被运行中的 Spring Boot 进程加载（无 DevTools 热加载）', '重启后端 → Controller 被 Spring 扫描注册', '运维操作'],
        ['5', 'ai-init-tasks 400 报错', '@RequestParam Long projectId 但前端以路径变量方式调用', '@RequestParam → @PathVariable，@PostMapping 路径增加 {projectId}，前端同步修改', 'TaskController / api/task.js'],
    ],
    [1, 3.5, 5.5, 5, 4]
)
doc1.add_paragraph('')

doc1.add_heading('七、下周计划', level=1)
items = [
    '用户管理完善：头像上传、个人信息编辑、密码修改',
    '项目协作功能：多人共享项目、邀请成员、权限分级',
    '任务增强：标签系统、优先级分级、附件上传、评论功能、工时估算',
    '通知系统：任务分配通知、逾期提醒（站内信 + 邮件）',
    '单元测试与集成测试补充',
    'CI/CD 流水线配置（GitHub Actions 或 Jenkins）',
]
for item in items:
    doc1.add_paragraph(item, style='List Bullet')

doc1.save(os.path.join(BASE_DIR, '第二周周报.docx'))
print("OK: 第二周周报.docx generated")


# ===================================================================
# 第二周AI使用记录
# ===================================================================
doc2 = Document()

style2 = doc2.styles['Normal']
style2.font.name = '微软雅黑'
style2.font.size = Pt(10.5)
style2.paragraph_format.space_after = Pt(4)
style2.paragraph_format.line_spacing = 1.35
style2.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')

title2 = doc2.add_heading('SmartPM 项目 — 第二周 AI 使用记录', level=0)
title2.alignment = WD_ALIGN_PARAGRAPH.CENTER
doc2.add_paragraph('记录周期：2026年7月6日 — 2026年7月9日', style='Normal').alignment = WD_ALIGN_PARAGRAPH.CENTER
doc2.add_paragraph('')

doc2.add_heading('一、AI 使用概述', level=1)
doc2.add_paragraph(
    '本周 AI 使用延续上周的"双轨并行"模式：\n'
    '1) 开发辅助层面：通过 Claude Code 进行代码编写、架构设计、Bug 诊断修复等全栈开发工作；\n'
    '2) 产品功能层面：系统中 DeepSeek 大模型（已切换至 deepseek-v4-flash）的集成进一步深化，新增了 Wiki AI 写作协同和 AI 一键生成项目任务等产品功能。\n\n'
    '特别说明：本周 AI 开发辅助的显著特点是"深度调试驱动"——多轮次定位和修复 SSE 流式推送相关 Bug，涉及从 TCP 缓冲区切分到操作符链信号传播的底层问题排查。'
)

# ==================== 7月6日 ====================
doc2.add_heading('二、2026年7月6日 — 项目卡片增强 + 统计大屏 + AI 流式修复', level=1)

doc2.add_paragraph('一、Claude Code 开发辅助任务：', style='Normal')
items = [
    'Dashboard 卡片编辑/删除功能：生成三点下拉菜单布局（el-dropdown + MoreFilled 图标）、编辑弹窗（预填当前值）、删除确认（ElMessageBox.confirm）、阻止事件冒泡（@click.stop）',
    '后端项目更新接口：生成 PUT /api/project/update（存在性校验 + 创建者权限校验），优化级联删除 SQL 顺序（子任务 → 主任务 → 项目）',
    '数据统计大屏全栈：设计 AnalyticsVO 响应体结构（3 个内部类）、生成统计计算逻辑（项目数/任务数/状态分布/排行/7天趋势）、ECharts 图表配置（环形饼图/水平柱状图/平滑折线图）',
    'Analytics.vue 页面：四数据卡片 + 三图表布局、ECharts 生命周期管理（onMounted 初始化 → resize 自适应 → onUnmounted dispose）、路由配置 + Dashboard 入口按钮',
    'SSE 空白 Bug 诊断：精确定位 data: 冒号后空格格式不匹配问题 → flatMap 换行拆分 → extractData 兼容两种格式 → extractContent 自适应截取 → 三层异常防护（同步/异步/超时）',
    'AI 启动寄语兜底：当项目无历史任务时，不返回硬编码文案，改为构造 buildKickoffPrompt() 调用 AI 生成专业启动文档',
    '测试数据设计：生成 3 个项目 + 38 条任务（含逾期/子任务/7天趋势分布的逼真业务数据）+ demo 体验账号',
    'Analytics 404 排查：逐层验证请求链路（前端→Axios→Vite代理→Controller→CORS→拦截器）→ 定位为后端未重启导致新类未加载',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')
doc2.add_paragraph('')
doc2.add_paragraph('AI 交互统计：代码生成 ~15 轮 | Bug 诊断 ~6 轮 | 架构设计 ~3 轮 | 数据设计 ~2 轮')

# ==================== 7月7日 ====================
doc2.add_heading('三、2026年7月7日 — Wiki 文档中心 + AI 写作协同 + SSE 多轮调试', level=1)

doc2.add_paragraph('一、Claude Code 开发辅助任务：', style='Normal')
items = [
    'Wiki 后端 CRUD 全栈：生成 pm_wiki 建表 SQL → Wiki.java 实体 → WikiMapper → WikiService/WikiServiceImpl（含权限校验 + AI Prompt 构造）→ WikiController（5 个 CRUD + 1 个 SSE 端点）',
    'Wiki 前端页面：左侧 280px 文档列表（新建/切换/删除，按更新时间倒序，当前文档高亮）+ 右侧 md-editor-v3 全功能编辑器 + AI 写作面板 380px（指令输入 + 划选自动填入 + streamChat 流式输出 + 替换/插入按钮）',
    '导航入口集成：TaskList.vue 顶栏新增"文档中心"按钮 + Dashboard.vue 项目卡片下拉菜单新增"文档中心"入口',
    'SSE 流式推送 Bug 4 轮排查（详见下方调试记录）：行缓冲区引入 → concatMap→handle 运算符 → Flux.create 彻底重构 → Controller 重构对齐已验证模式',
    '并行尝试了多种操作符组合（concatMap / flatMap / handle / Flux.create），最终确定 concatMap + StringBuilder + Jackson path() 为最稳定方案',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')

doc2.add_paragraph('')
doc2.add_paragraph('二、SSE 流式推送 Bug 多轮调试详细记录（AI 辅助诊断全过程）：', style='Normal')
add_styled_table(doc2,
    ['轮次', 'AI 提出的方案', '尝试的操作符/架构', '发现的问题', '学到的东西'],
    [
        ['第1轮', '引入行缓冲区，用 concatMap + processRawChunk() 累积到完整行再解析', 'bodyToFlux → concatMap(行缓冲区 → Flux.fromIterable)', 'bodyToFlux 按 TCP 缓冲区切分，SSE 行在中途被截断（如 data: {"choices"... 和 "delta":...} 分属两个 chunk），flatMap split 后 JSON 静默失败', 'TCP 层的 chunk 边界不等于应用层消息边界，需要行缓冲区解决不完整行问题'],
        ['第2轮', 'concatMap 内层 Flux 订阅未传播，改用 handle 同步发射', '.handle((chunk, sink) → sink.next(content))', 'handle 打印了内容但 emitter.send() 日志未出现，操作符链中信号仍然丢失', 'handle 消除了内层 Flux 订阅环节，但在复杂的操作符链中仍存在信号传播不确定性'],
        ['第3轮', '使用 Flux.create 彻底消除中间环节', 'Flux.create(sink → { webClient.subscribe(onNext → sink.next(content)) })', '双层嵌套订阅的维护性和调试性较差，但链路最短', 'Flux.create 是 Reactor 中最底层、最灵活的创建方式，适合需要完全控制信号发射的场景'],
        ['第4轮', 'Controller 层重构，对齐已验证模式', 'WikiController 直接注入 AIService + 构造 Prompt + aiService.streamChat().subscribe(emitter.send)', 'ProjectController.aiSummary 的 SseEmitter + Flux.subscribe() 模式是已验证可工作的，WikiController 对齐后立即生效', '当有多种实现方式时，优先对齐已验证可工作的模式，而非另起炉灶'],
    ],
    [1, 3.5, 4.5, 5, 4]
)
doc2.add_paragraph('')
doc2.add_paragraph('总结：这次多轮调试是本周 AI 辅助开发中最具挑战性的环节。AI 能够快速提出多种备选方案（4 种不同的操作符架构），并在每次失败后分析日志、缩小问题范围、提出下一步尝试方向。最终通过"对齐已验证模式"的折衷策略解决问题。')
doc2.add_paragraph('')
doc2.add_paragraph('AI 交互统计：代码生成 ~12 轮 | SSE 调试 ~10 轮 | 架构讨论 ~3 轮')

# ==================== 7月9日 ====================
doc2.add_heading('四、2026年7月9日 — AI 重构 + 身份系统 + 智能指派 + 蓝图初始化', level=1)

doc2.add_paragraph('一、Claude Code 开发辅助任务：', style='Normal')
items = [
    'AI 大模型接入全面重构：AIServiceImpl 流式管道从 Flux.create + 行缓冲区重写为 concatMap + StringBuilder 管道，extractContent() 用 Jackson path() 安全导航替代逐层判空，AIController 补全 onTimeout/onError 回调',
    '前端 SSE 工具抽取：新增 utils/sse.js 共享 SSE 流式请求工具，统一处理 Bearer Token / 行解析 / [ERROR] 检测 / 缓冲区残留，summary.js 从 98 行缩减至 4 行',
    '用户身份系统全栈：sys_user 新增 identity 列 → User.entity 新增字段 → updateIdentity() 白名单校验 → PUT /api/user/identity 接口 → LoginVO 返回 identity → 前端 store 新增 needsIdentityPrompt getter',
    '身份选择弹窗设计：5 张不可手动关闭的卡片（el-dialog show-close=false + close-on-click-modal=false），3 列网格布局，品牌色 hover，loading 态防重复点击',
    'AI 智能分工指派：新增 pm_project_member 表 + sys_task.recommended_role 列 → 升级 Prompt 加入团队配置描述 → 自动指派算法（identity→userId 映射匹配）→ 前端角色彩色标签',
    'AI 一键生成项目任务：设计 initPrompt（资深技术项目经理+敏捷教练角色/任务约束/格式约束/数量约束）→ @Transactional 批量插入 → WebSocket 广播 → 前端空项目 Hero 引导区 + shimmer 渐变动画按钮',
    '接口注解 Bug 修复：排查 TaskController 中 @RequestParam 与 @PathVariable 误用，修复 ai-init-tasks 接口 + 同步前端调用路径，全面检查所有 Controller 的路径参数注解',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')

doc2.add_paragraph('')
doc2.add_paragraph('二、DeepSeek 大模型产品功能深化（本周新增/修改）：', style='Normal')
add_styled_table(doc2,
    ['功能', 'API 接口', '调用方式', '本周变更', '状态'],
    [
        ['任务智能拆解（升级）', 'POST /api/task/{id}/ai-decompose', 'WebClient 非流式 (stream:false)', 'Prompt 增加团队配置描述 + recommended_role 字段 + 自动指派算法', '已升级'],
        ['项目周报生成', 'GET /api/project/{id}/ai-summary', 'WebClient SSE 流式', '修复 SSE 格式兼容 + null 安全加固 + 空项目 AI 启动寄语兜底', '已修复'],
        ['Wiki AI 写作协同（新增）', 'GET /api/wiki/ai-copilot', 'WebClient SSE 流式', '全新功能：Prompt 指令驱动 + 划选文本输入 + 流式输出 + 替换/插入', '新增'],
        ['AI 一键生成任务（新增）', 'POST /api/task/{projectId}/ai-init-tasks', 'WebClient 非流式 (stream:false)', '全新功能：空项目蓝图初始化，AI 生成 3-5 个阶段性大任务', '新增'],
        ['通用流式对话', 'GET /api/ai/stream-chat', 'WebClient SSE 流式', '超时延长至 300s + JWT 认证统一 + onTimeout/onError 回调补全', '已完善'],
    ],
    [3, 4.5, 3.5, 5, 1.5]
)
doc2.add_paragraph('')

doc2.add_paragraph('三、本周 AI 模型配置变更：', style='Normal')
items = [
    '模型切换：deepseek-chat → deepseek-v4-flash（更快的响应速度，适合开发调试阶段的频繁调用）',
    'SSE 超时统一：120s/180s → 300s（给 DeepSeek 大模型更充裕的响应时间）',
    'AI 接口鉴权统一：/api/ai/stream-chat 从拦截器白名单移除，全部 AI 接口统一要求 JWT 认证',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')

doc2.add_paragraph('')
doc2.add_paragraph('AI 交互统计：代码生成 ~18 轮 | Prompt 设计 ~4 轮 | 重构 ~6 轮 | Bug 修复 ~2 轮')

# ==================== 汇总 ====================
doc2.add_heading('五、AI 使用汇总', level=1)

doc2.add_heading('1. Claude Code 开发辅助统计', level=2)
add_styled_table(doc2,
    ['统计指标', '第1周数值', '第2周数值', '累计', '说明'],
    [
        ['总交互轮次', '~80+ 轮', '~75+ 轮', '~155+ 轮', '代码生成 + 配置编写 + 调试诊断 + 架构设计 + 文档撰写'],
        ['新增 Java 文件', '34 个', '13 个', '47 个', '第2周新增：Analytics(4) + Wiki(5) + ProjectMember(2) + 其他(2)'],
        ['新增前端文件', '16 个', '3 个', '19 个', '第2周新增：Analytics.vue + WikiView.vue + utils/sse.js'],
        ['新增部署/配置文件', '8 个', '1 个', '9 个', '第2周新增：sql/test_data.sql'],
        ['数据库表', '3 张', '2 张', '5 张', '第2周新增：pm_wiki + pm_project_member'],
        ['Bug 诊断修复', '~8 个', '~5 个', '~13 个', '第2周重点：SSE 流式空白(4轮调试) / CORS 冲突 / Analytics 404 / 接口注解错误'],
        ['Prompt 工程设计', '3 个', '3 个', '6 个', '第2周新增：AI 写作 Prompt / initPrompt / 指派 Prompt 升级'],
        ['SSE 调试专项', '—', '~10 轮', '~10 轮', '第2周独有的深度调试：4 种操作符架构尝试'],
    ],
    [4, 2.5, 2.5, 2.5, 7.5]
)
doc2.add_paragraph('')

doc2.add_heading('2. DeepSeek 大模型产品功能统计', level=2)
add_styled_table(doc2,
    ['AI 功能', '技术方案', '后端文件', '前端文件', '本周状态变化'],
    [
        ['通用流式对话', 'WebClient + SseEmitter', 'AIController / AIService / AIServiceImpl / AIConfigProperties / AIWebClientConfig', '—（后端直接测试）', '超时延长 + JWT 鉴权 + 回调补全'],
        ['任务智能拆解', 'WebClient 非流式 + JSON 解析三级防御', 'TaskService.decomposeTask() + TaskController', 'api/task.js + TaskList.vue', 'Prompt 升级 + 自动指派算法 + 角色推荐'],
        ['项目周报生成', 'WebClient SSE 流式 + Markdown 模板', 'ProjectService.generateSummary() + ProjectController', 'api/summary.js + TaskList.vue', 'SSE 格式修复 + null 安全 + AI 启动寄语兜底'],
        ['Wiki AI 写作协同（新增）', 'WebClient SSE 流式 + AIService 直调', 'WikiController', 'api/wiki.js + WikiView.vue', '全新功能：指令驱动写作 + 划选输入 + 流式输出'],
        ['AI 一键生成任务（新增）', 'WebClient 非流式 + JSON 解析', 'TaskService.initTasks() + TaskController', 'api/task.js + TaskList.vue', '全新功能：空项目蓝图初始化'],
    ],
    [3, 4, 5, 4, 4]
)
doc2.add_paragraph('')

doc2.add_heading('3. 本周 AI 使用新特征分析', level=2)

doc2.add_heading('3.1 调试驱动开发模式', level=3)
items = [
    'SSE 流式推送 Bug 经历了 4 轮 AI 辅助调试，每轮 AI 提出不同方案（行缓冲区 → handle → Flux.create → Controller 对齐），体现了 AI 在复杂异步编程问题上的多方案探索能力',
    'AI 能够在每次失败后根据新日志缩小问题范围：从"SSE 无输出" → "数据已解析但 emitter.send() 未触发" → "操作符链信号丢失" → "订阅传播失败" → 最终定位',
    '经验总结：当遇到框架底层行为相关的问题时，AI 可提供多种备选方案，但最终需要实际运行验证来确定有效方案',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')

doc2.add_heading('3.2 代码重构与抽象能力', level=3)
items = [
    '前端 SSE 工具抽取：从两个文件（summary.js 98行 + wiki.js 部分）中识别出共同的 SSE 解析模式，抽取为 utils/sse.js 共享模块（~60行），summary.js 缩减至 4 行',
    'AIServiceImpl 流式管道重构：从 Flux.create + 行缓冲区重构为 concatMap + StringBuilder 管道，每个步骤可独立测试和调试',
    'Controller 层模式统一：AIController / ProjectController / WikiController 三个 SSE 端点统一为相同的 SseEmitter + Flux.subscribe() + 超时/错误回调模式',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')

doc2.add_heading('3.3 Prompt 工程持续迭代', level=3)
items = [
    '任务拆解 Prompt 升级：从基础的角色设定+格式约束，升级为增加团队配置描述（5 种身份）、recommended_role 字段要求、正确/错误示例对照',
    '蓝图初始化 Prompt 设计：全新场景（空项目启动），角色设定为"资深技术项目经理+敏捷教练"，约束条件（独立完整开发阶段、按依赖排序、标题8-16字、描述20-60字），示例使用"电商平台"场景避免歧义',
    'AI 写作 Prompt：指令驱动模式（用户输入自然语言指令 + 待处理文本），要求直接输出处理结果不带解释',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')

doc2.add_heading('4. AI 使用成效评估', level=2)
doc2.add_paragraph('成效优势：')
items = [
    '深度调试能力：AI 在 SSE 流式推送这一涉及 TCP 缓冲区切分、Reactor 操作符链、SSE 协议格式的多层技术栈问题上，能够逐层排查并提出多种备选方案，最终在 4 轮尝试后定位并修复',
    '产品功能深化：成功将 AI 大模型嵌入 4 个不同业务场景（任务拆解 + 周报生成 + 写作协同 + 蓝图初始化），每个场景都有独特的数据流和 Prompt 设计',
    '代码质量提升：通过重构（SSE 工具抽取、流式管道优化、Controller 模式统一）持续改善代码结构和可维护性',
    '全栈效率：4 天完成 13 个后端文件 + 3 个前端文件 + 5 个 Bug 修复 + 多个功能迭代，传统方式预估 1-2 周',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')

doc2.add_paragraph('待改进：')
items = [
    'SSE 调试过程暴露了对 Reactor 底层机制理解不足的问题，建议补充 Project Reactor 官方文档的系统学习',
    'AI 生成的前端代码有时包含未使用的 import 或变量，需要在编译阶段通过构建工具（Vite build warning）捕获',
    '部分 AI 辅助的架构决策（如 Flux.create vs concatMap）缺乏性能基准测试，建议后续对关键路径进行 JMH 基准测试',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')

doc2.add_heading('5. 后续 AI 使用计划', level=2)
items = [
    'AI 功能扩展：自然语言创建任务（"明天完成登录页面"→ 自动解析为 Task 并设置截止日期）、智能排期建议（根据当前任务负载推荐合理的开始/截止日期）',
    'Token 消耗监控：在 AIServiceImpl 中增加 usage 字段日志（prompt_tokens / completion_tokens / total_tokens），便于成本分析和 Prompt 长度优化',
    'Prompt 版本管理：将 6 个 Prompt 模板从 Java 代码中抽取到独立配置文件或数据库，支持非代码修改和 A/B 测试',
    'AI 输出缓存：对相同 Prompt 的重复调用（如多个用户对同一项目生成周报）引入短期缓存，减少 API 调用成本',
    '性能优化：对非流式 AI 调用（任务拆解/蓝图初始化）引入超时熔断机制，防止大模型响应过慢阻塞用户操作',
]
for item in items:
    doc2.add_paragraph(item, style='List Bullet')

doc2.save(os.path.join(BASE_DIR, '第二周ai使用记录.docx'))
print("OK: 第二周ai使用记录.docx generated")
