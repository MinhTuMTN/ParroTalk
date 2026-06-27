from telegram import Update
from telegram.ext import ContextTypes

from services.health_service import HealthService
from utils.health_message_formmater import HealthMessageFormatter


class HealthCommandHandler:

    def __init__(self):
        self.health_service = HealthService()

    async def execute(self, update: Update, context: ContextTypes.DEFAULT_TYPE):
        processing_message = await update.message.reply_text("⏳ Checking server health...")
        health = self.health_service.health_check()
        message = HealthMessageFormatter.format(health)
        await processing_message.edit_text(message, parse_mode="HTML")
