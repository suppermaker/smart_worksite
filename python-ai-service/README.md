# Smart Worksite Python AI Service

This service wraps Qwen model calls for the Java backend. The frontend must not call it directly.

## Start

```powershell
cd python-ai-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env
# set QWEN_API_KEY in .env
uvicorn app.main:app --host 0.0.0.0 --port 8015
```

## API Key

Set `AI_SERVICE_API_KEY` in Python and `AI_PYTHON_API_KEY` in Java. Java sends it as `X-AI-Service-Key`.

Qwen secrets stay in this Python service. Do not configure Qwen API keys in Java.


## RAG and Agent

RAG indexing is available at `POST /v1/rag/index`; search is available at `POST /v1/rag/search`. The service chunks documents, creates embeddings, writes them to the configured vector store, retrieves by vector similarity, and applies Qwen rerank when configured, with lexical fallback for offline tests or provider failures.

Vector providers:

- `RAG_PROVIDER=LOCAL`: local JSONL vector store for development.
- `RAG_PROVIDER=PGVECTOR`: PostgreSQL with pgvector extension.
- `RAG_PROVIDER=MILVUS`: Milvus collection.

Embedding providers:

- `EMBEDDING_PROVIDER=QWEN`: use Qwen embeddings.
- `EMBEDDING_PROVIDER=LOCAL_HASH`: deterministic local embeddings for offline tests only.

Rerank providers:

- `RERANK_PROVIDER=QWEN`: use the DashScope text-rerank endpoint, configured by `QWEN_RERANK_BASE_URL`, `QWEN_RERANK_MODEL`, and `QWEN_RERANK_API_STYLE`. `LEGACY` uses the DashScope `input/parameters` body, which is the verified request shape for the current text-rerank endpoint.
- Any non-`QWEN` value uses the built-in lexical rerank fallback.

Agent tool execution uses a registry. The built-in `rag_search` tool lets the Agent retrieve indexed project knowledge during reasoning; `database_query_plan` lets the Agent ask the database-Q&A planner for read-only SQL plans.
