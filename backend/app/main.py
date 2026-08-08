from contextlib import asynccontextmanager
from typing import List
from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.config import settings
from app.database import engine, Base, get_db
from app.models import User, LogEntry
from app.schemas import (
    LogProcessRequest, 
    LogEntryResponse, 
    GoogleAuthRequest, 
    EmailAuthRequest,
    TelegramAuthRequest, 
    TokenResponse, 
    UserResponse,
    AIDashboardRequest,
    AIDashboardConfig
)
from app.auth import (
    verify_google_token, 
    verify_telegram_auth, 
    create_access_token, 
    get_current_user
)
from app.gemini_service import analyze_log_text, generate_dashboard_config

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Автоматическое создание таблиц при старте сервера
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield

app = FastAPI(
    title="AuraTracker API",
    description="Backend server for parsing natural language log entries with Gemini ИИ",
    version="1.0.0",
    lifespan=lifespan
)

# Настройка CORS (Cross-Origin Resource Sharing)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], # Разрешаем все источники для мобильного приложения
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ==========================================
# Маршруты здоровья и проверки
# ==========================================
@app.get("/api/v1/health", status_code=status.HTTP_200_OK)
async def health_check():
    return {"status": "ok", "message": "AuraTracker Backend is active and running"}

# ==========================================
# Маршруты авторизации
# ==========================================
@app.post("/api/v1/auth/google", response_model=TokenResponse)
async def auth_google(req: GoogleAuthRequest, db: AsyncSession = Depends(get_db)):
    """Авторизация по Google ID Token."""
    google_data = await verify_google_token(req.id_token)
    email = google_data.get("email")
    if not email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Google Token не содержит email"
        )
        
    # Ищем пользователя в БД или создаем нового
    result = await db.execute(select(User).filter(User.email == email))
    user = result.scalars().first()
    
    if not user:
        user = User(email=email)
        db.add(user)
        await db.flush() # Получаем ID пользователя
        await db.commit()
    
    # Генерируем JWT токен приложения
    access_token = create_access_token(data={"sub": user.email})
    
    return TokenResponse(
        access_token=access_token,
        token_type="bearer",
        user=UserResponse.model_validate(user)
    )

@app.post("/api/v1/auth/email", response_model=TokenResponse)
async def auth_email(req: EmailAuthRequest, db: AsyncSession = Depends(get_db)):
    """Прямая авторизация по email пользователя."""
    email = req.email.strip().lower()
    if not email or "@" not in email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Некорректный email адрес"
        )
    
    result = await db.execute(select(User).filter(User.email == email))
    user = result.scalars().first()
    
    if not user:
        user = User(email=email)
        db.add(user)
        await db.flush()
        await db.commit()
        
    access_token = create_access_token(data={"sub": user.email})
    
    return TokenResponse(
        access_token=access_token,
        token_type="bearer",
        user=UserResponse.model_validate(user)
    )

@app.post("/api/v1/auth/telegram", response_model=TokenResponse)
async def auth_telegram(req: TelegramAuthRequest, db: AsyncSession = Depends(get_db)):
    """Авторизация по Telegram Auth Data."""
    # Плейсхолдер проверки подписи Telegram.
    # В продакшене требуется передать токен бота через config.py
    bot_token = "placeholder_bot_token"
    auth_data = req.model_dump()
    
    if not verify_telegram_auth(auth_data, bot_token):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Ошибка валидации хэша Telegram"
        )
        
    # Создаем виртуальный email на основе telegram_id, так как email обязателен
    tg_email = f"tg_{req.id}@telegram.auratracker.ru"
    
    result = await db.execute(select(User).filter(User.email == tg_email))
    user = result.scalars().first()
    
    if not user:
        user = User(email=tg_email, telegram_id=req.id)
        db.add(user)
        await db.flush()
        await db.commit()
        
    access_token = create_access_token(data={"sub": user.email})
    
    return TokenResponse(
        access_token=access_token,
        token_type="bearer",
        user=UserResponse.model_validate(user)
    )

# ==========================================
# Маршруты логов (CRUD & ИИ-обработка)
# ==========================================
@app.post("/api/v1/logs/process", response_model=LogEntryResponse)
async def process_log_entry(
    req: LogProcessRequest, 
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Основной эндпоинт приложения: получает текст от пользователя,
    отправляет его в Gemini, сохраняет в БД и возвращает структурированный ответ.
    """
    # 1. Анализируем текст с помощью Gemini API (или заглушки)
    structured_log = await analyze_log_text(req.raw_text)
    
    # 2. Сохраняем результат в БД
    db_entry = LogEntry(
        user_id=current_user.id,
        raw_text=req.raw_text,
        category=structured_log.category,
        structured_data=structured_log.model_dump()
    )
    
    db.add(db_entry)
    await db.commit()
    await db.refresh(db_entry)
    
    return db_entry

@app.get("/api/v1/logs", response_model=list[LogEntryResponse])
async def get_log_entries(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """Возвращает историю всех записей текущего пользователя (для синхронизации)."""
    result = await db.execute(
        select(LogEntry)
        .filter(LogEntry.user_id == current_user.id)
        .order_by(LogEntry.created_at.desc())
    )
    entries = result.scalars().all()
    return entries

@app.delete("/api/v1/logs/{log_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_log_entry(
    log_id: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """Удаление лог-записи."""
    import uuid
    try:
        uuid_id = uuid.UUID(log_id)
    except ValueError:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Неверный формат UUID")
        
    result = await db.execute(
        select(LogEntry)
        .filter(LogEntry.id == uuid_id, LogEntry.user_id == current_user.id)
    )
    entry = result.scalars().first()
    if not entry:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, 
            detail="Запись не найдена или принадлежит другому пользователю"
        )
        
    await db.delete(entry)
    await db.commit()
    return None

@app.post("/api/v1/dashboards/generate", response_model=AIDashboardConfig)
async def generate_ai_dashboard(req: AIDashboardRequest):
    """
    Генерирует умную конфигурацию виджета/дашборда с помощью Gemini AI по свободному промпту пользователя.
    """
    return await generate_dashboard_config(req.prompt)
