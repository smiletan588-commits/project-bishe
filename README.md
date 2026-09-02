# SmartPM

SmartPM 是一个融合 AI 任务拆解、岗位协作、实时看板和项目数据分析的项目管理平台。

## 一键启动

### Windows

确保 Docker Desktop 已启动，然后双击 `start.bat`。

### macOS / Linux

```bash
./start.sh
```

也可以在项目根目录执行：

```bash
docker compose up -d --build
```

启动完成后访问 <http://localhost>。

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
