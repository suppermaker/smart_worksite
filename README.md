# 智慧工地大模型应用系统

智慧工地大模型应用系统面向建筑工地管理场景，提供项目资料管理、知识库管理、知识问答、合规审查、报告生成、OCR 识别、任务编排、权限安全和审计追踪等能力。

本仓库当前包含 Java Spring Boot 后端主系统、Vue 3 + TypeScript 前端工程、Docker Compose 本地依赖环境，以及需求文档、架构设计文档、接口文档。

智能体、大模型、RAG、OCR 算法等 AI 能力由 Python 智能算法服务实现，Java 后端通过 REST API 调用，不在 Java 主系统中实现智能体核心逻辑。前端只调用 Java 后端 REST API，不直接访问 Python 服务、数据库、MinIO、向量库或 OCR 引擎。

## 技术栈

### 后端主系统

| 技术 | 版本/说明 | 用途 |
| --- | --- | --- |
| Java | 17 | 后端开发语言 |
| Spring Boot | 3.3.7 | 后端主框架 |
| Spring Web | Spring Boot Starter Web | REST API |
| Spring Security | JWT + 方法权限 | 登录认证、接口鉴权 |
| Spring Validation | Spring Boot Starter Validation | 参数校验 |
| Spring Data Redis | Spring Boot Starter Data Redis | Redis 访问、缓存、JWT 黑名单、登录失败锁定 |
| Spring Boot Actuator | health、info | 健康检查 |
| MyBatis | 3.0.4 starter + XML | 数据访问 |
| PageHelper | 2.1.0 | 分页查询 |
| MySQL Connector/J | runtime | MySQL 访问 |
| Flyway | flyway-core + flyway-mysql | 数据库迁移 |
| MinIO Java SDK | 8.5.12 | 对象存储访问 |
| Apache PDFBox | 2.0.31 | PDF 处理 |
| Apache POI | 5.2.5 | Word、Excel、PPT 处理 |
| Maven | 项目构建 | 依赖管理和打包 |

### 前端应用

| 技术 | 用途 |
| --- | --- |
| Vue 3 | 前端框架 |
| TypeScript | 类型约束 |
| Vite | 构建工具和开发服务 |
| Pinia | 状态管理 |
| Vue Router | 路由和权限守卫 |
| Axios | HTTP 请求 |
| Element Plus | UI 组件库 |
| @element-plus/icons-vue | 图标库 |

### Python 智能算法服务

Python 智能算法服务属于整体架构的一部分，位于 `python-ai-service/`。

| 技术方向 | 用途 |
| --- | --- |
| 大模型调用 | 智能问答、合规审查、报告内容生成 |
| Agent 智能体 | 任务拆解、工具调用、多步骤推理和业务编排 |
| RAG 检索增强 | 项目知识库、政策标准库、行业资料库检索增强 |
| Embedding 向量化 | 文档切片向量化和语义检索 |
| OCR 识别 | 身份证、车牌、发票、合同和自定义字段识别 |
| 文档解析 | 内容抽取、版面理解、表格识别 |
| CryptoAgentV3 | 当前报告生成集成的外部 Python 报告生成服务 |

### 数据与基础设施

| 技术 | 版本/说明 | 用途 |
| --- | --- | --- |
| MySQL | 8.4 | 业务元数据、权限、任务、审计、文件元数据 |
| Redis | 7.2-alpine | 缓存、轻量队列、分布式锁、JWT 黑名单、登录失败锁定 |
| MinIO | RELEASE.2025-04-22T22-12-26Z | 文档、图片、模板、报告文件存储 |
| Docker Compose | 本地环境编排 | 启动 MySQL、Redis、MinIO |
| pgvector / Milvus | 可选规划 | 知识库向量检索，由 Python 服务访问 |

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

