# FASE 1: Build
FROM node:19-alpine AS build
WORKDIR /Document-web-app-fe

# Installazione dipendenze
COPY package*.json ./
RUN npm install

# Copia codice e build
COPY . .
RUN npm run build

# FASE 2: Runtime (Nginx)
FROM nginx:stable-alpine
# Copiamo la build di React/Vite nella cartella di Nginx
COPY --from=build /Document-web-app-fe/dist /usr/share/nginx/html

# Espone la porta standard HTTP
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]