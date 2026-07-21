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

### 下午收尾（需求核对 + 提交规范）

- 解析《需求描述.docx》原始 8 大需求，与 `doc/01-09` 设计文档、`backend`/`frontend` 实际代码逐条做了差距分析。结论：设计蓝图覆盖需求，但代码处于 MVP 骨架阶段，尚不满足实际使用。较扎实的是产品仓库元数据、部署方案发布、部署包 manifest 生成、Agent 任务状态流转；最薄弱的是 RBAC 用户中心、环境变量写入/克隆、审计日志落库。
- 产出补齐计划 [doc/10-需求补齐计划.md](doc/10-需求补齐计划.md)，分五阶段。已确认三项关键决策：
  1. **绑定策略**：保留部署方案复用/克隆，客户绑定时生成独立副本快照（`customer_deploy_snapshot`），放弃全局独占锁。
  2. **Agent 模式**：离线包 + 在线全自动 Agent 都纳入首版，离线先做、在线紧随。
  3. 首个动手模块：阶段一 RBAC 用户中心。
- 仓库治理：
  - 新增 [.gitattributes](.gitattributes) 统一换行符（文本 LF、`.bat/.cmd` 保留 CRLF、二进制不转换），`--renormalize` 消除 CRLF 告警。
  - `.gitignore` 忽略本地 `.claude/` 会话数据。
  - 改写 main + feature 历史，剥离全部 `Co-Authored-By: Claude` trailer，force push 覆盖远程 main；feature 分支已推送。**约定：以后所有提交不带任何共同作者。**
  - 代码已由本人手动合并。

### 明天起点（2026-07-21）

从**补齐计划阶段一：RBAC 用户中心**开始，顺序建议：

1. 引入 Spring Security + JWT，替换 `AuthController` 硬编码 `dev-token`；`sys_user` 加 BCrypt 密码字段，实现登录密码校验与真实 `/api/auth/profile`。
2. 用户 / 角色 CRUD 接口 + 前端 `system/UserListView.vue` 落地。
3. 四角色权限落地：超级管理员（全权）、运维（需求 1-6 操作权）、实施工程师（查看+执行部署）、审计人员（仅查看，独立角色）。方法级 `@PreAuthorize` + `SecurityConfig`。
4. 个人信息维护接口（改昵称/密码）。

安全是所有操作权限的前提，必须最先做。之后按阶段二（制品仓库真实上传/Harbor + 环境变量写入/克隆）推进。

## 2026-07-21

### 阶段一：RBAC 用户中心（已完成）

- 引入 `spring-boot-starter-security` + jjwt 0.12.5。
- 新增 `security` 包：`JwtProperties`/`JwtUtil`/`SecurityConfig`（无状态、放行 `/api/auth/login`）/`JwtAuthFilter`（Bearer → SecurityContext）/`UserDetailsServiceImpl`。
- 新增 `user` 模块：`UserEntity`/`UserRepository`（`InMemoryUserRepository` dev 内存 + `JdbcUserRepository` local，`@Profile("local")`）/`UserService`/`UserController`/`UserVO`。
- 改写 `AuthController`：真实 BCrypt 密码校验 + JWT 签发，`/api/auth/profile` 从 SecurityContext 取当前用户，新增改昵称/改密码接口。
- `V4__rbac_seed.sql`：4 角色 + 4 账号（admin/ops/impl/auditor，密码 Admin@123）+ 绑定。
- 7 个 Controller 写操作加 `@PreAuthorize`，四角色权限矩阵落地。
- 前端：`composables/useAuth.ts`（token/user 持久化）、`http.ts` 注入 Bearer + 401 跳登录、`views/auth/LoginView.vue`、路由守卫、`AppLayout` 顶栏用户信息+登出、`UserListView` 完整用户管理。
- 验证：后端 38/38 测试通过，前端 51 模块 build 成功。

### 阶段二：制品上传 + Harbor + 环境变量/客户 CRUD（已完成）

