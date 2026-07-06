# deploy

本目录用于启动本地开发依赖服务，包括 MySQL、Redis 和 MinIO。

## 文件说明

| 文件 | 说明 |
| --- | --- |
| `.env.example` | 本地环境变量示例 |
| `.env` | 本地实际环境变量，复制 `.env.example` 后生成 |
| `docker-compose-env.yml` | MySQL、Redis、MinIO 的 Docker Compose 编排文件 |

## 启动依赖

```powershell
cd deploy
copy .env.example .env
docker compose -f docker-compose-env.yml --env-file .env up -d
```

## 查看服务状态

```powershell
docker compose -f docker-compose-env.yml --env-file .env ps
```

## 查看日志

```powershell
docker compose -f docker-compose-env.yml --env-file .env logs -f mysql
docker compose -f docker-compose-env.yml --env-file .env logs -f redis
docker compose -f docker-compose-env.yml --env-file .env logs -f minio
```

## 停止服务

```powershell
docker compose -f docker-compose-env.yml --env-file .env down
```

## 清理数据

以下命令会删除 MySQL、Redis、MinIO 的 volume 数据。执行后数据库、缓存和文件对象都会被清空。

```powershell
docker compose -f docker-compose-env.yml --env-file .env down -v
```

## 默认服务地址

| 服务 | 地址 |
| --- | --- |
| MySQL | `localhost:3306` |
| Redis | `localhost:6379` |
| MinIO API | `http://localhost:9000` |
| MinIO Console | `http://localhost:9001` |

## 默认账号

| 服务 | 用户名 | 密码 |
| --- | --- | --- |
| MySQL业务用户 | `worksite` | `worksite` |
| MySQL root | `root` | `root` |
| MinIO | `minioadmin` | `minioadmin` |

## MinIO bucket

启动时 `minio-init` 会自动创建 `.env` 中配置的 bucket：

```env
MINIO_BUCKET=smart-worksite
```

bucket 默认设置为私有访问。后端通过 MinIO SDK 生成临时预签名 URL 进行下载和预览。

## 外部AI服务配置

`.env.example` 中包含文档解析和报告生成相关配置：

```env
QWEN_VL_ENDPOINT=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
QWEN_VL_API_KEY=
QWEN_VL_MODEL=qwen-vl-plus
CRYPTO_AGENT_V3_BASE_URL=http://127.0.0.1:8012
CRYPTO_AGENT_V3_INVOKE_PATH=/v1/report-generation/invoke
CRYPTO_AGENT_V3_CONNECT_TIMEOUT_SECONDS=5
CRYPTO_AGENT_V3_READ_TIMEOUT_SECONDS=3000000
```

说明：

- Qwen-VL 用于文件解析模块的模型解析适配。
- CryptoAgentV3 用于报告生成模块。
- 智能体和复杂 AI 能力由 Python 服务实现，Java 后端通过 HTTP 调用。
- 如果本机没有启动对应 Python 服务，相关接口会返回外部服务调用失败。
