from scheduler.backup_scheduler import BackupScheduler
from services.telegram_service import TelegramService
from config.bot_setting import BotSettings
from telegram.ext import ApplicationBuilder, CommandHandler

from commands.health_command_handler import HealthCommandHandler
from commands.backup_command_handler import BackupCommandHandler

class TelegramBot:

    def __init__(self):
        bot_setting = BotSettings()
        self.app = ApplicationBuilder().token(bot_setting.bot_token).build()
        self.telegram_service = TelegramService(self.app.bot)

    def register_handlers(self):
        self.app.add_handler(CommandHandler("health", HealthCommandHandler().execute))
        self.app.add_handler(CommandHandler("backup", BackupCommandHandler().handle))

    def register_scheduler(self):
        backup_scheduler = BackupScheduler(self.telegram_service)
        backup_scheduler.register(self.app)

    def run(self):
        self.register_handlers()
        self.register_scheduler()
        print("Telegram Bot is running...")
        self.app.run_polling()


if __name__ == "__main__":
    TelegramBot().run()
