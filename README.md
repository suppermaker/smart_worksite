# 智慧工地大模型应用系统

智慧工地大模型应用系统面向建筑工地管理场景，提供项目资料管理、知识库管理、知识问答、合规审查、报告生成、OCR识别、任务编排、权限安全和审计追踪等能力。

本仓库当前包含 Java Spring Boot 后端主系统、Vue 3 + TypeScript 前端工程、Docker Compose 本地依赖环境，以及需求文档、架构设计文档、接口文档。

智能体、大模型、RAG、OCR算法等 AI 能力由 Python 智能算法服务实现，Java 后端通过 REST API 调用，不在 Java 主系统中实现智能体核心逻辑。

## 技术栈

### 后端主系统

| 技术 | 版本/说明 | 用途 |
| --- | --- | --- |
| Java | 17 | 后端开发语言 |
| Spring Boot | 3.3.7 | 后端主框架 |
| Spring Web | Spring Boot Starter Web | REST API |
| Spring Validation | Spring Boot Starter Validation | 参数校验 |
| Spring Data Redis | Spring Boot Starter Data Redis | Redis访问 |
| Spring Boot Actuator | Spring Boot Starter Actuator | 健康检查 |
| MyBatis | 3.0.4 starter | 数据访问 |
| PageHelper | 2.1.0 | 分页查询 |
| MySQL Connector/J | runtime | MySQL连接 |
| Flyway | flyway-core + flyway-mysql | 数据库迁移 |
| MinIO Java SDK | 8.5.12 | 对象存储访问 |
| Apache PDFBox | 2.0.31 | PDF处理 |
| Apache POI | 5.2.5 | Word、Excel、PPT处理 |
| Maven | 项目构建 | 依赖管理和打包 |

### 前端应用

| 技术 | 用途 |
| --- | --- |
| Vue 3 | 前端框架 |
| TypeScript | 类型约束 |
| Vite | 构建工具和开发服务 |
| Pinia | 状态管理 |
| Vue Router | 路由和权限守卫 |
| Axios | HTTP请求 |
| Element Plus | UI组件库 |
| @element-plus/icons-vue | 图标库 |

### Python智能算法服务

Python智能算法服务不在当前 Java 主工程中实现，但属于系统整体架构的一部分。

| 技术方向 | 用途 |
| --- | --- |
| Python | 智能算法服务开发语言 |
| 大模型调用 | 智能问答、合规审查、报告内容生成 |
| Agent智能体 | 任务拆解、工具调用、多步骤推理和业务编排 |
| RAG检索增强 | 项目知识库、政策标准库、行业资料库检索增强 |
| Embedding向量化 | 文档切片向量化和语义检索 |
| OCR识别 | 身份证、车牌、发票、合同和自定义字段识别 |
| 文档解析 | 复杂文档内容抽取、版面理解、表格识别 |
| CryptoAgentV3 | 当前报告生成集成的外部Python报告生成服务 |

### 数据与基础设施

| 技术 | 版本/说明 | 用途 |
| --- | --- | --- |
| MySQL | 8.4 | 业务元数据、权限、任务、审计、文件元数据 |
| Redis | 7.2-alpine | 缓存、轻量队列、分布式锁 |
| MinIO | RELEASE.2025-04-22T22-12-26Z | 文档、图片、模板、报告文件存储 |
| Docker Compose | 本地环境编排 | 启动MySQL、Redis、MinIO |
| Milvus或pgvector | 规划中 | 知识库向量检索 |

## 系统边界

```text
Vue 3 + TypeScript 前端
        ↓ REST API
Java + Spring Boot 后端主系统
        ↓ REST API
Python 智能算法服务
        ↓
大模型 / Agent / RAG / OCR / 文档解析 / CryptoAgentV3 / 向量化
```

前端不直接调用 Python 服务、数据库、对象存储或向量库。Java 后端负责统一鉴权、项目隔离、业务编排、状态记录、文件保存和审计追踪。

