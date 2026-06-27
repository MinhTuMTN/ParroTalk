from datetime import datetime


class HealthMessageFormatter:

    @staticmethod
    def format(health) -> str:

        api_status = "✅ Online" if health.api_status else "❌ Offline"

        message = (
            "🤖 <b>Hello!</b>\n\n"
            f"🕘 <b>Time</b>\n"
            f"{datetime.now().strftime('%d/%m/%Y %H:%M:%S')}\n\n"

            "🩺 <b>Health Check</b>\n"
            f"├ 🌐 API Server : {api_status}\n"
            f"├ 🖥 CPU        : {health.cpu_percent:.1f}%\n"
            f"├ 💾 RAM        : {health.ram_used_gb:.1f}/{health.ram_total_gb:.1f} GB\n"
            f"└ 📦 Storage    : {health.disk_used_gb:.1f}/{health.disk_total_gb:.1f} GB\n"
        )

        if health.api_status:
            message += "\n👍 Everything looks good."
        else:
            message += "\n⚠️ API server is not responding."

        return message