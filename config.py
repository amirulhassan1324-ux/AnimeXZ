"""
Koyeb Environment Variables config.

Koyeb me: Service -> Settings -> Environment variables me ye add karo:
    MONGODB_URL  = mongodb+srv://USER:PASS@cluster.mongodb.net/animexz   (required)
    ADMIN_KEY    = koi-bhi-secret-password                               (required)
    PUBLIC_URL   = https://your-app-name.koyeb.app   (Koyeb FQDN, optional)
    PORT         = 8000   (Koyeb khud set karta hai, optional)

Local test ke liye .env file bhi chalegi (python-dotenv install ho to).
"""
import os

try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass


def _get(name: str, default: str = "", required: bool = False) -> str:
    value = os.getenv(name, default).strip()
    if required and not value:
        raise RuntimeError(f"Environment variable '{name}' missing. Koyeb me add karo.")
    return value


class Config:
    # MongoDB (photo/link/metadata ke liye)
    MONGODB_URL: str = _get("MONGODB_URL", required=True)
    DB_NAME: str = _get("DB_NAME", "animexz")

    # Upload / admin endpoints ki security
    ADMIN_KEY: str = _get("ADMIN_KEY", required=True)

    # Koyeb FQDN, bina last slash ke (file links isi se bante hain)
    PUBLIC_URL: str = _get("PUBLIC_URL").rstrip("/")

    # Server port (Koyeb default 8000)
    PORT: int = int(_get("PORT", "8000"))

    # Server ke apne anime IDs is number se shuru hote hain (app ke ServerConfig se match)
    CUSTOM_ID_START: int = 1_000_000


config = Config()