## 当前实现状态

### 已实现

后端：

- Spring Boot 工程骨架
- 统一响应结构 `ApiResponse`
- 分页响应结构 `PageResult`
- 统一错误码 `ErrorCode`
- 业务异常 `BusinessException`
- 全局异常处理 `GlobalExceptionHandler`
- 请求链路 `X-Request-Id`
- MyBatis 配置和 Mapper 扫描
- Redis 缓存、锁、队列基础封装
- MinIO 文件存储适配
- Flyway 数据库迁移
- 系统探活接口
- 项目列表和项目详情查询
- 文件上传、列表、详情、下载 URL、预览 URL、删除
- 文件解析任务创建、记录查询、内容查询、重试
- Qwen-VL 文档解析适配配置
- 模板上传、列表、详情、修改、启用、停用、删除
- 报告模板兼容接口 `/api/report/templates`
- 审查模板兼容接口 `/api/review/templates`
- 报告创建、列表、详情、重新生成、下载 URL
- CryptoAgentV3 报告生成集成
- 报告版本记录和延迟下载逻辑

前端：

- Vue 3 + TypeScript + Vite 工程
- Pinia 状态管理
- Vue Router 路由
- Axios 请求封装
- mock 模式
- 登录页
- 首页工作台
- 知识库页面
- 知识问答页面
- 合规审查页面
- 报告列表和详情页面
- OCR 页面
- 403 和 404 页面
- 通用上传、表格、搜索、弹窗、状态、进度、JSON 查看、下载组件

### 规划中

- 用户登录和权限完整实现
- 项目成员管理
- 知识库完整入库和向量检索
- 数据源管理和数据库问答
- 知识问答后端业务实现
- 合规审查后端业务实现
- OCR识别后端业务实现
- 独立 Python 智能算法服务工程
- Agent智能体服务
- RAG向量检索服务
- 生产部署脚本和监控告警

## 目录结构

```text
smart_worksite/
  deploy/                 本地依赖环境，MySQL、Redis、MinIO
  docs/                   需求文档、架构设计文档、接口文档
  frontend/               Vue 3 + TypeScript前端工程
  src/main/java/          Java后端源码
  src/main/resources/     后端配置、Mapper XML、Flyway脚本
  pom.xml                 Maven配置
  README.md               项目总览
```

后端主要包结构：

```text
com.xd.smartworksite
  common                  通用响应、异常、配置、Redis封装
  system                  系统探活
  auth                    用户权限，规划中
  project                 项目管理
  file                    文件管理和文件解析
  template                模板管理
  report                  报告生成和CryptoAgentV3集成
  knowledge               知识库，规划中
  datasource              数据源，规划中
  qa                      知识问答，规划中
  review                  合规审查，规划中
  ocr                     OCR识别，规划中
  task                    任务编排，规划中
  audit                   审计日志，规划中
```

## 本地启动

### 1. 启动本地依赖

```powershell
cd deploy
copy .env.example .env
docker compose -f docker-compose-env.yml --env-file .env up -d
```

### 2. 启动后端

```powershell
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

### 3. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

## 当前接口

### 系统

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/system/ping` | 系统探活 |
| GET | `/actuator/health` | 健康检查 |

### 项目

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/projects` | 分页查询项目列表 |
| GET | `/api/projects/{projectId}` | 查询项目详情 |

### 文件

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/files/upload` | 上传文件 |
| GET | `/api/files` | 分页查询文件列表 |
| GET | `/api/files/{fileId}` | 查询文件详情 |
| GET | `/api/files/{fileId}/download-url` | 获取下载URL |
| GET | `/api/files/{fileId}/preview-url` | 获取预览URL |
| DELETE | `/api/files/{fileId}` | 删除文件 |

