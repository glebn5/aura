import os
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    # База данных (по умолчанию PostgreSQL в докере, с возможностью использовать SQLite для локальных тестов)
    DATABASE_URL: str = "postgresql+asyncpg://postgres:postgres@db:5432/auratracker"
    
    # Ключ API Gemini (получается в Google AI Studio)
    GEMINI_API_KEY: str = ""
    
    # Настройки безопасности JWT
    JWT_SECRET: str = "super-secret-key-change-me-in-production-12345"
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 30  # 30 дней для авторизации на мобильном
    
    # Google OAuth Client ID (для авторизации)
    GOOGLE_CLIENT_ID: str = ""
    
    # Разрешить запуск без Google Auth (для простого локального тестирования/разработки)
    BYPASS_AUTH: bool = True

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

settings = Settings()
