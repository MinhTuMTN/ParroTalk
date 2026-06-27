import os
import subprocess
from pathlib import Path
from datetime import datetime

class BackupService:

    def create_backup(self) -> Path:

        backup_file = Path(
            f"/tmp/parrotalk_{datetime.now():%Y%m%d_%H%M%S}.dump"
        )

        # set postgress password
        env = os.environ.copy()
        env["PGPASSWORD"] = os.getenv("POSTGRES_PASSWORD")

        # execute pg_dump
        subprocess.run(
            [
                "pg_dump",
                "-Fc",
                "-h", os.getenv("POSTGRES_HOST"),
                "-p", os.getenv("POSTGRES_PORT"),
                "-U", os.getenv("POSTGRES_USER"),
                "-d", os.getenv("POSTGRES_DB"),
                "-f", str(backup_file),
            ],
            env=env,
            check=True
        )

        return backup_file

    def delete_backup(self, file: Path):
        if file.exists():
            file.unlink()