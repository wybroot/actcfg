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

### 阶段五（审计 AOP 全量 + 登录日志 + Dashboard）已完成

- **Part A 审计 AOP（写操作全覆盖）**：`pom.xml` 加 `spring-boot-starter-aop`；`@AuditLog(module,action)` + `AuditLogAspect`（`@Around`，成功 SUCCESS/异常 FAILED，SecurityContext 取操作人、RequestContextHolder 取 IP，调 `recordOperation`）；全部写接口加注解（CUSTOMER/ENVIRONMENT/ENV_VARIABLE/DEPLOY_PLAN/RESOURCE/PACKAGE/AGENT/SNAPSHOT）。
- **Part B 登录日志**：`V6__sys_login_log.sql` + `LoginLogEntity`；`AuditRepository`/`AuditService` 加登录日志双路径；`AuthController.login` 注入 `AuditService`+`HttpServletRequest`（成功 SUCCESS/密码错 FAILED/禁用 DISABLED）；`GET /api/audit/login-logs`。
- **Part C 统计 Dashboard**：`stats` 包（`StatsOverview`/`StatsService` 复用 list 聚合无新表/`StatsController` `GET /api/stats/overview`）；前端 http.ts 加类型与 API，`DashboardView` 升级统计卡片+任务状态分布，`AuditLogView` 加登录日志第三 Tab。
- 验证：后端 **45/45**（+1 StatsServiceTests），前端 **59** 模块 build。

### 阶段六（在线 Agent，拉模型）已完成 —— doc/10 全功能收官

选型：**拉模型**（agent 主动拉取，平台不反连），适合私有化/防火墙环境，复用现有 `agent_task` 状态机。

- `V7__agent_instance.sql`：在线 agent 实例表（agent_code 唯一、hostname/ip、customer/environment、instance_status ONLINE/OFFLINE、last_heartbeat_at）。
- `AgentInstanceEntity` + `RegisterAgentRequest` + `AgentInstanceRepository`（`@Profile("local")`，含 insert/reRegister/heartbeat/markStaleOffline）。
- `AgentService` 扩展（双路径，注入 `ObjectProvider<AgentInstanceRepository>`）：
  - `registerInstance`（按 agentCode 幂等，存在则重注册为在线）
  - `heartbeat`（保活）
  - `listInstances`（先 `markStaleOffline` 超 90 秒无心跳→OFFLINE）
  - `claimNextTask`（拉模型：认领最早 PENDING 任务→RUNNING，复用 reportStatus，无任务返回 null）
- `AgentController`：`GET /instances`、`POST /instances/register`（@AuditLog）、`POST /instances/{code}/heartbeat`、`POST /instances/{code}/claim`（@AuditLog）。
- 前端：`http.ts` 加 AgentInstance/RegisterAgent 类型与 4 个 API；`OfflineAgentView` 顶部加"在线 Agent 实例"面板（注册表单 + 实例表 + 心跳/认领按钮）。
- 验证：后端 **49/49**（+4 AgentInstanceTests：注册+心跳保活、按 code 幂等、认领 PENDING→RUNNING、无任务返回 null），前端 **59** 模块 build。

**doc/10 五大阶段 + 在线 Agent 全部完成。** 后续可做的增量：真实 agent 客户端二进制、实时日志流（WebSocket/SSE）、自动回滚、更细数据权限、审计查询筛选。所有提交不带共同作者。

### 通し测试（dev profile 实跑）与修复

用 `java21 -jar` 以 dev profile（完全内存、无 MySQL/Redis/MinIO）实跑后端，真实 curl 走通全链路：
- 登录取 JWT（admin/`Admin@123`，seed 四账号 admin/ops/impl/auditor）→ login-logs 正确记录 FAILED（先试错密码）+ SUCCESS。
- stats/overview、packages、execution-plan（阶段四步骤真实生成）、在线 agent 注册/心跳/实例列表、创建 PENDING 任务→claim→RUNNING（拉模型）、操作日志 AOP 记录 REGISTER/CREATE/CLAIM 均 SUCCESS+operatorName。

**发现并修复 2 个真实 bug**：
1. `@PreAuthorize` 抛的 `AccessDeniedException` 被 `GlobalExceptionHandler` 的通用 `Exception` 处理器吞成 HTTP 200 + 500001「系统内部错误」。→ 加专用 `@ExceptionHandler(AccessDeniedException.class)` 返回 **403 + 403001「无操作权限」**；未捕获异常补日志。
2. 被拒绝的写操作**未进审计**（`@PreAuthorize` 拦截器先于 `@Around` 切面执行，方法未到达）。→ 给 `AuditLogAspect` 加 `@Order(100)`（低于 Security 拦截器 400），切面包住权限校验，拒绝时记 **FAILED**。实测 auditor 建客户返回 403 且操作日志出现 `auditor/CUSTOMER/CREATE/FAILED`。