- Java 后端负责统一鉴权、项目隔离、业务编排、状态记录、文件保存、下载 URL、审计追踪和外部调用日志。
- Python 服务负责大模型、Agent、RAG、Embedding、OCR 和文档解析等智能算法能力。
- Qwen API Key 只允许配置在 `python-ai-service/`，Java 配置、SQL、文档和日志中不得写入 Qwen 密钥。

## 当前实现状态

### 已实现

后端：

- Spring Boot 工程骨架、统一响应、统一异常、请求 ID。
- Spring Security + JWT 登录、退出、当前用户信息、当前用户改密。
- 用户、角色、权限、项目成员管理基础接口。
- 项目列表、详情、创建、修改、启停、逻辑删除和项目级访问隔离。
- 文件上传、列表、详情、访问 URL、删除和项目级访问校验。
- 文件解析任务创建、记录查询、内容查询、重试和项目级访问校验。
- 模板上传、列表、详情、修改、启用、停用、删除和项目级访问校验。
- 报告模板和审查模板兼容接口。
- 报告创建、列表、详情、重新生成、下载 URL、版本记录、项目级访问校验和异步 CryptoAgentV3 生成链路。
- Java AI 适配层：模型调用、Agent 调用、RAG 检索/索引、数据库问答、路由、上下文准备、外部调用日志和项目级访问校验。
- 知识库基础管理、文档上传、索引任务创建、任务 outbox 投递、Worker 异步调用 Python RAG 索引和失败状态记录。
- 任务管理接口：任务列表、详情、阶段日志、状态统计、失败任务重试、等待/运行中任务取消请求和项目级访问校验。
- 任务 outbox 基础投递：以 MySQL `task_outbox` 为事实源，按配置投递任务事件到 Redis 队列，并记录失败原因和重试时间。
- 任务 Worker 基础状态机：领取 `QUEUED` 任务、写入 worker 租约和心跳、按 owner 校验完成成功或失败；执行业务前校验项目仍为可写状态；Redis 队列坏消息会记录原因和 payload 摘要后拒绝，不 claim 任务。
- 报告创建、列表、详情、重新生成、下载 URL、版本记录和 CryptoAgentV3 集成。
- OCR 识别后端接口：提交识别、列表、详情、重试、删除、字段修订、结果 JSON 查询和类型模板。
- Java AI 适配层：模型调用、Agent 调用、RAG 检索/索引、数据库问答、路由、上下文准备和外部调用日志。
- Python 智能算法服务：新增 `/v1/ocr/recognize`，封装 Qwen VL 完成身份证、车牌、发票和自定义字段 OCR 抽取。
- Redis 基础封装、MinIO 适配、Flyway 迁移、MyBatis XML、PageHelper 分页。

前端：

- Vue 3 + TypeScript + Vite 工程。
- Pinia、Vue Router、Axios 请求封装和权限路由。
- 登录页、首页工作台、知识库、知识问答、合规审查、报告、OCR 页面。
- 项目管理、项目成员、用户管理、角色权限页面。
- 403、404 页面。
- 通用上传、表格、搜索、弹窗、状态、进度、JSON 查看、下载组件。

### 规划中

- 登录失败锁定、密码强度策略、登录审计、刷新令牌等完整安全策略。
- OCR 模块由其他成员负责，当前 P0 后端计划不推进 OCR 业务接口。
- 数据源管理页面、数据库问答历史和数据源权限管理。
- 知识问答业务页面与 Java/Python AI 适配层联调完善。
- 合规审查后端业务实现。
- Python 智能算法服务生产化。
- Agent 工具注册、业务工具执行审计和多步骤任务编排完善。
- 生产部署脚本、监控告警和审计报表。

## 目录结构

```text
smart_worksite/
  deploy/                 本地依赖环境，MySQL、Redis、MinIO
  docs/                   需求文档、架构设计文档、接口文档
  frontend/               Vue 3 + TypeScript 前端工程
  python-ai-service/      Python 智能算法服务
  src/main/java/          Java 后端源码
  src/main/resources/     后端配置、Mapper XML、Flyway 脚本
  src/test/java/          后端测试
  pom.xml                 Maven 配置
  README.md               项目总览
```

