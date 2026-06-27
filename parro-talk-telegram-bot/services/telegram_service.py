import os
from pathlib import Path
from telegram import Bot

class TelegramService:
    def __init__(self, bot: Bot):
        self.bot = bot
        self.admin_id = os.getenv("TELEGRAM_ADMIN_ID")

    async def upload_file(self, file: Path):
        try:
            with open(file, "rb") as f:
                await self.bot.send_document(
                    chat_id=self.admin_id,
                    document=f,
                    filename=file.name
                )
            return True
        except Exception as e:
            print(f"Upload file failed: {e}")
            return False
