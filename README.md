# 优达智慧课堂教学管理平台

[![CI](https://github.com/CloudGiaohaooo/smart-classroom/actions/workflows/ci.yml/badge.svg)](https://github.com/CloudGiaohaooo/smart-classroom/actions/workflows/ci.yml)
![GitHub last commit](https://img.shields.io/github/last-commit/CloudGiaohaooo/smart-classroom)

**后端**：
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6.2-6DB33F?logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-0.11-000000?logo=jsonwebtokens&logoColor=white)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5-B8332C?logo=mybatis&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)

**前端**：
![Vue](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-5-646CFF?logo=vite&logoColor=white)
![Element Plus](https://img.shields.io/badge/Element_Plus-2.7-409EFF)
![Pinia](https://img.shields.io/badge/Pinia-2-FFD859?logo=pinia&logoColor=000000)
![ECharts](https://img.shields.io/badge/ECharts-5-AA344D?logo=echarts&logoColor=white)

**质量与部署**：
![JUnit](https://img.shields.io/badge/JUnit-5-25A162?logo=junit5&logoColor=white)
![Testcontainers](https://img.shields.io/badge/Testcontainers-2.0-291A3F?logo=testcontainers&logoColor=white)
![SpringDoc](https://img.shields.io/badge/SpringDoc-2.5-85EA2D?logo=swagger&logoColor=black)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)

> **一句话定位**：一个前后端分离的教学管理平台，覆盖「课程—作业—提交—批阅—资源」完整闭环；主线是**工程规范 + 安全加固**，并在此基础上做了三个**可评估的工程尝试**——智能辅助批阅、学习预警看板、资源 MD5 秒传去重。

## 项目亮点速览（30 秒电梯陈述）

| 亮点 | 一句话 | 证据 |
| --- | --- | --- |
| 🔍 智能辅助批阅 | 纯 Java TF-IDF + 余弦做「雷同检测」，历史评语频率统计做「一键推荐」，完全离线 | 100 对标注集 P/R/F1 评测 |
| 📊 学习预警看板 | `@BehaviorLog` AOP 零侵入埋点 + ECharts 看板 + 外置规则 | 登录后预警实时消失 |
| ⚡ 资源 MD5 秒传 | 同文件复用物理文件、引用计数删除 | 二次上传 `deduplicated=true` |

### 量化结果（主张—证据，详见 `docs/EVALUATION.md`）

| 维度 | 结果 |
| --- | --- |
| 后端测试 | `mvn test` **61 例 0 失败**（含 Testcontainers 真实 MySQL 集成测试，Docker 可用时运行） |
| 雷同检测 | 阈值 0.45 → P/R/F1 = **1.000/1.000/1.000**；0.75 → 1.000/0.625/0.769 |
| 登录限流 | 并发 20 → **5 放行 / 15 拦截**（与 `max-attempts=5` 一致） |
| 读接口性能 | 课程列表 QPS **49.5** / P50 **18ms**（开发机 Docker 环境，下限估计） |
| CI | 每次 push 自动跑后端测试 + 前端构建（顶部绿徽章） |

### 闭环故事线

```
学生行为埋点(@BehaviorLog)
      │  AOP 异步落库（不拖慢业务）
      ▼
student_behavior 表 ──聚合──▶ 学习预警看板(ECharts)
                                    │ 教师发现异常 → 干预
                                    ▼
                          作业批阅：雷同检测 + 评语推荐（辅助初筛）
                                    │
                                    ▼
                          评估：P/R/F1 + 压测 QPS（docs/evaluation/）
```

> 这条数据流是项目的「记忆点」：从**行为埋点**到**预警**到**干预**到**辅助批阅**再到**量化评估**，串起「有主张、有证据、可复现」的完整故事。展开讲解见 `docs/PROBLEM.md` 与 `docs/DESIGN_DECISIONS.md`。

## 文档导航（分层，各取所需）

| 文档 | 目标读者 | 内容 |
| --- | --- | --- |
| [`docs/PROBLEM.md`](docs/PROBLEM.md) | 研究生导师 | 三个「可评估工程问题」的问题定义 |
| [`docs/DESIGN_DECISIONS.md`](docs/DESIGN_DECISIONS.md) | 技术面试官 | 12 条「为什么这么选」的技术决策 + Redis key 设计 |
| [`docs/EVALUATION.md`](docs/EVALUATION.md) | 导师 + 面试官 | 主张—证据链（雷同 P/R/F1、压测 QPS、测试） |
| [`docs/LIMITATIONS.md`](docs/LIMITATIONS.md) | 导师 | 边界与反思 + 伦理/隐私边界 |
| [`docs/架构设计.md`](docs/架构设计.md) · [`docs/数据库设计.md`](docs/数据库设计.md) · [`docs/接口文档.md`](docs/接口文档.md) | 面试官 | 架构 / ER / 接口契约（另有 Swagger 在线版） |
| [`docs/evaluation/`](docs/evaluation/) | 导师 | 原始评测 / 压测报告（可复现） |
| [`答辩材料/`](答辩材料/) | 本人 | PPT 大纲、演示讲解稿、问答逐字稿、自测清单 |

## 一、项目简介

本系统面向高校教师与学生，提供：

| 模块 | 功能 | 对应实习工作 |
| --- | --- | --- |
| 课程管理 | 课程的新增、编辑、删除、分页查询、关键词搜索 | 前端「课程管理」页面开发 |
| 作业列表 | 作业的布置、编辑、删除、分页查询、课程筛选 | 前端「作业列表」页面开发 |
| 作业批阅反馈 | 学生提交作业、教师打分并填写反馈 | 后端「作业批阅反馈」接口开发 |
| 课程资源上传 | 资源文件上传、下载、分页查询、删除 | 后端「课程资源上传」接口开发 |

## 二、技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite、Element Plus、Vue Router、Pinia、Axios、ECharts |
| 后端 | Spring Boot 3.2、MyBatis-Plus、MySQL 8、Redis、Spring AOP |
| 安全 | JWT 令牌认证、BCrypt 密码加密、Spring Security 过滤链 + RBAC 权限模型 |
| 质量 | RESTful API 设计、统一响应/异常处理、事务管理、SpringDoc 接口文档、JUnit 单测、Docker 部署 |

## 三、系统架构

```
浏览器（Vue3 SPA）
      │  HTTP / JSON（RESTful）
      ▼
Spring Boot 后端
  ├─ Controller 层：接收请求、参数校验、返回统一 Result
  ├─ Service 层：业务逻辑、@Transactional 事务控制
  ├─ Mapper 层：MyBatis-Plus BaseMapper + 自定义 SQL（多表 JOIN）
  └─ MySQL 数据库
        ├─ 业务表：course / assignment / submission / course_resource
        ├─ 权限表：sys_user / sys_role / sys_permission / sys_user_role / sys_role_permission
        └─ 埋点表：student_behavior（学习预警）
```

安全链路：登录 → 后端校验账号密码（BCrypt 比对）→ 签发 JWT → 前端每次请求携带 `Authorization: Bearer <token>` → 后端 Spring Security 过滤链解析 JWT 并注入用户上下文 → `@PreAuthorize` 方法级授权（RBAC 四表）。

## 四、目录结构

```
优达智慧课堂教学管理平台
├─ backend/                     # 后端（Spring Boot）
│  ├─ pom.xml
│  └─ src/main/
│     ├─ java/com/youda/classroom/
│     │  ├─ controller/         # 接口层
│     │  ├─ service/            # 业务层
│     │  ├─ mapper/             # 数据访问层（含多表 JOIN SQL）
│     │  ├─ entity/ dto/ vo/    # 实体 / 入参 / 出参
│     │  ├─ security/           # Spring Security 过滤链 + JWT 工具、用户上下文
│     │  ├─ config/             # 配置类、演示数据初始化
│     │  └─ common/             # 统一响应、异常、状态码
│     └─ resources/
│        ├─ application.yml
│        └─ db/schema.sql       # 建表脚本
├─ frontend/                    # 前端（Vue3 + Vite）
│  └─ src/
│     ├─ views/                 # 页面
│     ├─ layout/                # 布局
│     ├─ router/ store/ utils/ api/   # 路由、状态、请求封装、接口
│     └─ main.js App.vue
├─ docs/                        # 设计文档（数据库 / 接口 / 架构）
└─ 答辩材料/                     # PPT 大纲、问答、演示讲解稿
```

## 五、快速启动

### 0. 一键启动（推荐，Docker Compose）

```bash
docker compose up -d --build
```

首次启动会自动拉取镜像、建库建表、写入演示数据，拉起 4 个容器（前端 / 后端 / MySQL / Redis），访问 `http://localhost` 即可（端口可用 `FRONTEND_PORT` / `BACKEND_PORT` 环境变量覆盖）。详见 `docker-compose.yml`。

### 1. 环境准备

- JDK 17+
- Maven 3.6+
- MySQL 8.x（默认账号 `root` / `root`，如不同请修改 `backend/src/main/resources/application.yml`）
- Node.js 18+（前端）

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

首次启动会自动完成三件事：
1. 自动创建数据库 `youda_classroom`（JDBC URL 中的 `createDatabaseIfNotExist=true`）；
2. 自动执行 `db/schema.sql` 建表（`IF NOT EXISTS`，幂等）；
3. 自动写入演示数据（`DataInitializer`）。

启动成功后，后端监听 `http://localhost:8080`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`，使用以下演示账号登录（密码均为 `123456`）：

| 账号 | 角色 | 权限 |
| --- | --- | --- |
| admin | 管理员 | 全部功能 |
| teacher | 教师 | 课程管理、布置作业、批阅、上传资源 |
| student | 学生 | 查看课程/作业/资源、提交作业、查看我的提交 |

### 4. 常见问题

- **端口占用**：修改 `application.yml` 的 `server.port`，同步修改 `vite.config.js` 的代理 target。
- **数据库连接失败**：确认 MySQL 已启动、账号密码正确。
- **Maven 下载慢**：可配置阿里云镜像仓库。
- **Windows 本地 `npm run build` 报 `Access is denied`（esbuild 临时文件）**：部分安全软件会拦截 esbuild 原生二进制写系统临时目录。改用 `frontend\build-win.bat`（把临时目录重定向到项目内 `.build-tmp`）即可；Linux / CI 不受影响。

## 六、核心亮点（答辩 / 复试可重点讲解）

### 差异化创新点（复试级）

1. **智能辅助批阅（创新点 A）**：纯 Java TF-IDF + 余弦相似度做「雷同提交检测」，历史评语频率统计做「一键推荐」，无需大模型、完全离线、阈值外置可调。
2. **学习预警看板（创新点 B）**：`student_behavior` 表 + `@BehaviorLog` 注解 + AOP 切面**零侵入埋点**，ECharts 展示提交率趋势 / 资源热力 / 预警学生，规则外置到 `application.yml`。
3. **课程资源 MD5 秒传去重（创新点 C）**：上传时纯 JDK `MessageDigest` 计算文件 MD5，相同文件复用既有物理文件、仅新增元数据（秒传），删除按 `file_url` 引用计数防误删；开关 `resource.dedup-enabled` 外置可调。

### 工程深度（生产级）

4. **RBAC + Spring Security**：四表权限模型 + JWT 无状态认证 + `@PreAuthorize` 方法级授权，替换原拦截器方案。
5. **Redis 缓存三策略**：空值缓存（防穿透）/ 互斥锁重建（防击穿）/ 随机 TTL（防雪崩），Redis 宕机自动降级直查 DB。
6. **可观测与质量**：`@RestControllerAdvice` 统一异常、SpringDoc 接口文档、JUnit 单测 + MockMvc 接口集成测试 + JaCoCo 覆盖率、GitHub Actions CI、Docker Compose 一键部署。

### 基础工程

7. **前后端分离 + RESTful**：资源化 URL、HTTP 方法语义、统一 `Result` 响应体。
8. **多表查询**：课程联表查教师名、作业联表统计提交数、提交记录四表 JOIN，演示 `LEFT JOIN` 与聚合子查询。
9. **事务管理**：作业批阅使用 `@Transactional`，保证得分/反馈/状态/时间原子更新。
10. **文件上传**：本地落盘 + 数据库元数据、按课程分目录、唯一文件名避免覆盖、MD5 秒传去重（见创新点 C）。

## 七、系统截图（界面预览）

| 界面 | 截图 |
| --- | --- |
| 登录页 | ![登录页](docs/screenshots/01-login.png) |
| 学习预警看板（创新点 B） | ![学习预警看板](docs/screenshots/02-dashboard-warning.png) |
| 智能辅助批阅（创新点 A） | ![智能辅助批阅](docs/screenshots/03-grading-assist.png) |
| 课程资源（创新点 C） | ![课程资源](docs/screenshots/04-resource-list.png) |
| 课程管理 | ![课程管理](docs/screenshots/05-course-list.png) |
| 学生提交（我的提交） | ![学生提交](docs/screenshots/06-student-submission.png) |

## 八、后续可扩展方向

- 引入 MinIO 对象存储（内容寻址），替代本地磁盘存储，进一步支撑跨实例去重
- 消息队列（RocketMQ / RabbitMQ）实现批阅结果异步通知
- WebSocket 推送预警消息，把「教师主动看」升级为「实时告警」
- 课程资源全文检索（Elasticsearch）
- Element Plus 按需引入（unplugin-vue-components）进一步压缩 vendor 体积

## 九、改进路线图（复试级）

> 完整任务清单、验收标准与进度记录见 [`docs/improvement-plan.md`](docs/improvement-plan.md)。以下为概要。

围绕「智能」与「可信」两个关键词做增量，在不推倒重来的前提下补齐差异性与工程深度：

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| Phase 1 | 基线与文档（`dev` 分支、改进计划、路线图） | ✅ |
| Phase 2 | 权限与缓存（RBAC 四表 + Spring Security + Redis 三策略） | ✅ |
| Phase 3 | 创新点 A：智能辅助批阅（TF-IDF 雷同检测 + 评语推荐） | ✅ |
| Phase 4 | 创新点 B：学习预警看板（AOP 埋点 + ECharts 看板） | ✅ |
| Phase 5 | 部署与测试（Docker 一键起 + 单测覆盖率 + SpringDoc） | ✅ |
| Phase 6 | 答辩材料沉淀（创新点说明 / 技术问答 / 演示脚本） | ✅ |
| Phase 7 | 创新点 C：课程资源 MD5 秒传去重（本地去重 + 引用计数删除） | ✅ |
| Phase 8 | MockMvc 接口集成测试（资源上传 + 秒传去重） | ✅ |
| Phase 9 | MockMvc 补全（批阅辅助 / 批阅 / 提交 / 课程 RBAC） | ✅ |
| Phase 10 | 安全加固：配置可注入 + JWT 强化（HS256 校验、禁用账号即时失效） | ✅ |
| Phase 11 | 安全加固：上传扩展名白名单 + 文件名清洗 + 下载加固（防存储型 XSS） | ✅ |
| Phase 12 | 正确性：参数校验 500→400、级联删除保护、缓存写后失效、满分兜底 | ✅ |
| Phase 13 | 前端健壮性：真实 401 处理、localStorage 安全解析、防双击重入、内存泄漏修复 | ✅ |
| Phase 14 | 越权修复：学生只读本人提交、教师仅操作本人课程（P0-A/B） | ✅ |
| Phase 15 | 数据完整性：显式字段拷贝、防重复提交、负分校验、缓存键收敛 | ✅ |
| Phase 16 | 部署硬化：中间件不对外暴露、专用账号 + Redis 密码、JWT 密钥告警 | ✅ |
| Phase 17 | 复审 v3 收尾：grading-assist 归属校验、孤儿提交 404、显式拷贝补全、老库去重 + 唯一索引迁移 | ✅ |
| Phase 18 | 选课模型：course_enrollment 表 + 选/退课接口 + 学生只看/交自己选的课（RBAC 扩展） | ✅ |
| Phase 19 | 权限缓存：Caffeine 本地缓存权限编码，JWT 过滤器每请求 DB 查询减半 | ✅ |
| Phase 20 | 文件安全：/files/** 签名 URL 鉴权下载 + 流式上传（弃 getBytes 大文件 OOM） | ✅ |

**三个创新点**（A + B 分别对应实习前端与后端成果，C 为工程加分项）：

- **创新点 A · 智能辅助批阅**：轻量 TF-IDF + 余弦相似度做「雷同提交检测」，历史评语关键词检索做「一键推荐」，评分仍走原 `@Transactional` 链路。
- **创新点 B · 学习预警看板**：`student_behavior` 表 + AOP 切面零侵入埋点，ECharts 展示提交率趋势 / 资源热力 / 预警学生，规则外置到 `application.yml`。
- **创新点 C · MD5 秒传去重**：上传时纯 JDK `MessageDigest` 计算文件 MD5，相同文件复用既有物理文件（秒传），删除按 `file_url` 引用计数防误删。