后端主要包结构：

```text
com.xd.smartworksite
  common                  通用响应、异常、请求 ID、MyBatis、Redis、安全工具
  system                  系统探活
  auth                    登录认证、用户、角色、权限、项目成员管理
  project                 项目管理
  file                    文件管理和文件解析
  template                模板管理
  report                  报告生成和 CryptoAgentV3 集成
  knowledge               知识库基础管理、文档生命周期和异步 RAG 索引任务
  datasource              数据源基础管理和数据库问答支撑
  qa                      Knowledge QA sessions, messages, references, feedback, and AI routing loop
  review                  Compliance review records, issues, status handling, and Python Agent review loop
  ocr                     OCR foundation tables; OCR business APIs are outside the current P0 backend scope
  task                    任务查询、统计、重试、取消和阶段日志
  audit                   审计和外部调用日志基础表
  ai                      Java AI 适配层，调用 Python 智能算法服务
```

## 本地启动

AI/audit observability rule: AI external-call logs and audit logs must check affected rows and generated IDs. Python service failures keep the original error while attaching log-persistence failures for diagnosis; successful AI/audit operations must not be reported as successful when required trace records cannot be persisted.

### 1. 启动本地依赖

```powershell
cd deploy
copy .env.example .env
docker compose -f docker-compose-env.yml --env-file .env up -d
```

Docker 只启动 MySQL、Redis、MinIO。业务表由 Flyway 自动迁移创建，不通过 Docker 初始化 SQL 创建。

### 2. 启动后端

```powershell
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

本地开发默认管理员账号：`admin / admin123`。Flyway 迁移 `V8__reset_default_admin_password.sql` 会重置该密码，仅用于本地联调和演示；生产环境必须重置或禁用默认管理员密码。

### 3. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

### 4. 启动 Python 智能算法服务

```powershell
cd python-ai-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env
# Configure QWEN_API_KEY and AI_SERVICE_API_KEY in .env
uvicorn app.main:app --host 0.0.0.0 --port 8015
```

Java 后端通过 `AI_PYTHON_BASE_URL` 和 `AI_PYTHON_API_KEY` 调用 Python 服务。

## 当前接口

除 `/api/auth/login`、`/api/system/ping`、`/actuator/health`、`/actuator/info` 外，当前接口默认需要 `Authorization: Bearer <accessToken>`。

JWT 鉴权会回查当前用户状态；用户被停用或删除后，旧 token 不再注入认证上下文，后续请求按未登录处理。

### 系统

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/system/ping` | 系统探活 |
| GET | `/api/system/version` | Query version and service time |
| GET | `/api/system/runtime` | Query JVM, OS, and runtime status |
| GET | `/api/system/dependencies/health` | Query MySQL, Redis, and MinIO dependency health |
| GET | `/actuator/health` | 健康检查 |

### 认证与当前用户

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 登录并获取 JWT |
| POST | `/api/auth/logout` | 退出登录并拉黑当前 JWT |
| GET | `/api/auth/me` | 获取当前用户、角色、权限和默认项目 |
| PUT | `/api/auth/me/password` | 修改当前用户密码 |

