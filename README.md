# pitomets
this is description

Предварительная настройка (создание сети)
```
docker network create web
```

Можно пробить туннель до контейнеров
```
ssh -i ~/.ssh/1244 -L 5432:localhost:5432 student@178.154.194.186
```

дев сборка
```
docker compose -f docker-compose.base.yml -f docker-compose.dev.yml down && docker compose -f docker-compose.base.yml -f docker-compose.dev.yml up --build
```
прод сборка
```
docker compose -f docker-compose.base.yml -f docker-compose.prod.yml down && docker compose -f docker-compose.base.yml -f docker-compose.prod.yml pull && docker compose -f docker-compose.base.yml -f docker-compose.prod.yml up --build
```

Сборка образа
```
docker buildx build \
  --platform linux/amd64 \
  -t artshar/frontend:1.0.0 \
  --build-arg VITE_API_BASE_URL=https://pitomets.com/api \
  --push .
```

```
 docker buildx build \
--platform linux/amd64 \
-t artshar/monolit:1.0.0 \
--push .
```

Дропнуть всё
```
docker stop $(docker ps -aq) 2>/dev/null && docker rm $(docker ps -aq) 2>/dev/null && docker volume rm $(docker volume ls -q) 2>/dev/null
```

Дашборды графаны

1860
763
9628

a
локи логи {container=~".+"}