from __future__ import annotations

import os
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uvicorn

from app.config import PORT
from app.crawler_service import CrawlService
from app.worker import launch_worker_daemon


class CrawlRequest(BaseModel):
    word: str


crawl_service = CrawlService()


@asynccontextmanager
async def lifespan(app: FastAPI):
    if os.getenv("DISABLE_RABBITMQ_WORKER", "false").lower() != "true":
        launch_worker_daemon()
    yield


app = FastAPI(
    title="ParroTalk Vocabulary Crawler Service",
    version="1.0.0",
    lifespan=lifespan,
)


@app.get("/health")
def health_check():
    return {"status": "ok", "service": "parro-talk-vocab-crawler"}


@app.post("/api/v1/crawl")
def crawl_word_endpoint(req: CrawlRequest):
    word = (req.word or "").strip()
    if not word:
        raise HTTPException(status_code=400, detail="word must not be blank")
    result = crawl_service.crawl_word(word)
    return result.to_message_dict()


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=PORT, reload=False)

