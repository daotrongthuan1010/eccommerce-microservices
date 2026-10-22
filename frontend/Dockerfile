# syntax=docker/dockerfile:1
# FE_SCAFFOLD §3 — VITE_* la build-time: doi URL phai build lai image
# voi --build-arg tuong ung (ConfigMap runtime khong co tac dung).
ARG VITE_API_BASE_URL=http://162.4.177.91/api
FROM node:20-alpine AS build
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM nginx:alpine
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /web/dist /usr/share/nginx/html
EXPOSE 80
