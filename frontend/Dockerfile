# ===================================================================
# Stage 1: Build Vite Production Bundle
# ===================================================================
FROM node:20-alpine AS builder

WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm ci

# Copy source code and build
COPY . .
RUN npm run build

# ===================================================================
# Stage 2: Production Nginx Server
# ===================================================================
FROM nginx:alpine AS runner

WORKDIR /usr/share/nginx/html

# Clean default nginx files
RUN rm -rf ./*

# Copy production bundle and nginx configuration
COPY --from=builder /app/dist .
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# Health Check
HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD wget -q -O - http://127.0.0.1:80/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
