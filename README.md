# 🎰 抽奖平台系统

代币制在线抽奖平台（类盲盒/扭蛋机），支持代币充值、概率抽奖、保底机制、奖品仓库、代币转售、实物发货、代理佣金体系。

---

## 项目结构

```
抽奖平台/
├── lottery-platform/          # 前端 Vue 3
├── lottery-backend/           # 后端 Java Spring Boot
└── 抽奖平台系统_需求文档_v1.1.docx  # 原始需求文档
```

---

## 技术栈

| 端 | 技术 |
|---|------|
| 前端 | Vue 3 + Vite + Vue Router 4 + Pinia + Vant 4 + Element Plus + ECharts |
| 后端 | Java 17 + Spring Boot 3.x + MyBatis-Plus + MySQL 8 + Redis |
| 鉴权 | JWT + BCrypt |
| 适配 | 移动端 H5（postcss-px-to-viewport） |

---

## 角色体系

| 角色 | 路由前缀 | 布局 | 说明 |
|------|---------|------|------|
| player | /player/* | 移动端（底部 tab 栏） | 普通玩家 |
| agent | /agent/* | PC 端（侧边栏） | 代理商 |
| admin | /admin/* | PC 端（侧边栏） | 总管理员 |

一套系统，登录后按角色自动跳转对应界面。

---

## 功能完成度

### ✅ 认证模块
- [x] 登录（JWT token + 角色跳转）
- [x] 注册（支持邀请码绑定代理）
- [x] 退出登录
- [x] 路由守卫（未登录拦截、角色越权拦截）

### ✅ 玩家端
- [x] 首页（抽奖入口、余额展示）
- [x] 抽奖页（消耗代币、动画展示、保底计数显示）
- [x] 充值页（4档套餐 + 自定义数量 + 付款备注 + 充值记录）
- [x] 仓库（回收 / 删除 / 代币转售 / 取消挂售 / 请发货）
- [x] 转售市场（浏览 / 代币购买）
- [x] 发货记录（订单状态追踪）
- [x] 个人中心（余额、流水、基本信息）

### ✅ 代理后台
- [x] 数据概览（团队人数、充值总量、佣金、等级）
- [x] 旗下玩家列表（昵称、代币余额、充值总量）
- [x] 佣金统计（按月汇总，支持年月筛选）
- [x] 代理个人中心

### ✅ 总后台
- [x] 数据概览（总用户数、充值总量、今日抽奖、待处理订单、趋势折线图、代理拉新排行）
- [x] 玩家管理（搜索、代币增减、封禁/解封、手动增删仓库奖品、按代理筛选）
- [x] 创建用户（支持创建 player/agent/admin 角色）
- [x] 奖品管理（增删改查，概率配置为百分比如 0.9%）
- [x] 抽奖配置（保底阈值、单次消耗代币、大奖池奖品选择）
- [x] 充值管理（待确认订单列表，确认发币 / 驳回，触发代理佣金）
- [x] 发货订单管理（待发货 → 已发货 → 完成）
- [x] 代理管理（等级、团队人数、业绩、佣金汇总）
- [x] 转售市场管理（查看 / 下架商品）
- [x] 图片上传

### ✅ 核心业务逻辑
- [x] 抽奖防并发（Redis 分布式锁）
- [x] 保底机制（Redis 计数，到达阈值必出大奖）
- [x] 原子扣除代币（SQL 层面 `balance >= cost`）
- [x] 库存管理（库存为 0 自动下架）
- [x] 转售买卖双方代币流水
- [x] 充值确认触发代理佣金计算
- [x] 代理等级自动判定（每日凌晨 2:00 定时任务）

### ✅ 代理等级规则

| 等级 | 条件（满足其一） | 佣金比例 |
|------|----------------|---------|
| V1 | 团队人数 ≥ 10 或 充值代币 ≥ 1000 | 1% |
| V2 | 团队人数 ≥ 30 或 充值代币 ≥ 3000 | 2% |
| V3 | 团队人数 ≥ 50 或 充值代币 ≥ 5000 | 3% |

---

## 明确不含功能

- ❌ 待支付状态
- ❌ 物流查询
- ❌ 售后/退款
- ❌ 导出订单
- ❌ 在线支付对接（走淘宝购买 + 人工确认）

---

## 数据库

10 张表：`user`、`agent`、`recharge_order`、`token_log`、`prize`、`lottery_config`、`draw_record`、`warehouse`、`market`、`delivery_order`

建表 SQL：`lottery-backend/src/main/resources/db/schema.sql`

默认管理员：`admin` / `admin123`

---

## 快速启动

### 后端

```bash
# 1. 创建数据库并执行建表 SQL
mysql -u root -p < lottery-backend/src/main/resources/db/schema.sql

# 2. 修改数据库配置
# 编辑 lottery-backend/src/main/resources/application.yml
# 修改 spring.datasource.password 为你的 MySQL 密码

# 3. 确保 Redis 已启动

# 4. 启动后端
cd lottery-backend
mvn spring-boot:run
# 后端运行在 http://localhost:8080
```

### 前端

```bash
cd lottery-platform
npm install
npm run dev
# 前端运行在 http://localhost:3000
# 已配置代理，/api 请求自动转发到 8080
```

---

## 接口概览

| 模块 | 接口数 | 前缀 |
|------|--------|------|
| 认证 | 3 | /api/auth |
| 玩家端 | 13 | /api/player, /api/lottery, /api/market |
| 代理端 | 3 | /api/agent |
| 总后台 | 20+ | /api/admin |
| 文件上传 | 1 | /api/upload |

完整接口文档见：`lottery-backend/REQUIREMENTS.md`

---

## UI 风格

- **玩家端**：暗色系 + 金色点缀（#0f0f0f 背景，#f0c040 强调色），移动端适配
- **后台管理**：Element Plus 标准 PC 布局，深色侧边栏

---

## 待优化项

- [ ] 充值套餐改为后台可配置（目前前端写死 4 档）
- [ ] 404 页面样式完善
- [ ] 代理佣金逐条流水明细
- [ ] 总后台手动升降级代理
- [ ] 前端单元测试
- [ ] 后端接口单元测试
- [x] 生产环境部署配置（Nginx + 打包）