修复后 `mvn test` **49/49** 仍绿，提交 `acd2eb2`（并修正 `.gitignore`，排除运行时 `data/storage/` 产物）。环境提示：系统 PATH 的 `java` 是 1.8，需用 `JAVA_HOME` 的 JDK21 启动 jar（Maven 已用 21）。

### 健壮性修复 + P3 后置能力（承接通し测试）

通し测试后先补一个健壮性问题，再推进 doc/10 里列为"后续增量"的 P3 能力。全部提交不带共同作者。

- **畸形请求体返回 400**（`ff7d651`）：非法 JSON body 之前被吞成 500，改为返回 **400**，语义正确。
- **敏感变量读取脱敏**（`d6128d1`）：`listVariables` 对外读取时清空敏感变量明文、只保留掩码；新增内部 `listVariablesRaw` 供克隆/建包使用。
- **需求5 部署包生命周期 + 清理**（`f42f2cd`，`V8__package_lifecycle.sql`）：状态机 `ACTIVE→ARCHIVED→DEPRECATED→PURGED` + 下载计数 + 保留期；超管清理接口。前端部署包页展示生命周期状态与操作。
- **成功率统计 + 失败归因**（`fd8edfd`）：`GET /api/stats/deploy` 基于已结束任务算成功率，失败步骤/原因 Top5 归因（取自执行报告）；Dashboard 展示。
- **敏感配置加密 + 密钥轮换**（`fd8edfd`）：新包 `security.crypto`——`SecretCipher`（AES-256-GCM，密文自带密钥版本 `enc:keyId:iv:ct`，支持多密钥并存与轮换，历史明文向后兼容）+ `EncryptionProperties`。敏感变量入库前加密、内部读取解密、对外仍脱敏；`POST /api/environments/variables/rotate-secrets`（超管）重加密全部敏感变量。未配密钥时退化为进程内临时密钥（重启失效，仅测试用）。
- **实时日志（SSE）**（`9af8086`）：`AgentLogStreamService` 按任务 ID 维护 SSE 订阅者，`reportStatus` 时广播、终态自动关闭连接；`GET /api/agents/offline/tasks/{taskId}/logs/stream`（text/event-stream）。`JwtAuthFilter` 支持 `token` 查询参数（EventSource 无法设置 Authorization 头）。前端 `OfflineAgentView` 加"实时日志"开关 + 活跃指示灯。

### 镜像坐标接入离线部署链路（`fb7a5f3`）

确认并修复一处真实缺口：Harbor 同步登记的镜像坐标（`imageRepository:imageTag`）之前没接到 agent 执行脚本——镜像组件在现场既无 tar 也无坐标可拉，这条路是断的。

- `resolveComponentSources` 为镜像组件填充 `repo:tag`；`LOAD_IMAGE` 步骤 target 用完整坐标（含 registry/project），不再被 `fileName` 截断。
- agent `load_image` 改为：包内有 tar → 离线 `docker load`；否则按坐标 `docker pull`（拉取失败即失败）。
- 取向明确：**平台只同步元数据、不搬运镜像层**，实际镜像现场从源仓库/代理拉取，不做笨重的 Harbor/Nexus 替代。

### 源仓库前端可管理（`508a296`，`V9__source_repository.sql`）

按需求把"源仓库"做成前端可增删改的实体（先支持 HARBOR 类型），支持多源仓库并在同步镜像时选择。

- 新包 `repository.source`：实体/枚举/JDBC 仓库/服务/控制器，CRUD + **测试连接**（拿解密凭证 ping Harbor `/api/v2.0/health`）。
- 密码复用 `SecretCipher` 加密入库，list/get 脱敏为掩码，同步时解密取凭证；停用的源仓库拒绝取凭证；编辑时密码留空保留原值。
- `HarborSyncService` 改造：请求带 `sourceRepositoryId` 用该源仓库地址/凭证，为空回退全局 `app.harbor`（**向后兼容**，老流程不破）。
- 前端：源仓库管理页 `/repository/sources` + 路由/导航；资源页 Harbor 同步 Tab 加源仓库下拉。

### 验证情况

- 后端 `mvn test` 全绿：从 49 增至 **74**（新增畸形请求体、生命周期、deployStats、SecretCipher×5、镜像坐标、AgentLogStream×3、SourceRepository×7 等）。
- 前端 `npm run build` 通过，**61** 模块。
- 迁移新增 `V8`（部署包生命周期）、`V9`（源仓库表）。`application-local.yml` 补 `app.encryption` 密钥与 `app.harbor` 占位（该文件 gitignore，含真实凭证不入库）。

