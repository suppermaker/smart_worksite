from fastapi import Header, HTTPException
from .settings import get_settings


async def verify_service_key(x_ai_service_key: str | None = Header(default=None)) -> None:
    expected = get_settings().ai_service_api_key
    if expected and x_ai_service_key != expected:
        raise HTTPException(status_code=401, detail="invalid ai service key")
