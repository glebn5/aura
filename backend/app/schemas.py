import uuid
from datetime import datetime
from typing import Optional, Literal
from pydantic import BaseModel, Field

# ==========================================
# 1. Схемы авторизации и пользователей
# ==========================================

class UserBase(BaseModel):
    email: str

class UserCreate(UserBase):
    telegram_id: Optional[str] = None

class UserResponse(UserBase):
    id: uuid.UUID
    telegram_id: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True

class TokenResponse(BaseModel):
    access_token: str
    token_type: str
    user: UserResponse

class GoogleAuthRequest(BaseModel):
    id_token: str

class TelegramAuthRequest(BaseModel):
    id: str
    first_name: Optional[str] = None
    username: Optional[str] = None
    auth_date: int
    hash: str

# ==========================================
# 2. Схемы структурированных данных для ИИ (Gemini API)
# ==========================================

class FinanceData(BaseModel):
    amount: float = Field(..., description="Сумма траты/дохода в числовом формате (например, 1000.0)")
    currency: str = Field("RUB", description="Валюта транзакции, по умолчанию RUB (например, RUB, USD, EUR)")
    item: str = Field(..., description="На что потрачено или за что получено (например, кофе, автозапчасти, бензин, зарплата)")
    category: str = Field(..., description="Категория финансов (например, продукты, транспорт, автосервис, кафе, развлечения)")

class FitnessData(BaseModel):
    activity_type: str = Field(..., description="Тип активности (например, running, swimming, strength_training)")
    distance_km: Optional[float] = Field(None, description="Дистанция в километрах (если применимо)")
    duration_minutes: Optional[float] = Field(None, description="Длительность активности в минутах")
    intensity_level: Optional[Literal["low", "medium", "high"]] = Field(None, description="Уровень интенсивности")

class CarMaintenanceData(BaseModel):
    part_or_service: str = Field(..., description="Название работы или запчасти (например, замена масла, тормозные диски, шиномонтаж)")
    cost: Optional[float] = Field(None, description="Стоимость работы/запчасти в числовом формате")
    currency: str = Field("RUB", description="Валюта")

class RoutineData(BaseModel):
    activity: str = Field(..., description="Описание рутинного действия (например, сон, чтение книги, учеба)")
    duration_hours: Optional[float] = Field(None, description="Длительность в часах")

class GeminiStructuredLog(BaseModel):
    category: Literal["FINANCE", "FITNESS", "CAR_MAINTENANCE", "ROUTINE", "OTHER"] = Field(
        ..., 
        description="Категория, к которой относится запись"
    )
    finance_data: Optional[FinanceData] = Field(None, description="Данные для финансов (заполняется только при category=FINANCE)")
    fitness_data: Optional[FitnessData] = Field(None, description="Данные для фитнеса (заполняется только при category=FITNESS)")
    car_data: Optional[CarMaintenanceData] = Field(None, description="Данные для обслуживания автомобиля (заполняется только при category=CAR_MAINTENANCE)")
    routine_data: Optional[RoutineData] = Field(None, description="Данные для повседневных дел (заполняется только при category=ROUTINE)")
    other_summary: Optional[str] = Field(
        None, 
        description="Краткая выжимка или комментарий (заполняется только при category=OTHER или если данные не попали в другие секции)"
    )

# ==========================================
# 3. Схемы лог-записей API
# ==========================================

class LogProcessRequest(BaseModel):
    raw_text: str = Field(..., max_length=1000)

class LogEntryResponse(BaseModel):
    id: uuid.UUID
    user_id: uuid.UUID
    raw_text: str
    category: str
    structured_data: GeminiStructuredLog
    created_at: datetime

    class Config:
        from_attributes = True
