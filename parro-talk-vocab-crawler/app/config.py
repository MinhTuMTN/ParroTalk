import os
from pathlib import Path
from dotenv import load_dotenv

# Load local directory .env first, then fallback to project root .env
_local_env = Path(__file__).resolve().parent.parent / ".env"
if _local_env.exists():
    load_dotenv(_local_env)
else:
    load_dotenv()

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "admin")
RABBITMQ_PASS = os.getenv("RABBITMQ_PASS", "MinhTu@3005")

VOCAB_CRAWL_QUEUE = os.getenv("VOCAB_CRAWL_QUEUE", "vocab-crawl-queue")
VOCAB_CRAWL_RESULT_QUEUE = os.getenv("VOCAB_CRAWL_RESULT_QUEUE", "vocab-crawl-result-queue")
VOCAB_CRAWL_DLQ = os.getenv("VOCAB_CRAWL_DLQ", "vocab-crawl-dlq")

MW_THESAURUS_API_KEY = (
    os.getenv("MW_THESAURUS_API_KEY")
    or os.getenv("MERRIAM_WEBSTER_THESAURUS_API_KEY")
    or ""
).strip()
MW_DICTIONARY_API_KEY = (
    os.getenv("MW_DICTIONARY_API_KEY")
    or os.getenv("MERRIAM_WEBSTER_DICTIONARY_API_KEY")
    or ""
).strip()

DATAMUSE_API_URL = os.getenv("DATAMUSE_API_URL", "https://api.datamuse.com/words").strip()
DATAMUSE_TIMEOUT_SECONDS = float(os.getenv("DATAMUSE_TIMEOUT_SECONDS", "3.0"))

HTTP_TIMEOUT_SECONDS = float(os.getenv("HTTP_TIMEOUT_SECONDS", "12.0"))
PORT = int(os.getenv("PORT", "8002"))

