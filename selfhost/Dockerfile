# Build context must be the REPO ROOT:  docker build -f selfhost/Dockerfile ..
FROM node:20-slim
WORKDIR /srv
COPY app ./app
COPY selfhost ./selfhost
RUN cd app && npm ci --omit=dev
ENV PORT=3000
EXPOSE 3000
CMD ["node", "selfhost/server.js"]
