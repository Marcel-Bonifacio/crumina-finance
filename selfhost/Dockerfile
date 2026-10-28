# Build context must be the REPO ROOT:  docker build -f selfhost/Dockerfile ..
# The image holds no secrets (see ../.dockerignore): pass them at runtime (compose env_file, docker run --env-file).
FROM node:20-slim
WORKDIR /srv
COPY app ./app
COPY selfhost/server.js ./selfhost/server.js
RUN cd app && npm ci --omit=dev
ENV PORT=3000
EXPOSE 3000
USER node
CMD ["node", "selfhost/server.js"]
