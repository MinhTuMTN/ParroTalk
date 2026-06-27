from config.bot_setting import BotSettings
from telegram.ext import ApplicationBuilder, CommandHandler

from commands.health import HealthCommandHandler

class TelegramBot:

    def __init__(self):
        bot_setting = BotSettings()
        self.app = ApplicationBuilder().token(bot_setting.bot_token).build()

    def register_handlers(self):
        self.app.add_handler(CommandHandler("health", HealthCommandHandler().execute))

    def run(self):
        self.register_handlers()
        print("Telegram Bot is running...")
        self.app.run_polling()


if __name__ == "__main__":
    TelegramBot().run()
