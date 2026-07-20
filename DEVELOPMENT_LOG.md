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

## 2026-07-19

### 今日完成

- 按上一轮开发日志建议，继续推进“产品资源 CRUD 和资源版本管理”。
- 后端产品仓库模块从固定示例数据扩展为内存存储版 CRUD：
  - 资源列表、详情、新增、修改、软删除；
  - 资源版本列表、新增；
  - 资源编码唯一、同资源版本号唯一、禁用资源不可新增版本；
  - 镜像类资源或 Harbor 来源版本要求填写镜像仓库和镜像标签；
  - 非镜像资源版本至少填写外部地址或 checksum。
- 新增产品资源请求 DTO 和参数校验：
  - `CreateResourceRequest`
  - `UpdateResourceRequest`
  - `CreateResourceVersionRequest`
- 改进后端业务异常和参数校验提示：
  - `BusinessException` 支持自定义错误消息；
  - 参数校验失败时返回首个字段错误消息。
- 前端产品仓库页面从占位页升级为可操作页面：
  - 资源列表表格；
  - 新增/编辑资源表单；
  - 资源版本面板；
  - 新增版本表单；
  - loading、error、empty 状态。
- 前端 API 工具新增 `post`、`put`、`del`，并统一解析后端 `ApiResponse` 错误消息。
- 新增后端 `ResourceServiceTests`，覆盖资源创建、重复编码、更新、删除、版本创建、重复版本和镜像版本校验等规则。
- 按开发计划继续实现部署方案版本发布与只读规则：
  - 后端部署方案模块改为内存状态管理；
  - 新增部署方案、方案版本、组件创建接口；
  - 新增版本发布接口，草稿可发布、发布后只读；
  - 前端部署配置页面升级为方案/版本/组件可操作页面；
  - 新增后端 `DeployPlanServiceTests` 覆盖发布与只读规则。
- 继续实现客户环境绑定已发布部署方案版本：
  - 后端客户环境模块改为内存状态管理；
  - 新增 `PUT /api/environments/{id}/bind-plan` 绑定接口；
  - 绑定时校验目标部署方案版本必须为 `PUBLISHED`；
  - 前端客户环境页面升级为环境列表与绑定操作页面；
  - 新增后端 `CustomerServiceTests` 覆盖绑定成功、草稿版本拒绝、不存在数据拒绝等规则。
- 继续实现部署组件绑定真实资源版本：
  - 后端创建部署组件时校验资源版本必须存在且为 `ENABLED`；
  - 前端部署配置页将资源版本 ID 手工输入改为资源/版本联动选择；
  - 新增后端测试覆盖绑定成功、不存在资源版本拒绝、禁用资源版本拒绝等规则。

### 验证情况

前端验证通过：

```bash
cd frontend
npm run build
```

后端验证通过：

```bash
cd backend
mvn test
```

结果：`BUILD SUCCESS`，测试总数 16，失败 0，错误 0。

### 后续继续开发建议

1. 实现部署包 manifest.json 生成和 checksum 生成。
2. 后续再接入数据库访问层，将当前内存 CRUD 替换为真实持久化。

## 2026-07-20

### 今日完成

- 继续实现部署包 manifest.json 生成和 checksum 生成：
  - 后端部署包模块从固定示例数据扩展为内存状态管理；
  - 新增 `POST /api/packages/build` 创建部署包接口；
  - 新增 `GET /api/packages/{id}` 查询部署包详情接口；
  - 新增 `GET /api/packages/{id}/status` 查询生成状态接口；
  - 新增 `GET /api/packages/{id}/download` 查询下载信息接口；
  - 新增 `DELETE /api/packages/{id}` 删除部署包记录接口；
  - 创建部署包时校验客户、环境、已绑定且已发布的部署方案版本；
  - 根据部署组件、资源版本、客户环境生成 manifest JSON；
  - 基于 manifest JSON 生成 SHA-256 checksum，并写入部署包记录；
  - 新增后端 `PackageBuildServiceTests` 覆盖生成成功、状态查询、下载信息、删除、草稿版本拒绝、未绑定版本拒绝。
- 前端部署包管理页面从占位页升级为可操作页面：
  - 部署包列表表格；
  - 生成部署包表单；
  - manifest JSON 查看面板；
  - checksum 展示；
  - 状态查询、下载信息查看、删除操作；
  - loading、error、empty 状态。
- 继续推进 Agent 离线执行器和部署报告导入：
  - 后端 Agent 模块从固定示例数据扩展为内存状态管理；
  - 新增创建离线任务、任务详情、取消任务、状态上报接口；
  - 状态上报时追加执行日志，完成态任务禁止继续上报；
  - 取消任务时写入取消日志，已完成任务禁止取消；
  - 创建离线任务前校验部署包存在且可下载；
  - 新增后端 `AgentServiceTests` 覆盖任务创建、状态上报、取消、完成态拒绝继续上报。
