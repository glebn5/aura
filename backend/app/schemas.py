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

class EmailAuthRequest(BaseModel):
    email: str

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
    activity_type: str = Field(..., description="Тип активности (например, running, swimming, pushups, pullups, squats)")
    distance_km: Optional[float] = Field(None, description="Дистанция в километрах (если применимо)")
    duration_minutes: Optional[float] = Field(None, description="Длительность активности в минутах")
    intensity_level: Optional[Literal["low", "medium", "high"]] = Field(None, description="Уровень интенсивности")
    reps: Optional[int] = Field(None, description="Количество повторений (например, 50 для отжиманий)")

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

class AIDashboardConfig(BaseModel):
    title_ru: str = Field(..., description="Название виджета на русском")
    title_en: str = Field(..., description="Название виджета на английском")
    category_filter: Literal["FINANCE", "FITNESS", "CAR_MAINTENANCE", "ROUTINE", "ALL"] = Field(..., description="Главная категория")
    sub_category_filter: Optional[str] = Field(None, description="Ключевое слово для подкатегории (например: pushups, coffee, running, fuel, groceries)")
    time_range_days: int = Field(30, description="Период отслеживания в днях (7, 14, 30, 90, 365)")
    accent_color_hex: str = Field("#00E5FF", description="НЕОНОВЫЙ HEX цвет в дизайне приложения (например: #00E5FF, #AEEA00, #FF9100, #E040FB)")
    icon_name: str = Field("chart", description="Иконка: fitness, finance, car, chart")

class AIDashboardRequest(BaseModel):
    prompt: str = Field(..., description="Запрос пользователя для создания виджета в свободной форме")

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
