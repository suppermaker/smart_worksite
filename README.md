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
| Spring Data Redis | Spring Boot Starter Data Redis | Redis 访问、缓存、JWT 黑名单 |
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
| Redis | 7.2-alpine | 缓存、轻量队列、分布式锁、JWT 黑名单 |
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
- 项目列表、详情、创建、修改、启停和逻辑删除。
- 文件上传、列表、详情、访问 URL 和删除。
- 文件解析任务创建、记录查询、内容查询和重试。
- 模板上传、列表、详情、修改、启用、停用和删除。
- 报告模板和审查模板兼容接口。
- 报告创建、列表、详情、重新生成、下载 URL、版本记录和 CryptoAgentV3 集成。
- Java AI 适配层：模型调用、Agent 调用、RAG 检索/索引、数据库问答、路由、上下文准备和外部调用日志。
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
- 项目级读接口隔离和跨模块数据权限进一步收口。
- 知识库完整入库、文档状态管理和向量检索业务闭环。
- 数据源管理页面、数据库问答历史和数据源权限管理。
- 知识问答业务页面与 Java/Python AI 适配层联调完善。
- 合规审查后端业务实现。
- OCR 识别后端业务实现。
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
  knowledge               知识库元数据，完整入库规划中
  datasource              数据源基础表和数据库问答支撑，管理功能规划中
  qa                      知识问答基础表，业务接口规划中
  review                  合规审查基础表，业务接口规划中
  ocr                     OCR 识别基础表，业务接口规划中
  task                    任务和阶段日志基础表，编排能力规划中
  audit                   审计和外部调用日志基础表
  ai                      Java AI 适配层，调用 Python 智能算法服务
```

## 本地启动

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

### 系统

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/system/ping` | 系统探活 |
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
| GET | `/api/system/roles/permissions` | 查询权限列表 |
| PUT | `/api/system/roles/{roleId}/permissions` | 更新角色权限 |

### 项目与项目成员

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/projects` | 分页查询项目列表 |
| POST | `/api/projects` | 创建项目 |
| GET | `/api/projects/{projectId}` | 查询项目详情 |
| PUT | `/api/projects/{projectId}` | 修改项目 |
| PUT | `/api/projects/{projectId}/status` | 启用或停用项目 |
| DELETE | `/api/projects/{projectId}` | 逻辑删除项目 |
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
| GET | `/api/files/{fileId}/access-url?inline=true|false` | 获取访问 URL |
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
| POST | `/api/templates/report` | 上传报告模板 |
| POST | `/api/templates/review` | 上传审查模板 |
| POST | `/api/report/templates` | 上传报告模板兼容接口 |
| GET | `/api/report/templates` | 查询报告模板列表 |
| GET | `/api/report/templates/{templateId}/variables` | 查询报告模板变量 |
| POST | `/api/review/templates` | 上传审查模板兼容接口 |
| GET | `/api/review/templates` | 查询审查模板列表 |

### 报告

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/reports` | 创建报告生成任务并调用 CryptoAgentV3 |
| GET | `/api/reports` | 分页查询报告列表 |
| GET | `/api/reports/{reportId}` | 查询报告详情 |
| POST | `/api/reports/{reportId}/regenerate` | 重新生成报告 |
| GET | `/api/reports/{reportId}/download?format=WORD` | 获取 Word 报告下载 URL |

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

数据库结构变更必须新增 Flyway 脚本，不要修改已经合入并被团队使用的旧脚本。

## 外部服务配置

### Qwen-VL 文档解析

```env
QWEN_VL_ENDPOINT=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
QWEN_VL_API_KEY=
QWEN_VL_MODEL=qwen-vl-plus
```

### CryptoAgentV3 报告生成

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

前端改动后运行：

```powershell
cd frontend
npm run build
```