- 前端 Agent 离线部署页面从占位页升级为可操作页面：
  - 离线任务列表表格；
  - 创建任务表单；
  - 状态上报表单；
  - 执行日志查看面板；
  - 取消任务操作；
  - loading、error、empty 状态。
- 接入本地 MySQL 持久化基座并迁移核心模块：
  - 后端新增 JDBC/Flyway/MySQL 依赖；
  - `application-local.yml` 使用本地 MySQL 连接并加入 `.gitignore`，避免提交数据库密码；
  - Flyway `V1` 修复 MySQL 8 保留字 `sensitive` 为 `is_sensitive`；
  - 新增 `V2__seed_mvp_data.sql` 写入 MVP 示例数据；
  - 新增 JDBC RowMapper 辅助工具；
  - 产品资源、部署方案、客户环境、部署包、Agent 离线任务模块增加 JDBC Repository；
  - 服务层在 local profile 有 JdbcTemplate 时走 MySQL，默认 dev/test 仍保留内存实现，避免单元测试依赖本地数据库。
- 接入 MinIO 对象存储用于部署包产物：
  - 后端新增 MinIO SDK 依赖；
  - 新增 `StorageProperties`、`StoredObject`、`ObjectStorageService`；
  - 本地 `application-local.yml` 配置 MinIO endpoint、access key、secret key 和业务 bucket；
  - 已创建 `delivery-resources`、`delivery-packages`、`delivery-agent`、`delivery-reports` bucket；
  - 创建部署包时将 `manifest.json`、`checksum.sha256` 和 MVP package 占位文件写入 `delivery-packages`；
  - 部署包下载信息返回 MinIO 对象 URL。
- 实现部署报告导入和失败续跑：
  - 新增数据库迁移 `V3__agent_report_and_retry.sql`，新增 `agent_execution_report`、`agent_retry_record` 两张表；
  - 后端新增 `AgentExecutionReportEntity`、`AgentRetryRecordEntity`、`AgentRetryRecordView`、`ImportAgentReportRequest`；
  - `AgentRepository` 增加执行报告和续跑记录的 JDBC 读写方法，含按任务聚合续跑次数和最终状态的视图查询；
  - `AgentService` 新增 `importReport`、`getReport`、`listReports`、`retryTask`、`listRetryRecords`，内存态与 JDBC 态双路径均已实现；
  - 执行报告导入仅允许已完成任务（SUCCESS/FAILED/CANCELED），且每个任务只能导入一次；
  - 失败续跑仅允许 FAILED 任务触发，续跑时复用最近一条执行日志作为失败步骤和失败原因，任务状态置为 RETRYING；
  - 任务再次进入完成态时自动关闭对应的未结束续跑记录，写入最终结果状态；
  - 新增 `POST /api/agents/offline/reports/import`、`GET /api/agents/offline/reports`、`GET /api/agents/offline/tasks/{taskId}/report`、`POST /api/agents/offline/tasks/{taskId}/retry`、`GET /api/agents/offline/retry-records` 接口；
  - 扩展 `AgentServiceTests`，覆盖报告导入成功、重复导入拒绝、未完成任务拒绝导入、续跑成功、非失败任务拒绝续跑、续跑后完成任务关闭续跑记录等场景。
- 前端 Agent 离线部署页面继续扩展：
  - 已完成任务新增“导入报告”入口，FAILED 任务新增“续跑”按钮；
  - 新增“执行报告”和“失败续跑记录”两张只读汇总表格，均带独立 loading、error、empty 状态。

### 验证情况

后端验证通过：

```bash
mvn -f backend/pom.xml test
```

结果：`BUILD SUCCESS`，测试总数 38，失败 0，错误 0。

前端验证通过：

```bash
npm --prefix frontend run build
```

本地 MySQL profile 冒烟通过：

```bash
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
```

已验证 Flyway 成功执行 `V1`、`V2`，并确认核心表有 seed 数据：`repo_resource`、`deploy_plan`、`customer`、`package_build`、`agent_task` 均为 1 条。

核心 API 冒烟通过：

- `GET /api/repository/resources`
- `GET /api/deploy/plans`
- `GET /api/customers`
- `GET /api/packages`
- `GET /api/agents/offline/tasks`

MinIO 冒烟通过：

- `GET /minio/health/live` 返回 200；
- 使用 `mc` 成功列出 bucket；
- 创建部署包后确认 `delivery-packages/{packageCode}/manifest.json`、`checksum.sha256`、`package.txt` 已写入；
- `GET /api/packages/{id}/download` 返回 MinIO 对象 URL。

### 后续继续开发建议

1. 继续补充基于 local profile 的 Repository/MinIO 集成测试，覆盖执行报告和续跑记录的 JDBC 路径。
2. 继续实现 Agent 安装包管理。
3. 后续接入 Dashboard 大屏，展示客户、环境、部署包、Agent 任务和失败告警指标。
