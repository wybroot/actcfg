# 企业私有化交付编排与自动部署平台

本项目是面向私有化交付场景的交付编排与自动部署平台。平台定位为“交付编排中心”，不替代 Harbor、Nexus、CMDB、监控平台等已有系统，而是统一组织产品资源、部署方案、客户环境、不可变部署包和 Agent 离线执行。

## 技术栈

- 后端：Java 17 + Spring Boot 3 + Maven
- 前端：Vue 3 + TypeScript + Vite
- 数据库：MySQL 8
- 第一阶段重点：MVP 骨架，不实现完整业务闭环

## 目录结构

```text
backend/                 Spring Boot 后端工程
frontend/                Vue 3 前端工程
sql/                     数据库初始化脚本
scripts/                 本地开发脚本
doc/                     产品、技术、数据库、接口、流程等方案文档
需求描述.docx            原始需求描述
```

## MVP 范围

当前骨架已预留以下模块：

- 产品仓库：交付资源索引、资源版本、外部仓库引用
- 部署配置：部署方案、方案版本、组件编排、发布后只读
- 客户环境：客户、环境、环境变量、敏感配置脱敏占位
- 部署包：不可变部署包、manifest、checksum、生成状态
- Agent 离线部署：任务状态、执行日志、幂等和失败续跑状态模型
- 日志审计：操作日志、下载日志
- 用户权限：登录和当前用户信息的开发占位接口

## 本地启动

### 后端

```bash
cd backend
mvn spring-boot:run
```

后端默认端口：`8080`。

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端默认端口：`5173`，并代理 `/api` 到 `http://localhost:8080`。

## 构建验证

```bash
cd backend && mvn test
cd frontend && npm install && npm run build
```

也可以使用：

```bash
bash scripts/dev-check.sh
```

## 数据库

初始化脚本：

```text
backend/src/main/resources/db/migration/V1__init_schema.sql
sql/init.sql
```

首批表覆盖用户角色、产品资源、部署方案、客户环境、部署包、Agent 执行日志和审计日志。

## 后续开发顺序

1. 接入数据库访问层，替换当前内存示例数据。
2. 实现产品资源 CRUD 和资源版本管理。
3. 实现部署方案版本发布规则，确保已发布版本不可编辑。
4. 实现客户环境变量和敏感配置加密/脱敏。
5. 实现部署包 manifest.json 和 checksum.sha256 生成。
6. 实现 Agent 离线执行器和部署报告导入。
7. 后续再扩展在线 Agent、实时日志、自动回滚和复杂数据权限。