### 用户、角色、权限

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/system/users` | 分页查询用户 |
| POST | `/api/system/users` | 创建用户 |
| GET | `/api/system/users/{userId}` | 查询用户详情 |
| PUT | `/api/system/users/{userId}` | 更新用户信息和角色 |
| PUT | `/api/system/users/{userId}/status?status=ENABLED|DISABLED` | 启用或停用用户 |
| PUT | `/api/system/users/{userId}/password` | 管理员重置用户密码 |
| GET | `/api/system/roles` | 查询角色列表 |
| POST | `/api/system/roles` | 创建角色 |
| PUT | `/api/system/roles/{roleId}` | 更新角色基础信息和权限 |
| PUT | `/api/system/roles/{roleId}/status` | 启用或停用角色，查询参数 `status=ENABLED|DISABLED`，内置角色受保护 |
| DELETE | `/api/system/roles/{roleId}` | 删除未被用户使用的非内置角色 |
| GET | `/api/system/roles/permissions` | 查询权限列表 |
| PUT | `/api/system/roles/{roleId}/permissions` | 更新角色权限 |

### 项目与项目成员

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/projects` | 分页查询项目列表 |
| POST | `/api/projects` | 创建项目 |
| GET | `/api/projects/{projectId}` | 查询项目详情 |
| PUT | `/api/projects/{projectId}` | 修改项目 |
| PUT | `/api/projects/{projectId}/status` | 更新项目状态：`ENABLED`、`DISABLED`、`ARCHIVED`，请求值大小写不敏感，后端统一保存为大写 |
| DELETE | `/api/projects/{projectId}` | 逻辑删除项目 |
| POST | `/api/projects/{projectId}/enable` | 启用项目 |
| POST | `/api/projects/{projectId}/disable` | 停用项目 |
| POST | `/api/projects/{projectId}/archive` | 归档项目 |
| GET | `/api/projects/{projectId}/settings` | 查询项目配置 |
| PUT | `/api/projects/{projectId}/settings` | 更新项目配置 |
| GET | `/api/projects/{projectId}/statistics` | 查询项目统计 |

项目状态写入规则：`DISABLED` 或 `ARCHIVED` 项目只允许读取和重新启用；创建/修改项目业务数据、成员、文件、模板、知识库、数据源、QA、审查、报告、任务重试/取消、AI 调用等写操作会直接返回冲突错误，不做静默兜底。

项目配置校验规则：默认知识库必须存在、属于当前项目且处于 `ENABLED`；默认报告模板必须存在、属于当前项目、类别为 `REPORT` 且处于 `ENABLED`；默认问答路由只允许 `AUTO`、`MODEL`、`KNOWLEDGE`、`DATABASE`、`MIXED`；默认导出格式只允许 `WORD` 或 `PDF`。
| GET | `/api/projects/{projectId}/members` | 查询项目成员 |
| POST | `/api/projects/{projectId}/members` | 添加项目成员 |
| PUT | `/api/projects/{projectId}/members/{userId}` | 修改项目成员角色 |
| DELETE | `/api/projects/{projectId}/members/{userId}` | 移除项目成员 |

### 文件

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/files` | 上传文件，`multipart/form-data` |
| GET | `/api/files` | 分页查询文件列表 |
| GET | `/api/files/{fileId}` | 查询文件详情 |
| GET | `/api/files/{fileId}/access-url?usage=DOWNLOAD\|PREVIEW` | 获取访问 URL |
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

文件上传和解析任务创建必须在写入后读回到可追踪记录；读回失败时直接返回错误，已上传对象必须尽力清理，不允许返回内存对象假成功。

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
| POST | `/api/templates/report` | 上传报告模板 |
| POST | `/api/templates/review` | 上传审查模板 |
| POST | `/api/report/templates` | 上传报告模板兼容接口 |
| GET | `/api/report/templates` | 查询报告模板列表 |
| GET | `/api/report/templates/{templateId}/variables` | 查询报告模板变量；后端会读取已上传模板文件并解析 `${变量名}` 占位符，不支持或读取失败时直接返回错误 |
| POST | `/api/review/templates` | 上传审查模板兼容接口 |
| GET | `/api/review/templates` | 查询审查模板列表 |

报告模板变量解析规则：变量接口必须读取模板文件真实内容，当前支持 DOCX、TXT、MD 中的 `${变量名}` 占位符；模板文件缺失、跨项目不一致、文件为空、格式不支持或对象存储读取失败时直接返回错误，不允许返回假空列表。

模板写入规则：模板上传创建后必须读回持久化记录再返回成功；模板修改、启用、停用和删除必须检查数据库影响行数，记录不存在或状态已变化时直接返回冲突错误，不允许静默成功。

### 报告

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/reports` | 创建报告生成任务，`reportName`、`reportType`、`templateId` 必填，返回 `PENDING` 和 `taskId`，由任务 outbox/Worker 异步调用 CryptoAgentV3 |
| GET | `/api/reports` | 分页查询报告列表 |
| GET | `/api/reports/{reportId}` | 查询报告详情 |
| POST | `/api/reports/{reportId}/regenerate` | 重新生成报告 |
| GET | `/api/reports/{reportId}/download?format=WORD` | 获取 Word 报告下载 URL |

