# Messenger - Микросервис для мессенджера

Микросервис для обмена сообщениями в приложении Pitomets.

## Технологии

- Kotlin 2.0.21
- Java 21
- Ktor 2.3.12
- PostgreSQL
- Exposed ORM
- WebSocket для real-time сообщений

## Структура

- `models/` - Модели данных (Messages, Chats)
- `dto/` - DTO для запросов и ответов
- `service/` - Бизнес-логика
- `routing/` - API endpoints и WebSocket
- `db/` - Подключение к базе данных

## Запуск

1. Запустить PostgreSQL:
```bash
docker-compose up -d
```

2. Запустить приложение:
```bash
./gradlew run
```

Приложение будет доступно на `http://localhost:8081`

## API

### Chats

- `POST /api/chats` - Создать или получить чат
- `GET /api/chats` - Получить все чаты пользователя
- `GET /api/chats/{chatId}` - Получить чат по ID

### Messages

- `POST /api/messages` - Создать сообщение
- `GET /api/messages/chat/{chatId}` - Получить сообщения чата
- `PUT /api/messages/chat/{chatId}/read` - Отметить сообщения как прочитанные

### WebSocket

- `WS /ws/chat` - WebSocket для real-time сообщений

Все запросы требуют заголовок `X-User-Id` с ID пользователя из монолита.

## Переменные окружения

- `DATABASE_URL` - URL базы данных (по умолчанию: `jdbc:postgresql://localhost:5432/messenger`)
- `DATABASE_USER` - Пользователь БД (по умолчанию: `user`)
- `DATABASE_PASSWORD` - Пароль БД (по умолчанию: `password`)
