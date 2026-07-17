# 开发日志

## 2026-07-17

### 今日完成

- 根据需求文档和方案文档，启动第一版 MVP 骨架开发。
- 创建后端 Spring Boot 3 工程：`backend/`。
- 创建前端 Vue 3 + TypeScript 工程：`frontend/`。
- 创建数据库初始化脚本目录：`sql/`。
- 创建本地开发脚本目录：`scripts/`。
- 更新根目录 `README.md`，补充项目定位、技术栈、启动方式、MVP 范围和后续开发顺序。

### 后端进展

已完成 Spring Boot 后端基础骨架：

- 应用入口：`backend/src/main/java/com/example/delivery/DeliveryApplication.java`
- 配置文件：
  - `backend/src/main/resources/application.yml`
  - `backend/src/main/resources/application-dev.yml`
- 通用能力：
  - 统一响应 `ApiResponse`
  - 分页响应 `PageResponse`
  - 错误码 `ErrorCode`
  - 业务异常 `BusinessException`
  - 全局异常处理 `GlobalExceptionHandler`
  - 审计注解占位 `AuditLog`
- 模块骨架：
  - 登录与当前用户接口
  - 产品仓库资源与版本
  - 部署方案、方案版本、部署组件
  - 客户、客户环境、环境变量
  - 不可变部署包、manifest、checksum
  - Agent 离线任务、执行日志、失败续跑状态
  - 操作日志、下载日志

### 前端进展

已完成 Vue 前端基础骨架：

- Vite 配置：`frontend/vite.config.ts`
- 路由：`frontend/src/router/index.ts`
- API 层：`frontend/src/api/http.ts`
- 基础布局：`frontend/src/layout/AppLayout.vue`
- 页面占位：
  - 首页工作台
  - 产品仓库
  - 部署配置
  - 客户管理
  - 客户环境
  - 部署包管理
  - Agent 离线部署
  - 日志审计
  - 系统管理

### 数据库进展

已创建 MySQL 8 初始化脚本：

- `backend/src/main/resources/db/migration/V1__init_schema.sql`
- `sql/init.sql`

首批表覆盖：

- 用户和角色
- 产品资源和资源版本
- 部署方案、方案版本、部署组件
- 客户和客户环境
- 环境变量
- 部署包和 manifest
- Agent 任务和执行日志
- 操作日志和下载日志

### 验证情况

前端验证通过：

```bash
cd frontend
npm run build
```

后端暂未验证通过，原因是当前环境 Java 配置异常：

- `JAVA_HOME` 指向不存在的 `C:\Java\jdk21`
- 当前 `java -version` 为 Java 8
- 后端项目需要 Java 17+

修复 JDK 后执行：

```bash
cd backend
mvn test
```

### 明日/后续继续开发建议

1. 修复本地 JDK/JAVA_HOME，完成后端 `mvn test` 验证。
2. 接入数据库访问层，替换当前内存示例数据。
3. 优先实现产品资源 CRUD 和资源版本管理。
4. 实现部署方案版本发布规则：`DRAFT -> PUBLISHED`，发布后只读。
5. 实现客户环境绑定部署方案版本。
6. 实现部署包 manifest.json 生成和 checksum 生成。
7. 后续再推进 Agent 离线执行器和部署报告导入。

### 当前注意事项

- 第一版仍是 MVP 骨架，不包含真实文件上传、MinIO、JWT、Harbor 对接、真实部署包压缩和在线 Agent。
- 前端依赖版本已固定，避免 `latest` 拉取到与当前 Node 版本不兼容的包。
- 后端需要 Java 17+ 才能继续验证和开发。
