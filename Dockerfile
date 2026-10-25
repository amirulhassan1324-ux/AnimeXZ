FROM node:20-alpine
WORKDIR /app
COPY server/package.json ./
RUN npm install --omit=dev
COPY server/ .
ENV PORT=8000
EXPOSE 8000
CMD ["node", "index.js"]
