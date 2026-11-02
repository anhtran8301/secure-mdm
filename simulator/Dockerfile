FROM node:24-alpine
WORKDIR /app
COPY package.json yarn.lock ./
RUN corepack yarn install --frozen-lockfile
COPY tsconfig.json ./
COPY src ./src
RUN corepack yarn build
ENTRYPOINT ["node", "dist/cli.js"]
CMD ["run", "--state", "/state", "--trust", "/keys/command-public.json"]