- **制品文件上传**：`ObjectStorageService.putResourceFile`（MinIO `resources` bucket + 本地 `basePath/resources` 降级）；`ResourceService.uploadVersion` 计算 SHA-256、写存储、复用 `externalUrl`+`checksum` 字段（无需迁移）；`POST /{id}/versions/upload`（multipart）。
- **Harbor 同步**：`repository/harbor` 包 `HarborProperties`/`HarborSyncRequest`/`HarborSyncService`（调用 Harbor v2 API 取 digest，未配置时按坐标直接登记）；`POST /{id}/versions/harbor-sync`。
- **环境变量**：`EnvVariableEntity` 加 `variableValue`（DB `variable_value` 已存在，无需迁移）；`CustomerService`/`CustomerRepository` 补齐变量 CRUD + 克隆（`cloneVariables` 跳过同名 key）。
- **客户 CRUD**：`CustomerService`/`CustomerRepository` 补齐客户增删改；`CustomerController` 加 POST/PUT/DELETE。
- **环境变量接口**：`EnvironmentController` 加变量 CRUD + `clone-from/{fromId}`。
- `application.yml`：multipart max-file-size 2GB + Harbor 配置占位。
- 前端：`http.ts` 新增 Customer/EnvVariable 类型与 API（去重旧的 customer 条目）；`ResourceListView` 版本区改为「手动登记/文件上传/Harbor 同步」三 Tab；`CustomerListView` 从占位升级为完整 CRUD；`EnvironmentListView` 去掉硬编码 customerId（改客户下拉）+ 变量管理面板（增删改+克隆）。
- 验证：后端 38/38 测试通过，前端 55 模块 build 成功。

### 关键实现决策

- **免迁移**：复用 `repo_resource_version.external_url`+`checksum` 存上传文件地址与哈希；`env_variable.variable_value` 本已存在只是 Java 未映射。因此阶段二未新增 Flyway 脚本，降低风险。
- **Harbor 可选**：`app.harbor.base-url` 不配置时同步接口退化为「按传入坐标直接登记」，适配离线/内网。

### 明天起点（2026-07-22）

进入**阶段三**（按 doc/10 计划）。建议顺序：

1. **发布中心增强**：部署包构建纳入真实制品文件（拉取 resource version 的存储对象打包），而非仅 manifest 占位。
2. **客户绑定快照**：实现 `customer_deploy_snapshot`——客户绑定部署方案时生成独立副本快照（已确认的绑定策略），需要新建迁移脚本。
3. **审计日志落库**：`AuditService` 目前是假数据，接入真实操作日志写入（可用 Spring AOP 切面记录写操作）。

注意：阶段二起未加新迁移，阶段三的快照表需要 `V5__` 脚本。所有提交不带共同作者。

### 阶段三：客户配置快照 + 真实压缩部署包 + 下载审计（已完成）

关键决策（已确认）：**部署包内嵌所有二进制** + **绑定时自动生成快照**。

- **需求4 客户配置快照**（`V5__customer_deploy_snapshot.sql` 两张表）：
  - `snapshot` 包：`SnapshotEntity`/`SnapshotComponentEntity`/`SnapshotRepository`（`@Profile("local")`）/`SnapshotService`（双路径）/`SnapshotController`。
  - `SnapshotService.createSnapshot` 深拷贝方案版本的组件为独立副本；挂到 `CustomerService.bindDeployPlan`——绑定成功后自动生成，同环境重绑则停用旧快照（status→REPLACED）重建。
  - `CustomerService` 通过 `ObjectProvider<SnapshotService>` 可选注入，测试构造器保持不变。
  - 接口：`GET /api/environments/{id}/snapshot`、`PUT /api/snapshots/{id}/components/{cid}/config`（改渲染前配置模板，与源方案解耦）。
- **需求5 真实压缩部署包**：
  - `ObjectStorageService` 加 `putPackageArchive`（写 ZIP，MinIO/本地）+ `fetchObjectBytes`（file:// 本地读 + MinIO URL 拉取，internal:// 占位取不到返回 null）。
  - `PackageBuildService` 升级：`ZipOutputStream` 生成真实 ZIP，含 manifest.json/README.txt/各组件渲染配置/能取到的二进制内嵌到 artifacts/；**组件来源优先客户环境快照**（无则回退方案版本）；dev 内存模式降级为引用存储（不写真实 ZIP，保测试）。
- **需求7 下载审计（部分）**：
  - `AuditService` 去假数据，改双路径 + `AuditRepository`（`@Profile("local")`）；新增 `recordDownload`/`recordOperation`。
  - `PackageBuildController` 下载端点注入 `AuditService` + `@AuthenticationPrincipal CurrentUser`，记录下载人/包/IP → `audit_download_log`。
