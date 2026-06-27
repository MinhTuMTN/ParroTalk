from dataclasses import dataclass

@dataclass
class HealthInfo:
    api_status: bool
    cpu_percent: float
    ram_used_gb: float
    ram_total_gb: float
    disk_used_gb: float
    disk_total_gb: float
