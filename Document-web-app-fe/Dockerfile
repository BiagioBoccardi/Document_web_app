# FASE 1: Build
FROM node:20-alpine AS build
WORKDIR /Document-web-app-fe

# Installazione dipendenze
COPY package*.json ./

# Rimuove eventuali binding errati e installa da zero per Linux
RUN npm ci
# Copia codice e build
COPY . .
RUN npm run build

# FASE 2: Runtime (Nginx)
FROM nginx:alpine

RUN rm /etc/nginx/conf.d/default.conf

# Copia configurazione specifica per React
COPY nginx-fe.conf /etc/nginx/conf.d/default.conf

# Copia la build di React
COPY --from=build /Document-web-app-fe/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]