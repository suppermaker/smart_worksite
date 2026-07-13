# 智慧工地前端

Vue 3 + TypeScript + Vite + Pinia + Vue Router + Axios + Element Plus，Element Plus 通过自动按需导入减少首屏包体积。

## 技术栈

| 技术 | 用途 |
| --- | --- |
| Vue 3 | 前端框架 |
| TypeScript | 类型约束 |
| Vite | 开发服务和构建工具 |
| Pinia | 全局状态管理 |
| Vue Router | 路由和权限守卫 |
| Axios | HTTP请求 |
| Element Plus | UI组件库 |
| unplugin-auto-import / unplugin-vue-components | Element Plus按需导入 |
| @element-plus/icons-vue | 图标库 |

前端只调用 Java Spring Boot 后端接口，不直接调用 Python 智能算法服务、数据库、对象存储或向量库。智能体、RAG、OCR算法和文档解析等AI能力由 Python 服务实现，并由 Java 后端统一编排。

## 启动方式

```powershell
cd F:\xd_project\smart_worksite\frontend
npm install
npm run dev
```

默认访问：`http://localhost:5173`。

## 构建方式

```powershell
npm run build
```

构建产物输出到 `dist/`。

## 环境变量

开发环境配置在 `.env.development`：

```env
VITE_API_BASE_URL=/api
VITE_USE_MOCK=false
```

- `VITE_API_BASE_URL`：后端 API 基础地址，开发代理默认使用 `/api`。
- `VITE_USE_MOCK`：默认 `false`，请求真实 Java 后端；只有显式设为 `true` 时才允许 API service 使用本地 mock 数据。
- 模块级 `VITE_USE_xxx_MOCK` 默认也必须为 `false`，仅用于离线演示。

## Mock 模式

mock 数据统一放在 `src/mocks/`，页面通过 `src/api/` service 间接获取数据，不在页面中拼接 URL。真实联调和交付验收不得依赖 mock；接口失败必须显示后端错误，不返回假成功或假空数据。

## 目录结构

```text
src/
  api/                 接口 service 和类型
  assets/              全局样式和静态资源
  components/common/   通用上传、表格、状态、JSON、下载等组件
  components/business/ 业务组件预留
  layouts/             主框架布局
  mocks/               本地 mock 数据
  router/              路由和权限守卫
  stores/              Pinia 状态
  utils/               request、requestId 等工具
  views/               登录、首页、项目、文件、模板、知识库、问答、审查、报告、OCR、数据源、任务、审计、错误页
```

## History 路由部署说明

前端使用 `createWebHistory()`。生产部署时 Nginx 或静态资源服务器必须把未知前端路由回退到 `index.html`，否则刷新 `/qa`、`/report/10001` 会返回 404。

```nginx
location / {
  try_files $uri $uri/ /index.html;
}
```

## 后续联调说明

- 登录：`POST /api/auth/login`、`GET /api/auth/me`、退出登录接口。
- 项目：`GET /api/projects`，项目切换后刷新当前页面数据。
- 知识库：知识库列表、文档列表、上传文档、触发入库；文档仅在 `PENDING` 或 `FAILED` 状态显示可提交入库，`INDEXING` 和 `SUCCESS` 禁止重复提交。
- 问答：会话、消息、反馈接口；发送问题前可选择模型、知识库、数据库或混合路由，数据库路由必须选择一个已启用数据源，知识库路由必须选择已启用知识库。
- 合规审查：只加载已启用审查模板，提交审查、查询审查结果；已完成审查的问题状态可按后端枚举更新为 `OPEN`、`PROCESSING`、`RESOLVED` 或 `IGNORED`。
- 报告：列表、详情、重新生成、下载 Word；新建报告只加载已启用报告模板，仅已完成报告允许下载，生成中报告禁止重复发起生成。
- OCR：提交识别、查询结果、保存人工修订。
- 数据源：列表、连接测试、Schema 查看和数据库问答为登录可见；新增、编辑、启用、停用、删除按钮按后端 `datasource:manage` 权限禁用，真实写入仍由后端 fail-fast 校验。
- 权限按钮：项目、文件、模板、知识库、问答、审查、报告、数据源等页面的写操作只在当前用户具备对应后端权限时展示或可点击；前端只做可见性和交互保护，后端权限校验仍是最终事实来源。
- 任务中心：提供唯一的任务列表、统计、详情、阶段日志、重试、取消入口；项目管理只保留 `/projects` 一个主入口，旧 `/project/manage` 重定向到 `/projects`。
- 长任务：报告生成、OCR、知识库入库通过任务详情和阶段日志接口展示进度；任务操作按钮必须按状态机禁用非法操作，例如仅 `FAILED` 可重试，`PENDING`、`QUEUED`、`RUNNING`、`RETRYING` 可取消。
- 文件管理：只提供审查文档上传、下载、解析记录和解析内容入口；知识库文档上传必须走知识库页面，报告模板/审查模板上传必须走模板中心，报告结果只能由报告生成任务产生，避免重复入口和孤儿业务文件。
- 文件解析：仅 `SUCCESS` 解析记录允许查看内容，仅 `FAILED` 解析记录允许重试解析；模板上传限制为 DOCX、TXT、MD，避免上传后端变量解析不支持的模板文件。
- 数据质量：前端会对项目、模板、问答历史中的疑似历史乱码文本打标提示，但不静默修改或隐藏后端返回的数据。
- 下载：统一使用 `downloadFile`，可解析 Blob 中的 JSON 错误和 `Content-Disposition` 文件名。
