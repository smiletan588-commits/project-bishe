const { chromium } = require('playwright')
const path = require('path')

const baseUrl = process.env.SMARTPM_PREVIEW_URL || 'http://127.0.0.1:4173'
const outputDir = process.env.SMARTPM_SCREENSHOT_DIR || path.join(process.cwd(), 'visual-output')
const routes = ['/login', '/dashboard', '/project/1', '/analytics', '/project/1/wiki?projectName=毕业设计协作平台', '/project/1/manage', '/admin/users', '/recycle-bin']
const widths = [1440, 1024, 768, 390]

const projects = [{ id: 1, name: '毕业设计协作平台', description: '面向软件项目团队的任务、文档与进度协作平台', createTime: '2026-09-01T09:30:00', ownerId: 1, memberCount: 6 }]
const tasks = [
  { id: 11, projectId: 1, title: '完成移动端任务看板适配', description: '保证窄屏下筛选和任务操作可用', status: 'TODO', priority: 'HIGH', assigneeId: 2, assigneeName: '林悦', dueDate: '2026-09-12', startDate: '2026-09-07', estimatedHours: 8, actualHours: 0 },
  { id: 12, projectId: 1, title: '联调项目成员权限接口', description: '覆盖管理员和普通成员路径', status: 'IN_PROGRESS', priority: 'MEDIUM', assigneeId: 3, assigneeName: '陈川', dueDate: '2026-09-10', startDate: '2026-09-05', estimatedHours: 12, actualHours: 7 },
  { id: 13, projectId: 1, title: '整理毕业答辩演示数据', description: '准备项目列表、进度分析与里程碑示例', status: 'DONE', priority: 'LOW', assigneeId: 1, assigneeName: '张明', dueDate: '2026-09-06', startDate: '2026-09-01', estimatedHours: 5, actualHours: 5 },
  { id: 14, projectId: 1, title: '实现一个用于验证超长任务标题在任务卡片和甘特图中是否能够正确截断而不会撑破页面布局的测试任务', description: '', status: 'TODO', priority: 'MEDIUM', assigneeName: '林悦', dueDate: '2026-09-18', startDate: '2026-09-08', estimatedHours: 10, actualHours: 0 }
]
const members = [{ userId: 1, username: 'admin', nickname: '张明', identity: 'PROJECT_MANAGER', permission: 'ADMIN' }, { userId: 2, username: 'linyue', nickname: '林悦', identity: 'FRONTEND_DEV', permission: 'MEMBER' }, { userId: 3, username: 'chenchuan', nickname: '陈川', identity: 'BACKEND_DEV', permission: 'MEMBER' }]

function dataFor(url) {
  const pathname = new URL(url).pathname.replace(/^\/api/, '')
  if (pathname === '/project/list') return projects
  if (pathname.endsWith('/members')) return members
  if (pathname === '/task/list/1') return tasks
  if (pathname.includes('/subtasks') || pathname.includes('/attachments')) return []
  if (pathname.endsWith('/milestones')) return [{ id: 1, name: '功能验收', description: '完成核心流程验收', targetDate: '2026-09-16', status: 'PLANNED', taskIds: '11,12,13' }]
  if (pathname === '/analytics/overview') return { totalProjects: 4, totalTasks: 27, completedTasks: 13, inProgressTasks: 8, statusDistribution: [{ status: 'TODO', count: 6 }, { status: 'IN_PROGRESS', count: 8 }, { status: 'DONE', count: 13 }], projectTaskRanking: [{ projectName: '毕业设计协作平台', taskCount: 14 }, { projectName: '课程管理系统', taskCount: 8 }, { projectName: '移动端原型', taskCount: 5 }], dailyCompletedTrend: ['09-01','09-02','09-03','09-04','09-05','09-06','09-07'].map((date, index) => ({ date: `2026-${date}`, count: [1, 3, 2, 4, 2, 5, 3][index] })) }
  if (pathname === '/admin/users') return members.map((member, index) => ({ id: member.userId, username: member.username, nickname: member.nickname, identity: member.identity, systemRole: index === 0 ? 'ADMIN' : 'USER', status: 'ACTIVE', createdAt: '2026-08-20T10:00:00' }))
  if (pathname === '/recycle-bin') return [{ id: 7, type: 'TASK', title: '旧版登录页视觉调整', projectName: '毕业设计协作平台', deletedByName: '张明', deletedAt: '2026-09-06T15:20:00' }]
  if (pathname === '/wiki/list/1') return [{ id: 21, title: '需求说明与验收标准', updateTime: '2026-09-07T11:30:00' }, { id: 22, title: '后端接口联调记录', updateTime: '2026-09-06T17:20:00' }]
  if (pathname === '/wiki/21' || pathname === '/wiki/22') return { id: Number(pathname.slice(6)), title: '需求说明与验收标准', content: '# 项目目标\n\n建立清晰、稳定的团队协作工作流。' }
  return []
}

