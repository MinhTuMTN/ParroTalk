import os
from dotenv import load_dotenv

load_dotenv()

class BotSettings:

    def __init__(self):
        self.bot_token = os.getenv("BOT_TOKEN")
