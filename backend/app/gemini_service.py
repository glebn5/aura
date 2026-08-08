import json
from google import genai
from google.genai import types
from google.genai.errors import APIError
from app.config import settings
from app.schemas import GeminiStructuredLog, FinanceData, FitnessData, CarMaintenanceData, RoutineData, AIDashboardConfig

# Инициализируем клиент Gemini, если передан ключ
client = None
if settings.GEMINI_API_KEY:
    client = genai.Client(api_key=settings.GEMINI_API_KEY)

# Системные инструкции для разбора текста
SYSTEM_INSTRUCTION = """
Вы — интеллектуальный ассистент приложения AuraTracker. Ваша задача — проанализировать входящую текстовую запись пользователя на русском или английском языке, классифицировать её в одну из категорий (FINANCE, FITNESS, CAR_MAINTENANCE, ROUTINE, OTHER) и извлечь структурированные параметры согласно предоставленной схеме JSON.

Правила классификации:
- FINANCE: Расходы, доходы, покупки. Пример: "купил молоко за 100р", "получил зарплату 50к".
- FITNESS: Спортивные тренировки. Пример: "пробежал 5 км за 30 мин", "сходил на силовую тренировку 1 час".
- CAR_MAINTENANCE: Ремонт авто, покупка автозапчастей, заправка бензином. Пример: "заменил масло за 3000 рублей", "купил колодки на озоне за 1500р".
- ROUTINE: Рутинные повседневные дела (сон, чтение книг, учеба, работа). Пример: "спал 8 часов", "учился 3 часа".
- OTHER: Любые другие записи, не подходящие под категории.

Будьте точны. Извлекайте числа корректно. Валюту по умолчанию ставьте RUB.
"""

def parse_text_mock(text: str) -> GeminiStructuredLog:
    """
    Мок-парсер для локального тестирования без рабочего ключа Gemini API.
    Анализирует ключевые слова и возвращает структурированный ответ.
    """
    text_lower = text.lower()
    
    # 1. FINANCE
    if any(k in text_lower for k in ["руб", "рублей", "коп", "потратил", "купил", "оплатил", "цена", "стоило", "$", "eur", "usd"]):
        # Попробуем вытащить число
        amount = 100.0
        for word in text_lower.split():
            cleaned = "".join(c for c in word if c.isdigit() or c == ".")
            if cleaned:
                try:
                    amount = float(cleaned)
                    break
                except ValueError:
                    pass
        
        item = "Покупка"
        if "кофе" in text_lower:
            item = "Кофе"
        elif "еда" in text_lower or "продукты" in text_lower:
            item = "Продукты"
            
        return GeminiStructuredLog(
            category="FINANCE",
            finance_data=FinanceData(
                amount=amount,
                currency="RUB" if "$" not in text_lower else "USD",
                item=item,
                category="food" if "кофе" in text_lower or "продукты" in text_lower else "shopping"
            )
        )
        
    # 2. CAR_MAINTENANCE
    elif any(k in text_lower for k in ["машин", "авто", "запчаст", "масло", "шины", "бензин", "заправка", "озон"]):
        cost = 1000.0
        for word in text_lower.split():
            cleaned = "".join(c for c in word if c.isdigit() or c == ".")
            if cleaned:
                try:
                    cost = float(cleaned)
                    break
                except ValueError:
                    pass
        return GeminiStructuredLog(
            category="CAR_MAINTENANCE",
            car_data=CarMaintenanceData(
                part_or_service="Обслуживание автомобиля/запчасти",
                cost=cost,
                currency="RUB"
            )
        )

    # 3. FITNESS
    elif any(k in text_lower for k in [
        "пробежал", "бег", "км", "тренировк", "бассейн", "зал", "спорт", "минут", "минуты",
        "отжался", "отжиманий", "отжимания", "присел", "приседаний", "подтянулся", "подтягиваний",
        "пресс", "жим", "раз", "подходов", "гантели", "штанга", "брусья", "планка"
    ]):
        distance = None
        if "км" in text_lower:
            parts = text_lower.split("км")
            if parts:
                words = parts[0].strip().split()
                if words:
                    try:
                        distance = float(words[-1].replace(",", "."))
                    except ValueError:
                        pass
        
        duration = None
        if "мин" in text_lower:
            parts = text_lower.split("мин")
            if parts:
                words = parts[0].strip().split()
                if words:
                    try:
                        duration = float(words[-1].replace(",", "."))
                    except ValueError:
                        pass

        activity = "gym"
        if "пробежал" in text_lower or "бег" in text_lower:
            activity = "running"
        elif "бассейн" in text_lower or "плавал" in text_lower:
            activity = "swimming"
        elif "отжался" in text_lower or "отжиманий" in text_lower:
            activity = "pushups"
        elif "подтянулся" in text_lower or "подтягиваний" in text_lower:
            activity = "pullups"
        elif "присел" in text_lower or "приседаний" in text_lower:
            activity = "squats"

        reps = None
        for word in text_lower.split():
            cleaned = "".join(c for c in word if c.isdigit())
            if cleaned:
                try:
                    reps = int(cleaned)
                    break
                except ValueError:
                    pass

        return GeminiStructuredLog(
            category="FITNESS",
            fitness_data=FitnessData(
                activity_type=activity,
                distance_km=distance,
                duration_minutes=duration,
                intensity_level="high" if any(k in text_lower for k in ["50", "100", "тяжелая", "интенсив"]) else "medium",
                reps=reps
            )
        )
        
    # 4. ROUTINE
    elif any(k in text_lower for k in ["спал", "сон", "учеба", "учился", "читал", "книг", "час", "часов"]):
        duration = 8.0
        for word in text_lower.split():
            cleaned = "".join(c for c in word if c.isdigit() or c == ".")
            if cleaned:
                try:
                    duration = float(cleaned)
                    break
                except ValueError:
                    pass
        return GeminiStructuredLog(
            category="ROUTINE",
            routine_data=RoutineData(
                activity="Сон/Рутина" if "спал" in text_lower else "Работа/Учеба",
                duration_hours=duration
            )
        )
        
    # 5. OTHER
    else:
        return GeminiStructuredLog(
            category="OTHER",
            other_summary=text
        )

