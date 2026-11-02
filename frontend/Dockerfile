FROM node:26.13.0-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend ./
RUN npm run build

FROM caddy:2.11.7-alpine
COPY infra/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist /srv
# The caddy binary ships with cap_net_bind_service file capabilities; with cap_drop ALL
# the kernel refuses to exec it. Port 8081 needs none.
RUN apk add --no-cache --virtual .setcap libcap-setcap \
 && setcap -r /usr/bin/caddy \
 && apk del .setcap \
 && addgroup -S -g 101 app && adduser -S -u 100 -G app app \
 && mkdir -p /data /config \
 && chown app:app /data /config
USER app
EXPOSE 8081
