FROM node:24-alpine AS build
WORKDIR /app
COPY package.json yarn.lock ./
RUN corepack yarn install --frozen-lockfile
COPY . .
RUN corepack yarn build
FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
