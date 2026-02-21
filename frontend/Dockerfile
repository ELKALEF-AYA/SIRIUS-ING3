# ============================================================
# Dockerfile – frontend (Vite + React)
# Context: frontend/
# Build:  docker build -t sirius-frontend ./frontend
# ============================================================

# ---------- Stage 1 : Build ----------
FROM node:20-alpine AS builder

WORKDIR /app

# Install deps first (better layer caching)
COPY package.json package-lock.json ./
RUN npm ci --silent

# Copy source and build
COPY . .
RUN npm run build

# ---------- Stage 2 : Serve with nginx ----------
FROM nginx:1.27-alpine

# Remove default nginx page
RUN rm -rf /usr/share/nginx/html/*

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

# nginx config: handle SPA routing (React Router)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
