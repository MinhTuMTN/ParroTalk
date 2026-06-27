import requests
import os

class ApiService:

    def __init__(self):
        self.base_url = os.getenv("API_BASE_URL")

    def health_check(self) -> bool:
        try:
            response = requests.get(f"{self.base_url}/health", timeout=3)
            return response.status_code == 200 and response.text == "OK"
        except requests.exceptions.RequestException:
            return False