### 说明 / 下一步

- **doc/10 五大阶段 + 在线 Agent 已收官**，本轮完成的是原列为"后续增量"的 P3 能力（生命周期、成功率归因、敏感加密、SSE 实时日志、源仓库前端管理）。
- 仍可做的增量：真实 agent 客户端二进制、自动回滚、更细数据权限、审计查询筛选、registry 代理形态（平台统一代理拉取再回源）。
- 尚未做真实业务联调（连真实 MySQL/Redis/MinIO/Harbor 走通），下次可起 local profile 从页面配真实源仓库测连接 + 同步一个真实镜像。所有提交不带共同作者。

## 2026-07-22

### 真实环境联调（local profile）——全部验收项通过

首次连接真实 MySQL + Redis + MinIO 完整 E2E 测试。

**发现并修复 1 个 bug（提交 `870ec85`）：**

V4 迁移里的 BCrypt hash 与注释密码 `Admin@123` 不匹配（生成时笔误）。通过提取 `spring-security-crypto` 用 `BCryptPasswordEncoder.matches` 验证发现。修复：替换正确 hash，密码仍为 `Admin@123`，执行 `mvn flyway:repair` 同步 schema history checksum。

**通过的验收项：**

- Flyway V1–V9 在全新库上全部成功执行，seed 数据就绪
- 登录 admin/ops/impl/auditor（密码 `Admin@123`）→ JWT 签发，登录日志落库
- stats/overview + stats/deploy（成功率 + 失败归因）正常返回
- 资源/客户/部署包 CRUD 走 JDBC 路径
- RBAC：auditor 写操作返回 403 + 审计日志记录 FAILED
- **真实建包**：ZIP 写入 MinIO `delivery-packages`，返回真实 SHA-256 + MinIO URL
- **部署包生命周期**：ACTIVE → ARCHIVED → DEPRECATED（禁止下载 409）全程走通，下载计数落库
- **敏感变量加密**：DB 存 `enc:v1:...` 密文，接口脱敏，重启后持久密钥可正常处理
- **源仓库**：新增 + 密码密文入库 + 列表脱敏 + 测试连接接口正常
- **在线 Agent**：注册→心跳→创建 PENDING 任务→claim（PENDING→RUNNING）→上报 SUCCESS，审计日志完整
- 重启后无 Flyway checksum 冲突，登录仍正常

**注意事项（留给后续使用）：**
- Git Bash 里 curl `-d` 包含汉字会乱码，写操作 JSON body 用 `--data-binary @file`
- 运行 jar 须用 JDK21 路径（系统 PATH 的 java 是 1.8）
- MinIO bucket 需提前建：`delivery-resources`/`delivery-packages`/`delivery-agent`/`delivery-reports`

### 文档完善 + Docker 部署支持（2750cce → 59a9f64）

**doc/08-部署方案.md 重写**：从162行扩展为600+行，新增完整操作步骤——MySQL/MinIO 初始化命令、`application-local.yml` 完整配置模板（带逐项说明）、Nginx 配置示例、默认账号表、Docker Compose、Agent 离线/在线操作步骤、备份命令（mysqldump + mc）、安全加固清单、常见问题 Q&A（含今天实际踩过的坑）。

**README 重写**：补功能概览表、技术栈实际版本、快速开始3步命令、项目结构目录树、数据库迁移版本说明（V1–V9）、部署包内部结构说明、文档导航表。

**新增 `docker/` 目录**（4579a37）：
- `docker-compose.yml`：全栈一键部署（后端+前端+MySQL+Redis+MinIO），minio-init 自动创建 Bucket
- `docker-compose.deps.yml`：仅依赖服务，适合本地调试时单独拉起数据库
- `.env.example`：环境变量模板（入库），复制为 `.env` 后填写真实值
- `Dockerfile.backend`：多阶段构建（Maven 3.9 → JRE 21 Alpine，非 root 用户运行）
- `Dockerfile.frontend`：多阶段构建（Node 20 → Nginx 1.26 Alpine）
- `nginx/default.conf`：含 Vue history 路由、`/api` 反代、`proxy_buffering off`（支持 SSE 实时日志）
- `.gitignore` 加入 `docker/.env`

**清理冗余** `sql/` 目录（59a9f64）：原 `sql/init.sql` 只是指向 V1 的四行 SOURCE 占位，实际 SQL 统一由 Flyway 管理，删除消除歧义。
