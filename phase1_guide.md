# 智能协同看板系统 - 第一阶段（基础骨架与CRUD）开发指南

本指南旨在指导如何从零搭建一个基于 **Spring Boot 3** 和 **Vue 3** 的前后端分离项目，实现用户认证、项目管理及任务管理的基础功能。

---

## 一、 技术栈与工具版本推荐

*   **后端**：Java 17+, Spring Boot 3.x, MySQL 8.0, MyBatis-Plus, JWT
*   **前端**：Node.js 18+, Vite, Vue 3 (Composition API), Pinia, Vue Router, Element Plus, Axios
*   **API工具**：Apifox / Postman

---

## 二、 基础程序框架（目录结构）

### 1. 后端项目结构 (Spring Boot)
推荐采用标准的 MVC 三层架构：

```text
kanban-backend
├── src/main/java/com/example/kanban
│   ├── common               # 通用工具类与全局配置
│   │   ├── config           # 配置类（MyBatisPlus, Security, Cors）
│   │   ├── exception        # 全局异常处理
│   │   ├── result           # 统一返回结果封装 (R.java)
│   │   └── utils            # 工具类（JWTUtil等）
│   ├── controller           # 控制层（API 路由接收）
│   │   ├── UserController.java
│   │   ├── ProjectController.java
│   │   └── TaskController.java
│   ├── entity               # 实体类（与数据库表一一对应）
│   ├── mapper               # 数据访问层接口 (MyBatis)
│   ├── service              # 业务逻辑层接口
│   │   └── impl             # 业务逻辑层实现类
│   └── KanbanApplication.java
└── src/main/resources
    ├── mapper               # XML 映射文件（可选）
    └── application.yml      # 全局配置文件



2. 前端项目结构 (Vue 3 + Vite)
推荐采用模块化结构：
kanban-frontend
├── src
│   ├── api                  # API 请求模块（统一管理接口）
│   │   ├── user.js
│   │   ├── project.js
│   │   └── task.js
│   ├── assets               # 静态资源（图片、全局样式）
│   ├── components           # 公共组件
│   ├── router               # 路由配置 (index.js, guards.js)
│   ├── store                # 状态管理 (Pinia)
│   │   └── user.js          # 用户状态管理
│   ├── utils                # 工具类（request.js - Axios拦截器）
│   ├── views                # 页面组件
│   │   ├── Login.vue        # 登录/注册页面
│   │   ├── Dashboard.vue    # 项目列表/大屏页面
│   │   └── TaskList.vue     # 任务列表页面（表格/基础列表模式）
│   ├── App.vue
│   └── main.js
├── vite.config.js
└── package.jso