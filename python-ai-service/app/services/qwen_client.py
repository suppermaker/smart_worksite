import json
from typing import Any
import httpx
from app.core.settings import Settings
from app.models.schemas import Message


class QwenClient:
    def __init__(self, settings: Settings):
        self.settings = settings

    async def chat(self, messages: list[Message], model: str | None = None, parameters: dict[str, Any] | None = None) -> tuple[str, dict[str, Any]]:
        if not self.settings.qwen_api_key:
            raise RuntimeError("QWEN_API_KEY is not configured")
        payload: dict[str, Any] = {
            "model": model or self.settings.qwen_model,
            "messages": [{"role": item.role, "content": item.content} for item in messages],
        }
        if parameters:
            payload.update(parameters)
        url = self.settings.qwen_base_url.rstrip("/") + "/chat/completions"
        headers = {
            "Authorization": f"Bearer {self.settings.qwen_api_key}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        }
        async with httpx.AsyncClient(timeout=self.settings.qwen_timeout_seconds) as client:
            response = await client.post(url, headers=headers, json=payload)
            response.raise_for_status()
            body = response.json()
        answer = body.get("choices", [{}])[0].get("message", {}).get("content", "")
        if not answer:
            raise RuntimeError("Qwen response content is empty")
        usage = body.get("usage") or {}
        return answer, usage

    async def json_chat(self, messages: list[Message], model: str | None = None, parameters: dict[str, Any] | None = None) -> tuple[dict[str, Any], dict[str, Any]]:
        answer, usage = await self.chat(messages, model, parameters)
        text = answer.strip()
        if text.startswith("```"):
            text = text.strip("`")
            if text.lower().startswith("json"):
                text = text[4:].strip()
        return json.loads(text), usage

    async def embed(self, texts: list[str], model: str | None = None) -> tuple[list[list[float]], dict[str, Any]]:
        if not texts:
            return [], {}
        if not self.settings.qwen_api_key:
            raise RuntimeError("QWEN_API_KEY is not configured")
        payload: dict[str, Any] = {
            "model": model or self.settings.qwen_embedding_model,
            "input": texts,
        }
        if self.settings.qwen_embedding_dimensions > 0:
            payload["dimensions"] = self.settings.qwen_embedding_dimensions
        payload["encoding_format"] = "float"
        url = self.settings.qwen_base_url.rstrip("/") + "/embeddings"
        headers = {
            "Authorization": f"Bearer {self.settings.qwen_api_key}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        }
        async with httpx.AsyncClient(timeout=self.settings.qwen_timeout_seconds) as client:
            response = await client.post(url, headers=headers, json=payload)
            response.raise_for_status()
            body = response.json()
        vectors = [
            item["embedding"]
            for item in sorted(body.get("data", []), key=lambda item: item.get("index", 0))
        ]
        return vectors, body.get("usage") or {}

    async def rerank(self, query: str, documents: list[str], top_n: int) -> tuple[list[dict[str, Any]], dict[str, Any]]:
        if not documents:
            return [], {}
        if not self.settings.qwen_api_key:
            raise RuntimeError("QWEN_API_KEY is not configured")
        url_base = (self.settings.qwen_rerank_base_url or self.settings.qwen_base_url).rstrip("/")
        payload = self._build_rerank_payload(query, documents, top_n)
        url = url_base if "/services/rerank/" in url_base else url_base + "/rerank"
        headers = {
            "Authorization": f"Bearer {self.settings.qwen_api_key}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        }
        async with httpx.AsyncClient(timeout=self.settings.qwen_timeout_seconds) as client:
            response = await client.post(url, headers=headers, json=payload)
            response.raise_for_status()
            body = response.json()
        output = body.get("output") or {}
        results = output.get("results") or body.get("results") or []
        return results, body.get("usage") or {}

    def _build_rerank_payload(self, query: str, documents: list[str], top_n: int) -> dict[str, Any]:
        model = self.settings.qwen_rerank_model
        api_style = self.settings.qwen_rerank_api_style.upper()
        if api_style == "QWEN3" or (api_style == "AUTO" and model == "qwen3-rerank"):
            return {
                "model": model,
                "query": query,
                "documents": documents,
                "top_n": top_n,
                "return_documents": False,
                "instruction": self.settings.qwen_rerank_instruct,
            }
        return {
            "model": model,
            "input": {
                "query": query,
                "documents": documents,
            },
            "parameters": {
                "return_documents": False,
                "top_n": top_n,
                "instruction": self.settings.qwen_rerank_instruct,
            },
        }
