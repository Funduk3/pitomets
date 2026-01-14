FROM node:20-alpine

WORKDIR /app

# сначала зависимости (кешируется)
COPY package*.json ./
RUN npm install

# потом исходники
COPY . .

# dev-сервер
EXPOSE 3001
CMD ["npm", "run", "dev"]
