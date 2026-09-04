# SmartPM

SmartPM 是一个融合 AI 任务拆解、岗位协作、实时看板和项目数据分析的项目管理平台。

## 一键启动

### Windows

双击 `start.bat` 即可启动。脚本会优先使用 Docker；如果电脑还没有 Docker Desktop，会自动切换到本地开发模式，启动本地 Spring Boot 后端和 Vue 前端。

本地开发模式需要：Java 17、Maven、Node.js/npm，以及运行在 `localhost:3306` 的 MySQL。安装并启动 Docker Desktop 后，脚本会自动使用完整容器模式。

### macOS / Linux

```bash
./start.sh
```

也可以在项目根目录执行：

```bash
docker compose up -d --build
```

Docker 模式启动完成后访问 <http://localhost>；本地开发模式访问 <http://localhost:3000>。

## 环境变量

可在项目根目录创建 `.env` 文件：

```env
MYSQL_ROOT_PASSWORD=修改为安全密码
AI_API_KEY=你的模型接口密钥
```

AI 功能需要配置 `AI_API_KEY`；不配置时，基础项目管理功能仍可启动。

## 常用操作

```bash
docker compose ps
docker compose logs -f backend
docker compose down
```

MySQL 和 Redis 数据保存在 Docker 命名卷中。首次启动时会自动执行 `sql/init.sql`。