async def analyze_log_text(text: str) -> GeminiStructuredLog:
    """
    Основная функция разбора текста с помощью Gemini API.
    Если API-ключ не задан, автоматически переключается на мок-анализатор.
    """
    if not client:
        # Режим заглушки
        return parse_text_mock(text)
        
    try:
        # Отправляем запрос к Gemini 2.0 Flash
        response = client.models.generate_content(
            model="gemini-2.0-flash",
            contents=text,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=GeminiStructuredLog,
                system_instruction=SYSTEM_INSTRUCTION,
                temperature=0.1
            )
        )
        
        # Если SDK уже распарсил ответ в Pydantic модель
        if hasattr(response, "parsed") and isinstance(response.parsed, GeminiStructuredLog):
            return response.parsed

        # Иначе парсим из текста, удаляя возможные markdown-оболочки
        if response.text:
            raw_json = response.text.strip()
            if raw_json.startswith("```"):
                raw_json = raw_json.split("\n", 1)[-1].rsplit("```", 1)[0].strip()
            data = json.loads(raw_json)
            return GeminiStructuredLog(**data)

        return parse_text_mock(text)
        
    except APIError as e:
        print(f"Gemini API Error: {e}")
        # Если API выдал ошибку, откатываемся на мок, чтобы бэкенд не падал
        return parse_text_mock(text)
    except Exception as e:
        print(f"Unexpected error in Gemini service ({type(e).__name__}): {e}")
        return parse_text_mock(text)

async def generate_dashboard_config(prompt: str) -> AIDashboardConfig:
    """
    Генерирует уникальную конфигурацию виджета/дашборда с помощью Gemini AI по текстовому описанию пользователя.
    """
    if not client:
        return AIDashboardConfig(
            title_ru=prompt.capitalize(),
            title_en=prompt.capitalize(),
            category_filter="ALL",
            sub_category_filter=None,
            time_range_days=30,
            accent_color_hex="#00E5FF",
            icon_name="chart"
        )
        
    try:
        response = client.models.generate_content(
            model="gemini-2.0-flash",
            contents=f"Сгенерируй конфигурацию дашборда/виджета по запросу пользователя: '{prompt}'",
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=AIDashboardConfig,
                system_instruction="Вы — ИИ-дизайнер виджетов для AuraTracker. Проанализируйте запрос пользователя и верните полную схему виджета с фильтрами, подкатегориями, периодом и неоновым цветом accent_color_hex.",
                temperature=0.2
            )
        )
        if hasattr(response, "parsed") and isinstance(response.parsed, AIDashboardConfig):
            return response.parsed
        if response.text:
            raw_json = response.text.strip()
            if raw_json.startswith("```"):
                raw_json = raw_json.split("\n", 1)[-1].rsplit("```", 1)[0].strip()
            data = json.loads(raw_json)
            return AIDashboardConfig(**data)
            
        return AIDashboardConfig(
            title_ru=prompt.capitalize(),
            title_en=prompt.capitalize(),
            category_filter="ALL",
            sub_category_filter=None,
            time_range_days=30,
            accent_color_hex="#00E5FF",
            icon_name="chart"
        )
    except Exception as e:
        print(f"Error generating dashboard config via Gemini: {e}")
        return AIDashboardConfig(
            title_ru=prompt.capitalize(),
            title_en=prompt.capitalize(),
            category_filter="ALL",
            sub_category_filter=None,
            time_range_days=30,
            accent_color_hex="#00E5FF",
            icon_name="chart"
        )
