from datetime import timedelta
from datetime import time
from zoneinfo import ZoneInfo

from telegram.ext import Application

from services.backup_service import BackupService
from services.telegram_service import TelegramService


class BackupScheduler:

    def __init__(self, telegram_service: TelegramService):
        self.backup_service = BackupService()
        self.telegram_service = telegram_service

    def register(self, application: Application):
        application.job_queue.run_daily(
            callback=self.backup_job,
            time=time(
                hour=18,
                minute=56,
                tzinfo=ZoneInfo("Asia/Ho_Chi_Minh")
            ),
            name="daily_backup"
        )

    async def backup_job(self, context):
        backup_file = None

        try:
            backup_file = self.backup_service.create_backup()
            await self.telegram_service.upload_file(backup_file)
        except Exception as ex:
            print(f"Backup failed: {ex}")

        finally:
            if backup_file:
                self.backup_service.delete_backup(backup_file)