报告生成引用文件规则：`referenceFileIds` 中的文件必须存在且属于当前报告项目；跨项目文件引用会返回权限错误并将报告标记为 `FAILED`，不允许使用其他项目资料继续生成。报告列表 `status` 查询只允许 `DRAFT`、`PENDING`、`PROCESSING`、`COMPLETED`、`FAILED`、`ARCHIVED`、`DELETED`。

Report write rule: report generation must check affected rows for report-task linking, task status, processing, success, failed, and version file binding. A zero-row update is a conflict and must not be reported as completed generation.

P0 write confirmation addendum: project creates/updates/status/settings, file-object inserts, file-parse-record inserts, template file/template inserts and file business-ID binding, report config/report/task/output-file/version inserts, and review-record inserts must check affected rows or generated IDs and read back records where the API returns persisted data. Missing effects are conflicts or system errors, not successful operations.

### 任务

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/tasks` | 分页查询当前用户可访问项目内的任务 |
| GET | `/api/tasks/statistics` | 查询任务状态统计和待处理数量 |
| GET | `/api/tasks/{taskId}` | 查询任务详情 |
| GET | `/api/tasks/{taskId}/stages` | 查询任务阶段日志 |
| POST | `/api/tasks/{taskId}/retry` | 重试失败且未超过重试次数的任务 |
| POST | `/api/tasks/{taskId}/cancel` | 取消等待中的任务，或对运行中任务写入取消请求 |

任务 outbox 调度默认关闭，避免本地未启动 Redis 时影响后端启动。需要投递 Redis 队列时设置：

```properties
TASK_OUTBOX_DISPATCHER_ENABLED=true
TASK_OUTBOX_DISPATCHER_BATCH_SIZE=20
TASK_OUTBOX_DISPATCHER_FIXED_DELAY_MS=5000
```

Task write rule: task state transitions, stage logs, and task outbox events must confirm affected rows or generated IDs. Retry/cancel/worker/outbox operations fail with conflict when status records, stage traces, or durable outbox failure states cannot be persisted.

Auth write rule: user, password, role, role-permission, project-member, and last-login writes must check affected rows. A zero-row write is treated as conflict and must not be reported as successful account or permission management.

### 知识库

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/projects/{projectId}/knowledge-bases` | 创建项目知识库 |
| GET | `/api/projects/{projectId}/knowledge-bases` | 分页查询项目知识库 |
| GET | `/api/knowledge-bases/{knowledgeBaseId}` | 查询知识库详情 |
| PUT | `/api/knowledge-bases/{knowledgeBaseId}` | 修改知识库元数据 |
| POST | `/api/knowledge-bases/{knowledgeBaseId}/enable` | 启用知识库 |
| POST | `/api/knowledge-bases/{knowledgeBaseId}/disable` | 停用知识库 |
| DELETE | `/api/knowledge-bases/{knowledgeBaseId}` | 删除知识库 |
| POST | `/api/knowledge-bases/{knowledgeBaseId}/documents` | 上传知识库文档，创建待入库文档记录 |
| GET | `/api/knowledge-bases/{knowledgeBaseId}/documents` | 分页查询知识库文档 |
| GET | `/api/knowledge-documents/{documentId}` | 查询知识库文档详情 |
| DELETE | `/api/knowledge-documents/{documentId}` | 删除知识库文档 |
| POST | `/api/knowledge-documents/{documentId}/index` | 创建知识库文档入库任务，返回 `INDEXING` 与 `taskId`，由任务 outbox/Worker 异步调用 Python RAG 索引 |

