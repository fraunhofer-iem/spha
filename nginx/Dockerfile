FROM node:25.6.1-alpine3.23 AS ui-build
WORKDIR /app
COPY ui/package*.json ui/
WORKDIR /app/ui
RUN npm ci
COPY ui/ .
RUN npm run build

FROM nginx:1.29.5-alpine
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY nginx/certs /etc/nginx/certs
COPY --from=ui-build /app/ui/dist /usr/share/nginx/html
