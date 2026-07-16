import jwt
import urllib.request
import json
import hashlib
import hmac
from datetime import datetime, timedelta, timezone
from typing import Optional
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.config import settings
from app.database import get_db
from app.models import User

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="api/v1/auth/token", auto_error=False)

def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    """Генерация JWT access-токена."""
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:
        expire = datetime.now(timezone.utc) + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, settings.JWT_SECRET, algorithm=settings.JWT_ALGORITHM)
    return encoded_jwt

async def verify_google_token(id_token: str) -> dict:
    """
    Проверка Google ID токена через публичный Google API endpoint.
    Возвращает словарь с данными пользователя.
    """
    if settings.BYPASS_AUTH:
        # Для локальных тестов возвращаем моковые данные
        return {
            "email": "testuser@example.com",
            "name": "Test User",
            "sub": "mock-google-id-12345"
        }
        
    try:
        url = f"https://oauth2.googleapis.com/tokeninfo?id_token={id_token}"
        req = urllib.request.Request(url)
        with urllib.request.urlopen(req) as response:
            payload = json.loads(response.read().decode())
            
            # Проверяем, что токен выпущен для нашего Client ID (если он задан)
            if settings.GOOGLE_CLIENT_ID and payload.get("aud") != settings.GOOGLE_CLIENT_ID:
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Невалидный Google Client ID (aud)"
                )
            return payload
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"Ошибка валидации Google токена: {str(e)}"
        )

def verify_telegram_auth(auth_data: dict, bot_token: str) -> bool:
    """
    Плейсхолдер для проверки подлинности авторизации Telegram.
    Использует HMAC-SHA256 с токеном бота.
    """
    if settings.BYPASS_AUTH:
        return True
        
    # https://core.telegram.org/widgets/login#checking-authorization
    check_hash = auth_data.get("hash")
    if not check_hash:
        return False
        
    # Собираем строку из всех полей кроме hash в алфавитном порядке
    data_check_list = []
    for key, value in sorted(auth_data.items()):
        if key != "hash" and value is not None:
            data_check_list.append(f"{key}={value}")
    data_check_string = "\n".join(data_check_list)
    
    # Считаем SHA256 от токена бота
    secret_key = hashlib.sha256(bot_token.encode()).digest()
    
    # Считаем HMAC-SHA256
    calculated_hash = hmac.new(secret_key, data_check_string.encode(), hashlib.sha256).hexdigest()
    
    return calculated_hash == check_hash

async def get_current_user(
    token: str = Depends(oauth2_scheme), 
    db: AsyncSession = Depends(get_db)
) -> User:
    """Зависимость (Dependency) для извлечения текущего пользователя из JWT токена."""
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Не удалось валидировать учетные данные",
        headers={"WWW-Authenticate": "Bearer"},
    )
    
    # Если токен не передан, а байпас включен, создаем/возвращаем дефолтного пользователя
    if not token and settings.BYPASS_AUTH:
        result = await db.execute(select(User).filter(User.email == "testuser@example.com"))
        user = result.scalars().first()
        if not user:
            user = User(email="testuser@example.com")
            db.add(user)
            await db.flush()
            await db.commit()
        return user

    if not token:
        raise credentials_exception

    try:
        payload = jwt.decode(token, settings.JWT_SECRET, algorithms=[settings.JWT_ALGORITHM])
        email: str = payload.get("sub")
        if email is None:
            raise credentials_exception
    except jwt.PyJWTError:
        raise credentials_exception
        
    result = await db.execute(select(User).filter(User.email == email))
    user = result.scalars().first()
    if user is None:
        raise credentials_exception
    return user