Knowledge write rule: knowledge-base updates must check affected rows; document uploads must verify generated IDs and read back persisted records before success; `INDEXING`, `SUCCESS`, and `FAILED` indexing status writes must check affected rows, and failure-state persistence failures must surface conflict errors with the original error retained.

### 数据源

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/data-sources` | 创建数据源配置 |
| GET | `/api/data-sources` | 分页查询数据源 |
| GET | `/api/data-sources/{dataSourceId}` | 查询数据源详情 |
| POST | `/api/data-sources/{dataSourceId}/test` | Test data source connectivity with real JDBC |
| GET | `/api/data-sources/{dataSourceId}/schema` | Inspect data source schema with real JDBC |
| PUT | `/api/data-sources/{dataSourceId}` | 编辑数据源配置 |
| POST | `/api/data-sources/{dataSourceId}/enable` | 启用数据源 |
| POST | `/api/data-sources/{dataSourceId}/disable` | 停用数据源 |
| DELETE | `/api/data-sources/{dataSourceId}` | 删除数据源 |

数据源密码使用 AES-GCM 存储，创建或更新密码前必须配置 `AI_DATA_SOURCE_PASSWORD_KEY`，长度为 16、24 或 32 字节，或使用 `base64:` 前缀。
数据源写入规则：创建后必须读回持久化记录；修改、启用、停用和删除必须检查数据库影响行数，记录不存在或已变更时直接返回错误，不允许静默成功。

### QA

QA read APIs require `qa:view`; create/update/archive/send/regenerate/feedback APIs require `qa:manage`.

QA 提问时，`knowledgeBaseIds` 必须存在、属于当前会话项目且处于 `ENABLED`；`dataSourceIds` 必须存在、属于当前会话项目且处于 `ENABLED`。跨项目、停用或不存在的引用会在调用 AI 前直接失败，避免把其他项目资料或数据源传给模型。

QA 消息写入规则：问题消息创建后必须持有可读 ID；AI 返回后写入答案、引用和状态时必须检查数据库影响行数，写入失败直接返回冲突错误，不允许把未持久化的 AI 答案报告为成功。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/qa/sessions` | Create QA session; default title is used when blank |
| GET | `/api/qa/sessions` | List QA sessions |
| GET | `/api/qa/sessions/{sessionId}` | Get QA session detail |
| PUT | `/api/qa/sessions/{sessionId}` | Update QA session title |
| DELETE | `/api/qa/sessions/{sessionId}` | Archive QA session |
| POST | `/api/qa/sessions/{sessionId}/messages` | Send question through Java AI adapter |
| GET | `/api/qa/sessions/{sessionId}/messages` | List QA messages |
| POST | `/api/qa/sessions/{sessionId}/messages/{messageId}/regenerate` | Regenerate answer |
| GET | `/api/qa/messages/{messageId}` | Get QA message detail |
| GET | `/api/qa/messages/{messageId}/references` | List answer references |
| POST | `/api/qa/messages/{messageId}/feedback` | Submit answer feedback |

### Review

Review read APIs require `review:view`; submit/retry/delete/archive/update-issue APIs require `review:manage`.

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/review/records` | Submit review file and create review record via Python Agent |
| GET | `/api/review/records` | List review records |
| GET | `/api/review/records/{recordId}` | Get review record detail and issues |
| POST | `/api/review/records/{recordId}/retry` | Retry failed review |
| DELETE | `/api/review/records/{recordId}` | Delete review record |
| POST | `/api/review/records/{recordId}/archive` | Archive review record |
| PUT | `/api/review/records/{recordId}/issues/{issueId}` | Update review issue status |

审查执行失败写入规则：Python Agent 返回失败、空结果或无效 JSON 时，审查记录必须标记为 `FAILED` 并记录错误信息；如果失败状态无法落库，必须直接返回冲突错误，不允许丢失可观测性。
审查创建写入规则：提交审查记录后必须校验生成 ID 并读回持久化记录；读回失败时不调用 Python Agent，直接返回系统错误。

### 审计

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/audit/logs` | Query operation audit logs |
| GET | `/api/audit/external-call-logs` | Query external service call logs |

