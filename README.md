# 企业私有化交付编排与自动部署平台

> 面向私有化交付场景的交付编排中心。统一管理产品资源、部署方案、客户环境与不可变部署包，支持 Agent 离线/在线自动化部署，不替代 Harbor、Nexus、CMDB 等已有系统。

---

## 功能概览

| 模块 | 主要能力 |
|---|---|
| **产品仓库** | 资源元数据管理、制品文件上传（SHA-256 校验）、Harbor 镜像同步、源仓库管理 |
| **部署配置** | 部署方案 + 版本管理、组件编排、配置模板、发布后版本只读 |
| **客户环境** | 客户 / 环境 CRUD、环境变量（敏感值 AES-256-GCM 加密存储）、绑定方案版本 |
| **配置快照** | 客户绑定方案后生成独立副本，与源方案解耦，各客户互不影响 |
| **部署包** | 真实 ZIP 压缩包（含 manifest、checksum、配置、制品）、生命周期管理 |
| **Agent 离线** | bash 执行脚本同包、幂等 + 失败续跑、执行报告导入归档 |
| **Agent 在线** | 拉模型（Agent 主动拉取任务）、心跳保活、实时日志 SSE 推送 |
| **审计日志** | 操作日志（AOP 全覆盖）、登录日志、下载日志 |
| **用户权限** | JWT + RBAC，四角色：超管 / 运维 / 实施工程师 / 审计人员 |
| **统计大屏** | 交付概览、部署成功率、失败步骤 / 原因 Top5 归因 |

---

## 技术栈

**后端**
- Java 21 · Spring Boot 3.3.7 · Maven
- Spring Security + JWT（jjwt 0.12.5）
- Flyway 迁移 · MySQL 8 · Redis · MinIO
- AES-256-GCM 敏感配置加密

**前端**
- Vue 3.5 · TypeScript 5.8 · Vite 6.3

---

## 快速开始（dev 模式，无需任何外部依赖）

```bash
# 克隆项目
git clone <repo-url>
cd actcfg

# 启动后端（纯内存，自动加载种子数据）
cd backend
mvn package -DskipTests
java -jar target/delivery-platform-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev

# 启动前端（新开终端）
cd frontend
npm install
npm run dev
```

浏览器打开 [http://localhost:5173](http://localhost:5173)

**默认账号**（dev 和 local profile 均适用，密码 `Admin@123`）

| 账号 | 角色 | 权限 |
|---|---|---|
| `admin` | SUPER_ADMIN | 全部功能 |
| `ops` | OPS | 资源/方案/客户/包 读写 |
| `impl` | IMPL_ENGINEER | 查看 + 执行部署 |
| `auditor` | AUDITOR | 仅查看与审计 |

> **生产部署前请立即修改所有默认密码。**

---

## 生产部署（local profile）

详见 [doc/08-部署方案.md](doc/08-部署方案.md)，以下为要点：

1. 准备 MySQL 8 / Redis / MinIO，创建 4 个 Bucket（`delivery-resources` / `delivery-packages` / `delivery-agent` / `delivery-reports`）
2. 编写 `backend/src/main/resources/application-local.yml`（参考文档模板，**不入代码库**）
3. 构建并启动后端（首次启动 Flyway 自动建表并插入种子数据）
4. 构建前端 `npm run build`，产物放到 Nginx 静态目录
5. 配置 Nginx 反向代理 `/api → 8080`

```bash
# 构建 + 启动
cd backend && mvn package -DskipTests
java -jar target/delivery-platform-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

---

## 项目结构

```
actcfg/
├── backend/                       Spring Boot 后端
│   └── src/main/
│       ├── java/com/example/delivery/
│       │   ├── agent/             Agent 任务、执行日志、在线实例
│       │   ├── audit/             审计日志（AOP + 登录日志）
│       │   ├── customer/          客户 / 环境 / 变量（加密存储）
│       │   ├── deploy/            部署方案 / 版本 / 组件
│       │   ├── packagebuild/      部署包生成与生命周期
│       │   ├── repository/        产品仓库 + Harbor/源仓库同步
│       │   ├── security/          JWT + BCrypt + AES-256 加密
│       │   ├── snapshot/          客户配置快照
│       │   ├── stats/             统计大屏
│       │   └── user/              用户 / 角色 RBAC
│       └── resources/
│           ├── application.yml
│           └── db/migration/      Flyway V1–V9
├── frontend/                      Vue 3 前端
│   └── src/
│       ├── api/http.ts            统一 API 客户端
│       ├── composables/useAuth.ts JWT 状态管理
│       ├── layout/AppLayout.vue
│       └── views/                 各业务页面
├── doc/                           设计文档（01–10）
└── README.md
```

---

## 构建与测试

```bash
# 后端单元测试（74 个，无需外部依赖）
cd backend && mvn test

# 前端类型检查 + 构建
cd frontend && npm run build
```

---

## 数据库迁移

| 版本 | 内容 |
|---|---|
| V1 | 初始表结构（用户、资源、部署、客户、包、日志） |
| V2 | MVP 种子数据 |
| V3 | Agent 执行报告 + 失败续跑记录 |
| V4 | RBAC 角色 + 初始账号（密码 Admin@123） |
| V5 | 客户配置快照表 |
| V6 | 登录日志表 |
| V7 | 在线 Agent 实例表 |
| V8 | 部署包生命周期字段 |
| V9 | 源仓库表 |

迁移由 Flyway 在启动时自动执行，无需手动操作。

---

## 部署包结构

```
PKGxxxxxxxx.zip
├── manifest.json          部署清单（组件、资源版本、环境信息）
├── README.txt             包说明与目录结构
├── agent/
│   ├── deploy-agent.sh    可执行 bash 部署脚本（幂等 + 失败续跑）
│   └── execution-plan.json 步骤计划 JSON
├── components/
│   └── <组件名>/
│       ├── config.conf    渲染后的配置文件
│       └── healthcheck.txt 健康检查命令
└── artifacts/
    └── <组件名>/          内嵌的制品二进制（如有）
```

---

## 相关文档

| 文档 | 内容 |
|---|---|
| [01-总体产品方案](doc/01-总体产品方案.md) | 产品定位与核心需求 |
| [02-技术方案](doc/02-技术方案.md) | 架构设计与技术选型 |
| [03-数据库设计方案](doc/03-数据库设计方案.md) | 核心表结构 |
| [04-接口方案](doc/04-接口方案.md) | API 设计与错误码 |
| [05-业务流程方案](doc/05-业务流程方案.md) | 核心业务流程 |
| [07-开发计划](doc/07-开发计划.md) | P0–P3 优先级与验收标准 |
| [08-部署方案](doc/08-部署方案.md) | **完整部署步骤（看这个）** |
| [10-需求补齐计划](doc/10-需求补齐计划.md) | 功能补齐历程与验收清单 |