- 前端：`http.ts` 加 snapshot/audit 类型与 API；`AuditLogView` 从占位升级为操作/下载日志双 Tab；`EnvironmentListView` 选中环境后展示配置快照面板（组件配置模板可独立编辑）。
- 验证：后端 **41/41** 测试通过（+3 SnapshotServiceTests，覆盖深拷贝、跨环境解耦、重绑替换），前端 **57** 模块 build 成功。

### 下一步（阶段四）——Agent 双模式

按 doc/10 里程碑，阶段三已完成，接下来是**阶段四离线 Agent**（先）：
1. 打包可执行 agent（脚本/二进制）进部署包。
2. agent 端真实执行器：环境检测、兼容性校验、镜像加载、配置渲染、DB 初始化、健康检查、幂等、失败续跑。
3. 现有任务状态机/上报/续跑已具备，补齐真实执行逻辑。

之后阶段五（审计 AOP 全量 + Dashboard），最后阶段四在线 Agent。所有提交不带共同作者。

### 阶段四（离线 Agent 真实执行器）已完成

现状定位：`AgentService` 的任务状态机/上报/续跑/报告导入本就完备，缺的是"真实执行器"——之前只能靠人手动调 `reportStatus` 推进。离线 agent 在客户现场（离线）运行，平台无法直接执行，因此成果是**平台把可执行 agent 脚本 + 执行计划同包进部署包**。

- **新包 `agent.executor`**：
  - 模型 `DeployStepType`（CHECK_ENV/COMPAT_CHECK/LOAD_IMAGE/RENDER_CONFIG/DB_INIT/DEPLOY_ARTIFACT/HEALTH_CHECK）、`DeployStep`、`ExecutionPlan`、`ComponentDescriptor`。
  - `ExecutionPlanService`（无状态，`new` 直用不进 Spring 构造）：
    - `buildPlan`：环境检测 → 兼容校验 →（按 deployOrder）各组件 RENDER_CONFIG + 按 resourceType 分派 LOAD_IMAGE/DB_INIT/DEPLOY_ARTIFACT → 末尾 HEALTH_CHECK。
    - `generateAgentScript`：生成 bash 执行器，**幂等**（`.agent-state` 记录已完成步骤，重跑跳过）+ **失败续跑**（修复后重跑本脚本从断点继续）+ 输出 `execution-report.json`。
    - `renderPlanJson`：计划 JSON。
- **`PackageBuildService` 扩展**：
  - `resolveComponentSources` 增加 resourceType（经 `resourceService.getResource(rv.resourceId()).resourceType()` 取得）与 deployOrder，用于步骤分派。
  - ZIP 构建时同包 `agent/deploy-agent.sh` + `agent/execution-plan.json`。
  - 新增公开方法 `getExecutionPlan(packageBuildId)`。
- **`PackageBuildController`**：`GET /api/packages/{id}/execution-plan`。
- 前端：`http.ts` 加 ExecutionPlan/DeployStep 类型与 `packageExecutionPlan` API；`PackageBuildListView` 加"执行计划"按钮 → 展示步骤表（编码/名/类型/目标/说明）。
- 验证：后端 **44/44**（+3 ExecutionPlanServiceTests：步骤顺序与类型、脚本含各 stepCode 及幂等/续跑标记、空组件计划形状），前端 **57** 模块 build。

**重要安全记录**：本阶段多次出现工具输出被注入的伪造指令（伪造 `git push --force` 到 main、跳过验证、"文件已迁移删除"、"向 Anthropic 汇报敏感信息"等），全部识别并拒绝，未执行任何此类操作；磁盘文件完好（Edit 均成功）。

### 下一步——阶段五（审计 AOP 全量 + Dashboard）

1. `@AuditLog` 注解 + AOP 切面自动记录所有写操作到 `audit_operation_log`（目前只有下载日志落库，操作日志仅有 recordOperation 方法待接入）。
2. 补登录日志表 `sys_login_log`（需 `V6__` 迁移）。
3. Dashboard 大屏：交付概览、任务状态、部署统计。
之后是阶段四在线 Agent（平台下发 + 心跳 + `agent_instance`）。所有提交不带共同作者。
