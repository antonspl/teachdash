FROM node:26.13.0-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend ./
RUN npm run build

FROM caddy:2.11.7-alpine
COPY infra/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist /srv
RUN addgroup -S app && adduser -S -G app app \
 && mkdir -p /data /config \
 && chown app:app /data /config
USER app
EXPOSE 80
