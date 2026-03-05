# Moderator Service

Сервис автоматической модерации текстов.

## Как работает

1. Читает события из Kafka-топика `moderation.requested`.
2. Собирает текст из `textParts`.
3. Вызывает внешнее API `POST /api/v1/analyze`.
4. Пишет результат в Kafka-топик `moderation.processed`.

## Входящее событие (`moderation.requested`)

```json
{
  "eventId": "5e7ecffe-c09a-4f7f-8a78-7ce25aafc831",
  "entityType": "LISTING",
  "entityId": 101,
  "operation": "CREATE",
  "textParts": ["Заголовок", "Описание"],
  "withAnimal": true
}
```

`entityType`: `LISTING | SELLER_PROFILE | REVIEW`.

## Исходящее событие (`moderation.processed`)

```json
{
  "eventId": "143bd7f0-cb5c-4f95-b6e4-5756f4d6e5e4",
  "requestEventId": "5e7ecffe-c09a-4f7f-8a78-7ce25aafc831",
  "entityType": "LISTING",
  "entityId": 101,
  "operation": "CREATE",
  "status": "REJECTED",
  "reason": "toxicity >= 0.8 or explicit profanity detected",
  "sourceAction": "reject",
  "toxicityScore": 0.91
}
```

`status`: `APPROVED | REJECTED | REVIEW | ERROR`.

## Переменные окружения

- `SPRING_KAFKA_BOOTSTRAP_SERVERS` (по умолчанию `kafka:29092`)
- `MODERIUM_API_BASE_URL` (по умолчанию `https://moderium-ai.ru`)
- `MODERIUM_API_TOKEN` (обязательно для реальных вызовов)
- `MODERIUM_API_MODE` (по умолчанию `strong`)
- `MODERIUM_API_WITH_ANIMAL` (по умолчанию `true`)
