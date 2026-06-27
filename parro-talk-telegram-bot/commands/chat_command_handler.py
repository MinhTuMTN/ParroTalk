from telegram import Sticker
from telegram.ext._contexttypes import ContextTypes
from telegram import Update


class ChatCommandHandler:

    def __init__(self):
        pass

    async def execute(self, update: Update, context: ContextTypes.DEFAULT_TYPE):
        await update.message.reply_sticker(
            sticker="CAACAgIAAxkBAAFNmk5qP77zw0Wg7f315CMWU_32aBeOKwACaBMAAv988Ehma2nfVi-OWTwE"
        )