;(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe' })
  const results = []
  for (const width of widths) {
    const context = await browser.newContext({ viewport: { width, height: width <= 390 ? 844 : 900 }, deviceScaleFactor: 1 })
    const page = await context.newPage()
    const pageErrors = []
    page.on('pageerror', error => pageErrors.push(error.message))
    await page.route('**/*', route => {
      const pathname = new URL(route.request().url()).pathname
      if (!pathname.startsWith('/api/')) return route.continue()
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data: dataFor(route.request().url()) }) })
    })
    await page.goto(`${baseUrl}/login`, { waitUntil: 'domcontentloaded' })
    for (const routePath of routes) {
      await page.evaluate(routePath => {
        if (routePath === '/login') localStorage.clear()
        else {
          localStorage.setItem('token', 'visual-check-token')
          localStorage.setItem('userInfo', JSON.stringify({ userId: 1, username: 'admin', identity: 'PROJECT_MANAGER', systemRole: 'ADMIN' }))
          localStorage.removeItem('smartpm.sidebar.collapsed')
        }
      }, routePath)
      await page.goto(`${baseUrl}${routePath}`, { waitUntil: 'networkidle' })
      await page.waitForTimeout(150)
      const dimensions = await page.evaluate(() => ({ viewport: innerWidth, document: document.documentElement.scrollWidth, body: document.body.scrollWidth, textLength: document.body.innerText.length, htmlLength: document.body.innerHTML.length }))
      results.push({ width, route: routePath, url: page.url(), overflow: Math.max(dimensions.document, dimensions.body) - dimensions.viewport, textLength: dimensions.textLength, htmlLength: dimensions.htmlLength, errors: [...pageErrors] })
      pageErrors.length = 0
      const screenshotRoutes = width === 1440 ? ['/login', '/dashboard', '/analytics', '/project/1/wiki?projectName=毕业设计协作平台', '/project/1/manage'] : width === 390 ? ['/login', '/dashboard', '/project/1', '/project/1/wiki?projectName=毕业设计协作平台', '/project/1/manage', '/admin/users', '/recycle-bin'] : []
      if (screenshotRoutes.includes(routePath)) {
        const name = routePath.replaceAll('/', '_').replaceAll('?', '_').replace(/^_/, '') || 'home'
        await page.screenshot({ path: path.join(outputDir, `${name}-${width}.png`), fullPage: true })
      }
    }
    if (width === 1440) {
      await page.goto(`${baseUrl}/dashboard`, { waitUntil: 'networkidle' })
      await page.getByRole('button', { name: '新建项目' }).click()
      results.push({ width, route: 'create-project-dialog', visible: await page.locator('.el-dialog').isVisible() })
      await page.keyboard.press('Escape')
      await page.goto(`${baseUrl}/project/1`, { waitUntil: 'networkidle' })
      await page.getByRole('button', { name: '新建任务' }).first().click()
      results.push({ width, route: 'create-task-dialog', visible: await page.locator('.el-dialog').isVisible() })
    }
    if (width === 1024) {
      await page.goto(`${baseUrl}/dashboard`, { waitUntil: 'networkidle' })
      results.push({ width, route: 'tablet-sidebar', sidebarWidth: await page.locator('.shell-sidebar').evaluate(element => Math.round(element.getBoundingClientRect().width)) })
    }
    if (width === 390) {
      await page.goto(`${baseUrl}/dashboard`, { waitUntil: 'networkidle' })
      const drawerButton = page.getByRole('button', { name: '打开导航' })
      if (await drawerButton.count()) {
        await drawerButton.click()
        results.push({ width, route: 'mobile-drawer', visible: await page.locator('.shell-sidebar.is-mobile-open').isVisible() })
        await page.getByRole('button', { name: '关闭导航' }).first().click()
      } else results.push({ width, route: 'mobile-drawer', visible: false, url: page.url() })
      await page.goto(`${baseUrl}/project/1`, { waitUntil: 'networkidle' })
      await page.locator('.mobile-status-tabs button').filter({ hasText: '进行中' }).click()
      results.push({ width, route: 'mobile-board-segment', active: (await page.locator('.column.mobile-active .column-header').innerText()).includes('进行中') })
      await page.goto(`${baseUrl}/project/1/wiki?projectName=毕业设计协作平台`, { waitUntil: 'networkidle' })
      await page.getByRole('button', { name: '文档列表' }).click()
      results.push({ width, route: 'mobile-wiki-drawer', visible: await page.locator('.wiki-sidebar.is-open').isVisible() })
    }
    await context.close()
  }
  await browser.close()
  console.log(JSON.stringify(results, null, 2))
})().catch(error => { console.error(error); process.exit(1) })
