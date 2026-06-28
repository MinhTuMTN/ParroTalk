from telegram import Sticker
from telegram.ext._contexttypes import ContextTypes
from telegram import Update
import random

class ChatCommandHandler:

    def __init__(self):
        pass

    async def execute(self, update: Update, context: ContextTypes.DEFAULT_TYPE):
        await update.message.reply_sticker(
            sticker=self.choice_sticker()
        )

    def choice_sticker(self):
        STICKERS = [
            "CAACAgIAAxkBAAFNmk5qP77zw0Wg7f315CMWU_32aBeOKwACaBMAAv988Ehma2nfVi-OWTwE",
            "CAACAgIAAxkBAAFNpftqQIsrzWokeIWUVfhdt0Z2xAuXagACtBEAAjd48UgOzfKhnAHaQjwE",
            "CAACAgIAAxkBAAFNpf1qQItBrx7SFyIk76N_G5UDHd4SWwACExQAAq4ZyUgAAYcHiCR-PVY8BA",
            "CAACAgIAAxkBAAFNpf9qQItQAAH_wiSD1xSZILhuvVoQfTYAApkUAAJQAnBJlNI_gD9gn-E8BA",
            "CAACAgIAAxkBAAFNpgFqQItsi5Tn1ZDz4dQvIxLZcmvHiAACIxIAAjMeIEmz4aBlEao3zTwE",
            "CAACAgIAAxkBAAFNpgNqQIt8232Fb0aRuJRQUJD3uaXgIAACLRIAAtYqqUnldtHNZmxiiTwE",
            "CAACAgIAAxkBAAFNpgVqQIuDSQasclG79dxeA-ukRZS1rgACZxYAAkkH2UmJsFzQ9SO3DjwE"
        ]
        return random.choice(STICKERS)
