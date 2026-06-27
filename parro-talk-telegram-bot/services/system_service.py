import psutil

class SystemService:

    def get_cpu_percent(self) -> float:
        return psutil.cpu_percent(interval=1)

    def get_memory(self):
        return psutil.virtual_memory()

    def get_disk(self):
        return psutil.disk_usage("/")
