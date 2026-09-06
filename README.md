# SmartPM

SmartPM 是一个融合 AI 任务拆解、岗位协作、实时看板和项目数据分析的项目管理平台。

## 一键启动

### Windows

安装 Docker Desktop 后，双击 `start.bat` 即可一键构建并启动 MySQL、Redis、Spring Boot 后端和 Nginx 前端。Docker Desktop 未运行时，脚本会尝试自动启动并等待其就绪。

首次运行需要下载基础镜像和项目依赖，可能耗时数分钟。启动完成后脚本会自动打开 <http://localhost:3000>；数据库数据保存在 Docker 命名卷中，关闭启动窗口不会停止服务。

如果 Windows/Hyper-V 占用了 `3000` 端口，启动脚本会请求一次管理员权限，将异常的低位动态端口范围恢复为 Windows 标准范围，然后继续启动 Docker。

### macOS / Linux

```bash
./start.sh
```

也可以在项目根目录执行：

```bash
docker compose up -d --build
```

启动完成后访问 <http://localhost:3000>。

## 环境变量

可在项目根目录创建 `.env` 文件：

```env
MYSQL_ROOT_PASSWORD=修改为安全密码
AI_API_KEY=你的模型接口密钥
# 默认使用国内 Docker Hub 镜像代理；海外环境可改为 docker.io
DOCKER_REGISTRY_MIRROR=m.daocloud.io/docker.io
```

AI 功能需要配置 `AI_API_KEY`；不配置时，基础项目管理功能仍可启动。

## 常用操作

```bash
docker compose ps
docker compose logs -f backend
docker compose down
```

MySQL 和 Redis 数据保存在 Docker 命名卷中。首次启动时会自动执行 `sql/init.sql`。
