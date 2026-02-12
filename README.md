# pitomets
this is description

Можно пробить туннель до контейнеров
```
ssh -i ~/.ssh/pitomets -L 5432:localhost:5432 student@178.154.194.186
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
--push .
```

Дропнуть всё
```
docker stop $(docker ps -aq) docker rm $(docker ps -aq) docker volume rm $(docker volume ls -q)
```

Дашборды графаны

1860
763
9628