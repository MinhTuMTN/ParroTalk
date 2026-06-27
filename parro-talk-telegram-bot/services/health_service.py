from models.health_info import HealthInfo
from services.system_service import SystemService
from services.api_service import ApiService

class HealthService:

    def __init__(self):
        self.api_service = ApiService()
        self.system_service = SystemService()

    def health_check(self) -> HealthInfo:
        memory = self.system_service.get_memory()
        disk = self.system_service.get_disk()

        return HealthInfo(
            api_status=self.api_service.health_check(),
            cpu_percent=self.system_service.get_cpu_percent(),
            ram_used_gb=memory.used / (1024 ** 3),
            ram_total_gb=memory.total / (1024 ** 3),
            disk_used_gb=disk.used / (1024 ** 3),
            disk_total_gb=disk.total / (1024 ** 3),
        )
