import os

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_PORT = os.environ.get("DB_PORT", "5432")
DB_NAME = os.environ.get("DB_NAME", "aims")
DB_USER = os.environ.get("DB_USER", "aims")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "aims")
DATABASE_DSN = (
    f"host={DB_HOST} port={DB_PORT} dbname={DB_NAME} "
    f"user={DB_USER} password={DB_PASSWORD}"
)

LOG_FILE_PATH = os.environ.get(
    "LOG_FILE_PATH", "../aims-demo/logs/aims-demo-access.log"
)
OFFSET_FILE_PATH = os.environ.get("OFFSET_FILE_PATH", ".collector_offset")
DEAD_LETTER_FILE_PATH = os.environ.get("DEAD_LETTER_FILE_PATH", "dead_letter.log")
POLL_INTERVAL_SECONDS = float(os.environ.get("POLL_INTERVAL_SECONDS", "2"))
