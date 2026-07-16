import uuid
from datetime import datetime, timezone
from sqlalchemy import ForeignKey, String, DateTime, JSON
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.database import Base

class User(Base):
    __tablename__ = "users"
    
    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True)
    telegram_id: Mapped[str | None] = mapped_column(String(100), unique=True, index=True, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), 
        default=lambda: datetime.now(timezone.utc)
    )
    
    log_entries: Mapped[list["LogEntry"]] = relationship(
        back_populates="user", 
        cascade="all, delete-orphan",
        lazy="selectin"
    )

class LogEntry(Base):
    __tablename__ = "log_entries"
    
    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    raw_text: Mapped[str] = mapped_column(String(1000))
    category: Mapped[str] = mapped_column(String(50), index=True) # e.g. FINANCE, FITNESS, etc.
    structured_data: Mapped[dict] = mapped_column(JSON) # JSON/JSONB field
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), 
        default=lambda: datetime.now(timezone.utc),
        index=True
    )
    
    user: Mapped[User] = relationship(back_populates="log_entries")