### 文件解析

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/files/{fileId}/parse` | 创建文件解析任务 |
| GET | `/api/files/{fileId}/parse-records` | 查询文件解析记录 |
| GET | `/api/files/{fileId}/parse-records/latest` | 查询最新解析记录 |
| GET | `/api/file-parse-records/{recordId}` | 查询解析记录详情 |
| GET | `/api/file-parse-records/{recordId}/content` | 查询解析结果内容 |
| POST | `/api/file-parse-records/{recordId}/retry` | 重试解析 |

### 模板

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/templates` | 上传通用模板 |
| GET | `/api/templates` | 分页查询模板 |
| GET | `/api/templates/{templateId}` | 查询模板详情 |
| PUT | `/api/templates/{templateId}` | 修改模板元数据 |
| POST | `/api/templates/{templateId}/enable` | 启用模板 |
| POST | `/api/templates/{templateId}/disable` | 停用模板 |
| DELETE | `/api/templates/{templateId}` | 删除模板 |
| POST | `/api/report/templates` | 上传报告模板 |
| GET | `/api/report/templates` | 查询报告模板列表 |
| POST | `/api/review/templates` | 上传审查模板 |
| GET | `/api/review/templates` | 查询审查模板列表 |

### 报告

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/reports` | 创建报告生成任务并调用 CryptoAgentV3 |
| GET | `/api/reports` | 分页查询报告列表 |
| GET | `/api/reports/{reportId}` | 查询报告详情 |
| POST | `/api/reports/{reportId}/regenerate` | 重新生成报告 |
| GET | `/api/reports/{reportId}/download?format=WORD` | 获取 Word 报告下载 URL |

## 数据库迁移

Flyway脚本位于：

```text
src/main/resources/db/migration
```

当前脚本：

| 脚本 | 说明 |
| --- | --- |
| `V1__init_schema.sql` | 初始化用户、角色、项目、文件、知识库、数据源、任务、审计、系统配置等基础表 |
| `V2__extend_file_object.sql` | 扩展文件对象字段 |
| `V2__template_report_schema.sql` | 新增模板、报告配置、报告主表和报告版本表 |
| `V3__create_file_parse_record.sql` | 新增文件解析记录表 |

数据库结构变更必须新增Flyway脚本，不要修改已经合入并被团队使用的旧脚本。

## 外部服务配置

### Qwen-VL文档解析

```env
QWEN_VL_ENDPOINT=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
QWEN_VL_API_KEY=
QWEN_VL_MODEL=qwen-vl-plus
```

### CryptoAgentV3报告生成

```env
CRYPTO_AGENT_V3_BASE_URL=http://127.0.0.1:8012
CRYPTO_AGENT_V3_INVOKE_PATH=/v1/report-generation/invoke
CRYPTO_AGENT_V3_CONNECT_TIMEOUT_SECONDS=5
CRYPTO_AGENT_V3_READ_TIMEOUT_SECONDS=3000000
```

当前报告生成模块通过 Java 后端调用 CryptoAgentV3。智能体核心能力仍由外部 Python 服务实现，Java 只负责业务编排、状态记录、文件保存和下载 URL 返回。

## 文档

| 文档 | 说明 |
| --- | --- |
| `docs/智慧工地大模型应用系统-需求文档.md` | 系统需求文档，包含499条详细需求 |
| `docs/智慧工地大模型应用系统-架构设计文档.md` | 架构设计文档 |
| `docs/智慧工地大模型应用系统-接口文档.md` | 接口设计文档 |
| `docs/任务分工.xlsx` | 任务分工表 |
| `智慧工地前端UI风格指南.md` | 前端UI风格指南 |

注意：`docs` 描述的是完整目标系统，当前代码是阶段性实现。以当前实现状态和实际 Controller 为准判断已实现接口。

## AI能力说明

本项目智能体能力使用 Python 智能算法服务实现，不使用 Java 实现智能体核心逻辑。前端不直接调用 Python 服务，所有 AI 能力由 Java 后端统一编排和权限控制后调用。
