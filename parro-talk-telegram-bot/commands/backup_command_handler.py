import os

from telegram import Update
from telegram.ext import ContextTypes

from services.backup_service import BackupService


class BackupCommandHandler:

    def __init__(self):
        self.backup_service = BackupService()
        self.admin_id = int(os.getenv("TELEGRAM_ADMIN_ID"))

    async def handle(self,
                     update: Update,
                     context: ContextTypes.DEFAULT_TYPE):

        if update.effective_user.id != self.admin_id:
            return

        message = await update.message.reply_text(
            "⏳ Creating backup..."
        )

        backup_file = None

        try:

            backup_file = self.backup_service.create_backup()

            await message.edit_text(
                "⬆ Uploading backup..."
            )

            with open(backup_file, "rb") as file:

                await update.message.reply_document(
                    document=file,
                    filename=backup_file.name
                )

            await message.edit_text(
                "✅ Backup completed."
            )

        except Exception as ex:

            await message.edit_text(
                f"❌ Backup failed.\n{ex}"
            )

        finally:

            if backup_file:
                self.backup_service.delete_backup(
                    backup_file
                )