### Java AI 适配层

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/ai/model/invoke` | 调用 Python 模型能力 |
| POST | `/api/ai/agent/invoke` | 调用 Python Agent 能力 |
| POST | `/api/ai/knowledge/search` | RAG 检索 |
| POST | `/api/ai/knowledge/index` | RAG 索引 |
| POST | `/api/ai/database/query` | 数据库问答，只执行安全只读 SQL |
| POST | `/api/ai/route` | 智能路由 |
| POST | `/api/ai/context/prepare` | 上下文准备 |
| GET | `/api/ai/external-call-logs` | 查询外部 AI 调用日志 |

## 数据库迁移

Flyway 脚本位于：

```text
src/main/resources/db/migration
```

当前脚本：

| 脚本 | 说明 |
| --- | --- |
| `V1__init_schema.sql` | 初始化用户、角色、项目、文件、知识库、数据源、任务、审计、系统配置等基础表 |
| `V2__extend_file_object.sql` | 扩展文件对象字段 |
| `V3__template_report_schema.sql` | 新增模板、报告配置、报告主表和报告版本表 |
| `V4__create_file_parse_record.sql` | 新增文件解析记录表 |
| `V5__align_report_status_with_api_doc.sql` | 对齐报告状态枚举 |
| `V6__add_missing_foundation_tables.sql` | 补充问答、审查、OCR 基础表 |
| `V7__add_business_permissions.sql` | 新增前端路由和业务菜单权限 |
| `V8__reset_default_admin_password.sql` | 重置本地默认管理员密码为 `admin123` |
| `V9__align_task_and_parse_status_contracts.sql` | 对齐任务、报告和文件解析状态枚举 |
| `V10__extend_generate_task_runtime_fields.sql` | 扩展任务运行时字段、阶段日志尝试次数和任务 outbox 表 |
| `V11__add_knowledge_and_datasource_manage_permissions.sql` | 新增知识库和数据源管理权限 |
| `V12__add_qa_manage_permission_and_indexes.sql` | 新增问答管理权限和问答查询索引 |
| `V13__add_review_manage_permission.sql` | 新增合规审查管理权限 |
| `V14__extend_knowledge_document_index_task.sql` | 扩展知识库文档索引任务ID和失败原因字段 |

数据库结构变更必须新增 Flyway 脚本，不要修改已经合入并被团队使用的旧脚本。

## 外部服务配置

### Qwen-VL OCR / 文档解析

```env
QWEN_VL_ENDPOINT=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
QWEN_VL_API_KEY=
QWEN_VL_MODEL=qwen-vl-plus
QWEN_VL_MAX_TOKENS=8192
```

OCR 识别由 `python-ai-service` 封装 Qwen VL，Java 后端只调用 Python OCR API，不直接调用 `QWEN_VL_ENDPOINT`。图片 OCR 会在 Python 侧下载 Java 生成的临时文件 URL，并转为 `data:image/...;base64,...` 后发送给 Qwen VL，避免把本地或内网 MinIO 临时签名 URL 直接转发给云端模型服务。

### CryptoAgentV3 报告生成

报告创建接口不直接阻塞调用 CryptoAgentV3。开启任务 outbox 调度和 Worker 后，Worker 会领取 `REPORT_GENERATION` 任务，确认项目仍为 `ENABLED` 后再调用外部服务；本地测试可使用 fake client，不依赖真实 CryptoAgent 服务。

```env
CRYPTO_AGENT_V3_BASE_URL=http://127.0.0.1:8012
CRYPTO_AGENT_V3_INVOKE_PATH=/v1/report-generation/invoke
CRYPTO_AGENT_V3_CONNECT_TIMEOUT_SECONDS=5
CRYPTO_AGENT_V3_READ_TIMEOUT_SECONDS=3000000
```

### Python AI Service

```env
AI_PYTHON_BASE_URL=http://127.0.0.1:8015
AI_PYTHON_API_KEY=
AI_PYTHON_CONNECT_TIMEOUT_MS=5000
AI_PYTHON_READ_TIMEOUT_MS=120000
AI_PYTHON_RETRY_COUNT=1
```

Qwen 密钥必须配置在 `python-ai-service/.env` 或运行环境变量中，不得配置到 Java。

## Python AI 能力配置

知识库文档入库由 Java 创建 `KNOWLEDGE_INDEXING` 异步任务并通过任务 outbox/Worker 执行。Worker 执行前必须确认项目仍为 `ENABLED`；只读取已成功解析的文件内容并调用 Python RAG 索引接口；解析内容未就绪、内容为空或 Python 索引失败时，文档状态写为 `FAILED` 并记录错误，不做静默兜底或假成功。

Knowledge indexing write rule: `INDEXING`, `SUCCESS`, and `FAILED` status updates must check affected rows. If persisting `FAILED` also affects zero rows, the worker must fail visibly instead of losing the original Python/parse error.

Python AI 服务支持 RAG 索引和检索。文档会在 Python 服务中切片、向量化、存入向量提供方并 rerank。常用配置：

```env
EMBEDDING_PROVIDER=QWEN
QWEN_EMBEDDING_MODEL=text-embedding-v4
QWEN_EMBEDDING_DIMENSIONS=1024
QWEN_EMBEDDING_BATCH_SIZE=10
RERANK_PROVIDER=QWEN
QWEN_RERANK_BASE_URL=https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank
QWEN_RERANK_MODEL=qwen3-rerank
QWEN_RERANK_API_STYLE=LEGACY
RAG_PROVIDER=LOCAL
RAG_DATA_DIR=data/rag
PGVECTOR_DSN=
PGVECTOR_TABLE=smart_worksite_chunks
MILVUS_URI=http://127.0.0.1:19530
MILVUS_TOKEN=
MILVUS_COLLECTION=smart_worksite_chunks
```

`EMBEDDING_PROVIDER=QWEN` 是生产路径，`LOCAL_HASH` 仅用于离线测试和无模型额度开发。Java 不直接访问向量数据库。

## 文档

| 文档 | 说明 |
| --- | --- |
| `docs/智慧工地大模型应用系统-需求文档.md` | 系统需求文档，包含详细需求 |
| `docs/智慧工地大模型应用系统-架构设计文档.md` | 架构设计文档 |
| `docs/智慧工地大模型应用系统-接口文档.md` | 接口设计文档 |
| `docs/任务分工.xlsx` | 任务分工表 |
| `智慧工地前端UI风格指南.md` | 前端 UI 风格指南 |

注意：`docs` 描述的是完整目标系统，当前代码是阶段性实现。判断已实现接口时，以当前 Controller 和本 README 的当前接口列表为准。

## 测试

后端改动后运行：

```powershell
mvn clean test
```

当前 P0 后端测试门禁包含：基础安全、Flyway 迁移连续性、MyBatis JSON 参数规则、项目隔离、任务状态机、任务 outbox、知识库索引、QA、合规审查、报告生成、模板/文件上传 fail-fast 行为。新增或调整后端核心链路时，必须保证这些测试继续通过。

前端改动后运行：

```powershell
cd frontend
npm run build
```

当前 P0 验证还要求：非 OCR Controller 路由在 README 和接口文档中可追踪；前端非 OCR API 调用能匹配 Java 后端路由；文档编码检查通过；并确认 OCR 后端目录没有被修改。

Frontend report-template upload calls must send explicit `templateName` and `templateType` with the file; the backend does not derive fallback metadata from filenames.
