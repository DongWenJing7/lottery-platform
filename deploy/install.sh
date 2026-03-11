#!/bin/bash
set -e

# ========================================
#  抽奖平台 一键部署脚本 (Ubuntu/Debian)
# ========================================

# 颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log()  { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[INFO]${NC} $1"; }
err()  { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# 检查 root
if [ "$EUID" -ne 0 ]; then
  err "请使用 root 运行: sudo bash install.sh"
fi

DEPLOY_DIR=$(cd "$(dirname "$0")" && pwd)
APP_DIR=/opt/lottery
MYSQL_ROOT_PASS=""
DB_PASS="Lottery@2024"
REDIS_PASS="Lottery@Redis2024"

echo ""
echo "========================================="
echo "  抽奖平台 一键部署"
echo "========================================="
echo ""

# ========== 1. 安装 Java 17 ==========
if java -version 2>&1 | grep -q "17\.\|21\."; then
  log "Java 17+ 已安装"
else
  warn "正在安装 Java 17..."
  apt update -qq
  apt install -y -qq openjdk-17-jre-headless > /dev/null 2>&1
  log "Java 17 安装完成"
fi

# ========== 2. 安装 MySQL ==========
if command -v mysql &> /dev/null; then
  log "MySQL 已安装"
else
  warn "正在安装 MySQL..."
  apt install -y -qq mysql-server > /dev/null 2>&1
  systemctl start mysql
  systemctl enable mysql
  log "MySQL 安装完成"
fi

# 创建数据库和用户
warn "正在初始化数据库..."
mysql -u root ${MYSQL_ROOT_PASS:+-p$MYSQL_ROOT_PASS} -e "
  CREATE DATABASE IF NOT EXISTS lottery DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE USER IF NOT EXISTS 'lottery'@'localhost' IDENTIFIED BY '${DB_PASS}';
  GRANT ALL PRIVILEGES ON lottery.* TO 'lottery'@'localhost';
  FLUSH PRIVILEGES;
" 2>/dev/null

mysql -u lottery -p"${DB_PASS}" lottery < "${DEPLOY_DIR}/schema.sql" 2>/dev/null
log "数据库初始化完成"

# ========== 3. 安装 Redis ==========
if command -v redis-server &> /dev/null; then
  log "Redis 已安装"
else
  warn "正在安装 Redis..."
  apt install -y -qq redis-server > /dev/null 2>&1
  log "Redis 安装完成"
fi

# 配置 Redis 密码
if ! grep -q "^requirepass ${REDIS_PASS}" /etc/redis/redis.conf 2>/dev/null; then
  sed -i "s/^# requirepass .*/requirepass ${REDIS_PASS}/" /etc/redis/redis.conf
  sed -i "s/^requirepass .*/requirepass ${REDIS_PASS}/" /etc/redis/redis.conf
  systemctl restart redis
fi
log "Redis 配置完成"

# ========== 4. 部署应用 ==========
warn "正在部署应用..."
mkdir -p ${APP_DIR}
cp "${DEPLOY_DIR}/lottery-backend-1.0.0.jar" ${APP_DIR}/app.jar

# 生成配置文件
cat > ${APP_DIR}/application.yml << EOF
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lottery?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: lottery
    password: ${DB_PASS}
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASS}
      database: 0
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

jwt:
  secret: $(openssl rand -hex 32)
  expiration: 604800

lottery:
  cors:
    allowed-origins: "*"
EOF

log "应用部署完成"

# ========== 5. 创建 systemd 服务 ==========
cat > /etc/systemd/system/lottery.service << EOF
[Unit]
Description=Lottery Platform
After=network.target mysql.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=${APP_DIR}
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar ${APP_DIR}/app.jar --spring.config.additional-location=file:${APP_DIR}/application.yml
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable lottery
systemctl start lottery

log "服务已启动"

# ========== 6. 开放防火墙 ==========
if command -v ufw &> /dev/null; then
  ufw allow 8080/tcp > /dev/null 2>&1
  log "防火墙已开放 8080 端口"
fi

# ========== 完成 ==========
echo ""
echo "========================================="
echo -e "  ${GREEN}部署完成！${NC}"
echo "========================================="
echo ""
echo "  访问地址: http://$(hostname -I | awk '{print $1}'):8080"
echo ""
echo "  管理员账号: admin"
echo "  管理员密码: admin123"
echo ""
echo "  管理命令:"
echo "    启动: systemctl start lottery"
echo "    停止: systemctl stop lottery"
echo "    重启: systemctl restart lottery"
echo "    日志: journalctl -u lottery -f"
echo ""
echo "  数据库密码: ${DB_PASS}"
echo "  Redis 密码: ${REDIS_PASS}"
echo "  配置文件: ${APP_DIR}/application.yml"
echo ""
