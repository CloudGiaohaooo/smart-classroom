# 改进计划（复试级）

> 本文件是「优达智慧课堂教学管理平台」从单机 Demo 向「可真实部署 + 有差异化创新点」演进的任务清单与进度记录。
> 每条主线对应一个可验收的阶段，完成后在 `答辩材料/` 下沉淀对应文档。

## 0. 定位与总纲

**现状**：课程 / 作业 / 批阅 / 资源四模块完整，前后端分离、JWT、事务、多表 JOIN 基础扎实。

**两个复试硬伤**：
1. 功能是「标准教学管理系统」，缺少与市面现成产品的差异化；
2. 工程深度停留在单机 Demo：无缓存、无权限模型、无部署方案。

**改进关键词**：围绕「智能」与「可信」做增量，满足：
- 现实可用：能真实部署给一个班级试用（Docker 一键起、权限隔离、数据看板）；
- 有所提升：至少 2 个可讲清楚的创新点，每个都有「问题—方案—效果」完整叙事；
- 结合实习成果：把「课程管理前端」「作业批阅 / 资源上传后端」沉淀为可复用工程能力。

**执行原则**：不推倒重来，增量分支推进，保留原功能可运行；每个 Phase 结束更新 README 与答辩材料，并 git commit（`feat(phaseN): ...`）。

---

## 1. 创新点设计

### 创新点 A：作业批阅的「智能辅助评分」（对应实习后端成果）
- **痛点**：教师批阅重复性高、反馈模板化。
- **方案**：
  1. 相似答案聚类：对同一作业的学生提交做文本相似度计算（TF-IDF + 余弦相似度，纯 Java 轻量实现，无需大模型），标记疑似雷同提交；
  2. 历史评语推荐：基于教师过往评语做关键词检索，批阅页给出 Top3 推荐评语，一键填入；
  3. 评分提交仍走原 `@Transactional` 链路，保证原子性。
- **叙事**：「不是用大模型炫技，而是用轻量 NLP 解决教师真实重复劳动，且完全离线可跑、无隐私外泄风险。」
- **验收**：批阅页出现「相似提交提示」与「推荐评语」；提供 20 条模拟提交的聚类效果截图。

### 创新点 B：基于行为数据的「学习预警看板」（对应实习前端成果）
- **痛点**：教师难以及时发现「长期不交作业 / 不看资源」的学生。
- **方案**：
  1. 新增 `student_behavior` 表，记录登录、看资源、交作业等事件（AOP 切面统一埋点，业务零侵入）；
  2. ECharts 教师端看板：提交率趋势、资源访问热力、预警学生列表（连续 N 次未交 / 资源访问低于阈值）；
  3. 预警规则外置到 `application.yml`（可配置，非硬编码）。
- **叙事**：「把'事后统计'变成'事中预警'，埋点通过 AOP 解耦，业务代码零侵入。」
- **验收**：教师登录可见看板；演示数据触发 ≥2 条预警；埋点切面与业务代码物理分离。

### 创新点 C：课程资源「MD5 秒传去重」（Phase 7 已实现）
- **痛点**：同一课件被不同老师 / 课程反复上传，本地磁盘重复存储浪费空间。
- **方案**：上传时用纯 JDK `MessageDigest` 计算文件 MD5（32 位十六进制），写入 `course_resource.file_md5`；相同 MD5 已存在时复用既有物理文件、仅新增一条元数据记录（秒传），否则才落盘。删除时按 `file_url` 引用计数，避免误删被多记录共享的物理文件。开关外置到 `application.yml`（`resource.dedup-enabled`）。
- **叙事**：「解决同一课件被反复上传的存储浪费，MD5 去重是网盘级能力。」
- **验收**：连续两次上传同一文件，第二次返回 `deduplicated=true` 且物理文件只存在一份；结果通过新增响应字段 `ResourceUploadVO` 返回，原 `/api/resources/upload` 路径与语义不变。

> **取舍说明**：创新点 C 沿用现有本地磁盘存储（按课程分目录），去重粒度为「同 MD5 复用同一物理文件」，未引入 MinIO——避免改动既有存储路径破坏存量资源（硬性约束 6）。后续如需跨实例去重可平滑迁移到 MinIO 内容寻址存储。

---

## 2. 工程深度补强（让创新点「站得住」）

| # | 补强项 | 说明 |
| --- | --- | --- |
| 1 | 权限模型 | Spring Security + RBAC（`sys_role` / `sys_permission` / `sys_user_role` / `sys_role_permission` 四表），替换现有拦截器；保留 JWT 作认证载体。必须讲清「拦截器鉴权 vs Security 过滤链」差异 |
| 2 | 缓存层 | Redis 缓存课程列表与看板聚合数据；落地「空值缓存(穿透) / 互斥锁重建(击穿) / 随机 TTL(雪崩)」三件套并注释 |
| 3 | 部署方案 | `docker-compose.yml` 一键拉起 MySQL + Redis + MinIO + 后端 + Nginx；提供 `.env.example` |
| 4 | 测试 | 核心 Service 单测 + 批阅/上传接口 MockMvc 集成测试 + 覆盖率报告 |
| 5 | 接口文档 | SpringDoc，`/swagger-ui` 可访问 |

---

## 3. 分阶段任务清单

### Phase 1：基线与文档
- [x] 拉取 `dev` 分支，确认 `mvn compile` 与 `npm run dev` 可跑通（后端编译验证 EXIT=0）
- [x] `docs/` 下新建 `improvement-plan.md`（本文件）
- [x] 更新 `README.md` 增加「改进路线图」章节

### Phase 2：权限与缓存
- [x] 建 RBAC 四表 + 初始化数据（admin/teacher/student 映射到角色）
- [x] 接入 Spring Security，配置 JWT 过滤器，保留 `ThreadLocal` 上下文供业务读取
- [x] 接入 Redis，改造课程列表查询与看板聚合查询，落地缓存三策略
- [x] 验收：三个演示账号权限隔离正确；Redis 可见缓存键；关 Redis 后接口降级不 500

### Phase 3：创新点 A —— 智能辅助批阅
- [x] 相似度计算工具类（TF-IDF + 余弦，纯 Java，不引重依赖）
- [x] 批阅详情接口返回「相似提交列表」
- [x] 评语推荐：基于历史评语的关键词检索
- [x] 前端批阅页增加两个展示区 + 一键填入
- [x] 验收：20 条模拟提交演示聚类；评语推荐命中率可展示；原事务链路未被破坏

### Phase 4：创新点 B —— 学习预警看板
- [x] `student_behavior` 表 + AOP 埋点切面（登录 / 看资源 / 交作业三类事件）
- [x] 教师端看板页：ECharts 提交率趋势、资源热力、预警列表
- [x] 预警规则外置到 `application.yml`
- [x] 验收：演示数据触发预警；埋点切面与业务代码分离；看板数据与 DB 一致

### Phase 5：部署与测试
- [x] `docker-compose.yml` + Nginx 配置
- [x] 单测 / 集成测试 + 覆盖率
- [x] SpringDoc + Swagger 可访问
- [x] 验收：新机器 `docker compose up` 后前端可登录、看板可看、批阅可用（配置就绪，本机未装 Docker 未做容器化启动验证）

### Phase 6：答辩材料沉淀
- [x] `答辩材料/创新点说明.md`：每个创新点「问题—方案—效果」三段式 + 对比截图
- [x] `答辩材料/技术选型问答.md`：RBAC vs 拦截器、Redis 三问题、TF-IDF 为何够用、AOP 埋点优势
- [x] `答辩材料/部署与演示脚本.md`：一键启动步骤 + 演示动线
- [x] 更新根 `README.md`「核心亮点」，把创新点提到最前

### Phase 7：创新点 C —— MD5 秒传去重
- [x] `course_resource` 新增 `file_md5` 列（schema.sql + `SchemaMigrationRunner` 幂等迁移老库）
- [x] `FileMd5` 工具（纯 JDK `MessageDigest`）+ `ResourceUploadVO` 上传结果
- [x] `upload()` 计算 MD5、命中复用文件不重复落盘；`delete()` 按 file_url 引用计数防误删
- [x] 开关 `resource.dedup-enabled` 写入 `application.yml` 带注释
- [x] 前端上传提示区分「秒传成功 / 上传成功」
- [x] `FileMd5Test` 单测（RFC 1321 标准向量）+ `ResourceServiceTest`（去重命中复用 / 未命中落盘 / 引用计数删除 / 空文件分支）

### Phase 8：MockMvc 接口集成测试（资源上传 + 秒传去重）
- [x] `ResourceControllerTest`：`@WebMvcTest` + `@ContextConfiguration` 精确加载「控制器 + 真实 Security 过滤链 + 真实 JwtUtil」，Service 与 RBAC Mapper 用 `@MockBean` 隔离，不依赖 MySQL/Redis
- [x] 覆盖：未登录 401 / 无写权限 403 / 有权限 200 + 秒传去重响应字段（HTTP 恒 200 + 响应体业务码契约）

### Phase 9：MockMvc 接口集成测试补全（批阅 + 课程 RBAC）
- [x] `SubmissionControllerTest`：批阅辅助 / 批阅 / 提交接口的方法级授权（学生 403、教师 200 + `GradingAssistVO` 字段）
- [x] `CourseControllerTest`：课程建课 / 查询的 RBAC（学生建课 403、教师建课 200、学生查课 200）
- [x] `mvn test` 25 测试全绿（11 MockMvc + 5 ResourceService + 2 FileMd5 + 2 BehaviorService + 5 TextSimilarity）

---

## 4. 硬性约束（执行时严格遵守）

1. 不改动现有公开接口的路径与语义，新增能力通过新接口或响应字段扩展；
2. 新增配置项写入 `application.yml` 并带注释，禁止硬编码魔法值；
3. 每个 Phase 结束 git commit，格式 `feat(phaseN): ...`；
4. 不引入需联网下载大模型权重的依赖，创新点 A 的 NLP 纯本地轻量实现；
5. 每完成一个创新点，在本文档勾选并补一段 ≤200 字效果说明；
6. 若与现有代码冲突，优先保留原功能可运行，在文档中记录取舍原因。

---

## 5. 进度与效果记录

> 每个 Phase / 创新点完成后，在此追加「日期 + 内容 + 效果」条目。

- **2026-10-17 · Phase 2 完成**：引入 Spring Security 过滤链替换 JwtInterceptor（保留 JWT 载体与 ThreadLocal 上下文），落地 RBAC 四表（`sys_role`/`sys_permission`/`sys_user_role`/`sys_role_permission`，11 项权限、3 角色）并用 `@PreAuthorize` 做方法级授权；新增 `RedisCacheService` 落地「空值缓存(穿透)/互斥锁重建(击穿)/随机 TTL(雪崩)」三策略，缓存课程列表与看板统计。**效果**：实测学生调用建课/批阅返回 403、教师批阅 200、未登录 401；关闭 Redis 后接口自动降级直查数据库，日志确认不 500。
- **2026-10-17 · Phase 3 完成（创新点 A：智能辅助批阅）**：新增 `TextSimilarity`（TF-IDF + 余弦，CJK 单字+二元分词，纯 Java 零重依赖）、`GradingAssistService` 与 `/api/submissions/{id}/grading-assist` 接口；批阅页新增「相似提交提示（雷同检测）」与「推荐评语（历史高频，一键填入）」两个区块。幂等初始化器种入 1 演示作业 + 20 名学生 + 20 条「5 簇×4 人」成簇提交 + 9 条已批阅评语。**效果**：实测对同簇提交调用批阅辅助，返回 3 条同簇提交相似度 86.9%~94.3% 且全部标记「疑似雷同」，跨簇文本被阈值过滤；评语推荐命中 3 条（各 count=3、均分 85），原批阅/提交事务链路未受影响。
- **2026-10-17 · Phase 4 完成（创新点 B：学习预警看板）**：新增 `student_behavior` 埋点表 + `@BehaviorLog` 注解 + `BehaviorLogAspect` 切面（`@AfterReturning` 声明式埋点，登录/看资源/交作业三类事件，与业务代码完全解耦、try/catch 兜底不影响主流程）；新增 `/api/stats/behavior` 看板接口与教师端三个展示区（提交率趋势折线、资源热力柱状、预警列表）。预警阈值外置到 `application.yml`（`warning.*`）。**效果**：实测看板返回近 7 天提交趋势（20 条/天）、3 门课程资源热度、3 名静默 30 天学生预警；登录 demo18 后其「长期未登录」预警实时消失（切面实时写入新登录事件），证明埋点链路与预警规则联调正确，看板数据与 DB 一致。
- **2026-10-17 · Phase 5 完成（部署与测试）**：新增 `docker-compose.yml`（mysql/redis/backend/frontend 四服务 + 健康检查 + 卷持久化）与前后端多阶段 `Dockerfile`、`nginx.conf`（静态托管 + `/api`、`/files` 反向代理）；接入 `springdoc-openapi`（`OpenApiConfig` 配 JWT Bearer 认证，SecurityConfig 放行 `/swagger-ui`、`/v3/api-docs`）；新增 7 个单测（`TextSimilarityTest` 5 + `BehaviorServiceTest` 2，纯 Mock 无需外部依赖）+ JaCoCo 覆盖率插件。**效果**：`mvn verify` 全绿——7 测试全通过、`smart-classroom-1.0.0.jar` 打包成功；实测 `/v3/api-docs`、`/swagger-ui/index.html` 均 200（含 title 与 bearerAuth）。说明：本机未装 Docker，容器化启动未做本机验证；JaCoCo exec 采集在本机 Windows+中文路径下受路径编码影响（无空格路径可正常产出报告），标准 CI/Linux 环境不受影响。
- **2026-10-17 · Phase 6 完成（答辩材料）**：新增 `答辩材料/` 脱敏公开版三份——`创新点说明.md`（问题—方案—效果三段式 + 答疑要点）、`技术选型问答.md`（RBAC vs 拦截器 / Redis 三问题 / TF-IDF 为何够用 / AOP 埋点优势等 6 问）、`部署与演示脚本.md`（本地与 Docker 两种启动 + 5 步演示动线 + 30 秒电梯陈述）；根 `README.md`「核心亮点」已重构为「差异化创新点 → 工程深度 → 基础工程」三层，创新点提到最前。脱敏用【姓名】【学号】占位；含真实信息的 `答辩材料-原版备份/` 已 gitignore，确认未进入暂存区。
- **2026-10-17 · Phase 7 完成（创新点 C：MD5 秒传去重）**：`course_resource` 新增 `file_md5` 列（schema.sql + `SchemaMigrationRunner` 幂等迁移老库，information_schema 判存在）；新增纯 JDK `FileMd5`（`MessageDigest`，零外部依赖）与 `ResourceUploadVO`（resourceId/fileMd5/deduplicated/fileUrl）。`upload()` 先算 MD5 再查重，命中则复用既有 `file_url` 不重复落盘，否则落盘；`delete()` 按 `file_url` 引用计数，多条记录共享同一物理文件时保留文件不误删；开关 `resource.dedup-enabled` 外置。前端上传后按 `deduplicated` 区分「秒传成功 / 上传成功」。**效果**：`mvn test` 9 测试全绿（新增 `FileMd5Test` 2 例，对照 RFC 1321 标准向量）；原 `/api/resources/upload` 路径与语义不变，仅扩展响应字段。
- **2026-10-17 · Phase 8 完成（MockMvc 接口集成测试）**：新增 `ResourceControllerTest`（`@WebMvcTest` + `@ContextConfiguration` 精确加载 `ResourceController`/`SecurityConfig`/`JwtAuthenticationFilter`/`JwtUtil`，`@MockBean` 隔离 `ResourceService` 与 `PermissionMapper`，不依赖 MySQL/Redis）。**效果**：`mvn test` 13 测试全绿；实测未登录返回业务码 401、学生无 `resource:write` 上传返回 403、教师上传返回 200 且带 `deduplicated=true`/`fileMd5`/「秒传成功」字段——用真实 Security 过滤链 + 真实 JWT 验证了「HTTP 恒 200 + 响应体业务码」既定契约。
- **2026-10-17 · Phase 9 完成（MockMvc 补全：批阅 + 课程 RBAC）**：新增 `SubmissionControllerTest`（批阅辅助 `GET /grading-assist`、批阅 `PUT /grade`、提交 `POST` 三类接口的方法级授权）与 `CourseControllerTest`（建课 `POST`、查课 `GET` 的 RBAC）。**效果**：`mvn test` 20 测试全绿（11 MockMvc + 2 FileMd5 + 2 BehaviorService + 5 TextSimilarity）；实测学生访问批阅辅助 / 批阅 / 建课均 403、教师批阅辅助返回 200 且带 `similar[].similarity/flagged` 与 `suggestions[].text`、学生提交/查课 200——RBAC 方法级授权在 HTTP 层全覆盖验证。
- **2026-10-17 · 创新点 C 测试闭环补齐**：新增 `ResourceServiceTest`（5 例，Mockito + 临时目录），直接验证 `upload()` 的 MD5 去重分支（命中复用 `fileUrl` 不落盘、未命中落盘并持久化 MD5）与 `delete()` 的引用计数删除（有引用保留文件、无引用删除文件），补齐空文件异常分支。至此创新点 C 形成「算法 `FileMd5Test` → 服务 `ResourceServiceTest` → 接口 `ResourceControllerTest`」三层测试闭环。**效果**：`mvn test` 25 测试全绿。
- **2026-10-17 · 创新点 C 端到端冒烟验证（真实环境可行性）**：在 8081 端口对真实 MySQL + 真实磁盘做全链路冒烟——同一文件连续上传两次，第一次 `deduplicated=false` 并落盘、第二次 `deduplicated=true` 且 `fileUrl`/`fileMd5` 与第一次完全一致，物理文件仅存在一份；随后删除第一条记录时物理文件被保留（第二条仍引用）、删除最后一条后物理文件被清理。**效果**：秒传去重与引用计数删除在真实运行环境全链路验证通过，与三层测试结论一致。
- **2026-10-17 · Phase 10~13 评审驱动优化（安全 > 正确性 > 前端健壮性）**：对全项目做一轮三轮评审（安全 / 后端正确性 / 前端），合并去重后按「可行性优先」分 4 个 Phase 落地——

  - **Phase 10（安全·配置与认证）**：`application.yml` 的 JWT 密钥/DB 凭据改为环境变量注入（`JWT_SECRET`/`DB_USERNAME`/`DB_PASSWORD`，保留可运行默认值）；`JwtAuthenticationFilter` 增加「复查账号是否存在且未被禁用」——账号禁用/删除后已签发 token 立即失效；`parseToken` 显式校验签名算法 HS256（jjwt 0.11 无 `setAllowedAlgorithms`，改解析后校验 header 算法）；3 个 MockMvc 测试补 `@MockBean UserMapper`。
  - **Phase 11（安全·上传与下载）**：上传增加扩展名白名单（`resource.allowed-extensions` 外置，拒绝 html/svg/js 等可执行类型）+ 原始文件名清洗（去路径分隔符/控制字符/限长）；新增 `FileDownloadHeaderFilter` 对 `/files/**` 强制 `Content-Disposition: attachment` + `X-Content-Type-Options: nosniff`，与白名单形成纵深防御封死存储型 XSS；补 `mkdirs()`/`delete()` 返回值检查。
  - **Phase 12（正确性）**：`GlobalExceptionHandler` 补 `MissingServletRequestParameterException`/`MethodArgumentTypeMismatchException`/`MissingServletRequestPartException`（缺参/类型错误由 500 转 400）；删除课程/作业前校验关联数据（存在作业/资源/提交则拒绝），防孤儿记录；各写路径统一 `evict` 看板统计缓存保证「写后即失效」；批阅满分空值兜底防 NPE。
  - **Phase 13（前端健壮性 + 清理）**：请求拦截补真实 HTTP 401 处理（清登录态跳转）；`localStorage` 解析加 try/catch 防白屏；Dashboard resize 监听随组件销毁移除、图表 dispose 后置空、统计加载容错；课程保存/删除与批阅提交加防双击重复提交锁；移除后端 `countPending()` 死代码。

  **效果**：`mvn test` 26 测试全绿（新增非法扩展名单测）；前端 `npm run build` 通过；所有公开接口路径与语义不变，改动均为「新增防护 + 内部加固」。

  **已知取舍/待办（低优先级未改，避免过度工程）**：① CORS `allowedOriginPatterns("*")` 收敛需结合部署域名（本机 Vite 代理同源，暂不影响）；② 登录接口无限流（需 Redis 限流，演示场景可接受）；③ 上传仍一次性 `getBytes()` 读入内存（受 100MB 上限约束，秒传需整块算 MD5）；④ 登录页演示账号明文为答辩刻意设计；⑤ 初始化器启动期 N+1 与 BCrypt 多实例属一次性开销。
- **2026-10-17 · Phase 14~16 复审驱动优化（越权 > 数据完整性 > 部署硬化）**：针对复审报告 v2 的「P0-A/P0-B 两条横向越权 + 数据完整性缺口」按「答辩前（半天）」清单落地——

  - **Phase 14（越权修复 · P0-A/P0-B）**：新增 `CourseOwnershipGuard` 统一「课程归属」校验（ADMIN 放行、TEACHER 仅本人课程、其余角色拒绝）；学生查提交在 `SubmissionController#page` 强制 `studentId=本人`，教师查提交在 `SubmissionService#page` 按 `teacher_id` 过滤（`SubmissionMapper` 联表新增 `c.teacher_id` 条件）；批阅/改课/删课/改作业/删作业/传资源/删资源全部接入归属校验；`CourseService.create` 非管理员强制 `teacher_id=本人`（管理员可代建）。**效果**：`mvn test` 27 测试全绿（新增「学生查全站只返回本人」用例），学生读全站、教师跨课程操作两条越权路径封死。
  - **Phase 15（数据完整性 · P1-1/2/3 + 小问题）**：`CourseService`/`AssignmentService` 弃用 `BeanUtils.copyProperties` 改显式字段拷贝（未传字段不覆盖，修复 `course_name` 被清空 500 / 简介被清空）；`submission` 表加 `UNIQUE KEY (assignment_id, student_id)` + `submit()` 预查重与 `DuplicateKeyException` 兜底（防重复提交）；`GradeDTO.score` 加 `@DecimalMin("0.0")`（拒负分）；`ResourceService` 白名单逐项 trim；`FileDownloadHeaderFilter` 补 `Cache-Control: no-store`；`JwtAuthenticationFilter` 拆分 `JwtException`/`IllegalArgumentException` 与非预期异常（数据库故障不再被伪装成 401）；缓存键收敛到 `CacheKeys` 常量类。**效果**：`mvn test` 27 测试全绿。
  - **Phase 16（部署硬化 · P0-C + 文档同步）**：`docker-compose.yml` 移除 MySQL/Redis 宿主机端口映射、MySQL 改用专用账号 `youda`（非 root）、Redis 开 `requirepass`，凭据均可环境变量覆盖；`application.yml` 增 `spring.data.redis.password`；`JwtUtil` 启动时校验密钥，命中源码演示密钥即告警。README 路线图补齐 Phase 10~16、CI 注释更正为 27 测试。

  **待办/取舍（诚实记录，属后续演进而非本轮取舍）**：① `submission` 唯一索引对老库不生效（`sql.init=always` 无 Flyway，仅新建库生效），已用应用层预查重兜底，正式迁移建议上 Flyway；② compose 本机未做容器化验证（本机无 Docker/Redis），凭据默认值仍为演示值、生产须覆盖；③ 批阅辅助 `grading-assist` 与 `/files/**` 下载鉴权、权限缓存、Redis 锁 owner 等「两周内」项未做。
- **2026-10-17 · Phase 17 完成（复审 v3「今天」清单：越权补漏 + 校验补全 + 老库幂等迁移）**：针对复审报告 v3 的 5 条「今天（1–2 小时）」项落地——

  - **补 `grading-assist` 归属校验**：`GradingAssistService.assist()` 先查作业（null 抛 404）再 `ownershipGuard.assertCourseOwned(courseId)`，封死「教师读其它课程提交摘要/学生姓名/相似度」的横向泄露。
  - **孤儿提交不可批阅**：`SubmissionService.grade()` 将「作业已删除的孤儿提交」由“跳过校验”改为直接 404，避免任意教师对无归属提交打分。
  - **显式拷贝补全**：`CourseService.update()` 的 `courseName`、`AssignmentService.update()` 的 `courseId/title` 由无条件覆盖改为判 null 才覆盖，消除“部分更新把必填字段置 null → 500”。
  - **校验补全**：`CourseService.create()` ADMIN 代建课程校验 `teacherId` 非空（400）；`GradeDTO.feedback` 加 `@Size(max=1000)`，超长反馈由写库 500 转为参数校验 400。
  - **老库幂等迁移**：`SchemaMigrationRunner` 新增 `migrateSubmissionUniqueIndex()`——先按 `(assignment_id, student_id)` 去重（保留最早一条）再 `ADD UNIQUE KEY uk_assignment_student`，用 `information_schema.STATISTICS` 判存在保证幂等；老库从此同样享有数据库层防重复提交。

  **效果**：`mvn test` 27 测试全绿、`mvn compile` 通过；全部为「新增防护 + 内部加固」，公开接口路径与语义不变。**已知取舍**：`GradeDTO.feedback` 上限 1000 与 `schema.sql` 的 `feedback VARCHAR(1000)` 对齐；老库去重 `DELETE` 使用 MySQL 多表删除语法（本系统固定 MySQL 8）。
- **2026-10-17 · Phase 18 完成（选课模型 course_enrollment：业务模型最后一道分水岭）**：针对复审 v3「本周」第 1 项（即 2.2 第 4 项）落地——

  - **选课表**：新增 `course_enrollment`（`course_id` + `student_id`，唯一键 `uk_course_student` 防重复选课），schema.sql 幂等建表（`sql.init.mode=always` 对老库同样生效，无需额外迁移）。
  - **选课接口**：`EnrollmentService` + `EnrollmentController`（`/api/enrollments`）——学生 `POST/{courseId}` 选课、`DELETE/{courseId}` 退课、`GET/my` 查我选的课、`GET/course/{courseId}/students` 教师/管理员查选课名单（名单查询接 `CourseOwnershipGuard`，教师只能看自己课程的选课名单）。
  - **RBAC 扩展**：新增 `enrollment:read`/`enrollment:write` 权限——STUDENT 选/退 + 查、TEACHER/ADMIN 仅查，延续最小权限原则。
  - **选课门禁（封死「学生可对任意作业提交」）**：`SubmissionService.submit` 对 STUDENT 强制 `assertEnrolled(assignment.courseId)`；`CourseService.page`/`AssignmentService.page` 对 STUDENT 按 `EXISTS (SELECT 1 FROM course_enrollment ...)` 过滤，学生只看/交自己选的课，教师/管理员不受限。
  - **演示数据**：`EnrollmentInitializer`（@Order(4)）幂等补齐——student/lisi/wangwu 选全部课程、demo01~20 选第一门课，答辩现场开箱即用。

  **效果**：`mvn test` 39 测试全绿（新增 `EnrollmentControllerTest` 5 + `EnrollmentServiceTest` 5 + `SubmissionServiceTest` 2）；选课能力全部走新接口，公开接口路径不变，仅对学生视角做数据级收窄（与 Phase 14 授权收紧同性质）。**已知取舍**：① 选课为「学生自主选课」，教师/管理员代选未做（演示由初始化器直接落库）；② `/api/courses/all` 仍作为选课目录返回课程公开元信息（课程名/教师/学分），未按学生过滤；③ 前端已补选课 API（`enrollCourse`/`unenrollCourse`/`getMyCourses`/`getCourseStudents`），但未重写课程列表为「目录 + 选课」两段式 UI，学生当前列表即为其已选课程。
- **2026-10-17 · Phase 19 完成（权限缓存：JWT 过滤器每请求 DB 查询减半）**：针对复审 v3「本周」第 2 项（即 2.2 第 5 项「权限缓存」）落地——

  - **本地缓存**：新增 `PermissionCacheService`（`com.youda.classroom.cache`），用 Caffeine 纯内存缓存 `userId -> 权限编码列表`，`maximumSize(10_000)` + `expireAfterWrite`，TTL 由 `application.yml` 的 `permission-cache.ttl`（默认 300 秒）外置可调。
  - **过滤链改造**：`JwtAuthenticationFilter` 新增 `loadPermCodes()`——先命中缓存，未命中回源 `permissionMapper.selectPermCodesByUserId` 并回填；权限加载由「每请求一次 DB 查询」变为「命中后零 DB 查询」。
  - **账号状态仍每请求复查**：`userMapper.selectById` 保持每请求校验，因此 Phase 10「禁用账号即时失效」不受缓存影响；只有权限变更（改角色/改权限）最多延迟一个 TTL 生效，属可接受的最终一致。

  **效果**：`mvn test` 42 测试全绿（新增 `PermissionCacheServiceTest` 3：命中/回填/失效/空值安全）；过滤链每个已登录请求的 DB 查询从 2 次降为 1 次（账号状态查 + 权限缓存命中），QPS 提升显著。**已知取舍**：① 权限变更非即时生效（TTL 内旧权限残留），可接受；已预留 `PermissionCacheService.evict()` 供「改角色/改权限」后主动失效（当前管理端无改权限界面，故未接入调用点）；② 缓存为单机本地内存，多实例部署时各实例独立缓存，权限变更会存在实例间短暂不一致（TTL 兜底收敛）；③ Caffeine 为新增依赖（约 1MB 纯内存缓存库，无外部服务），满足「纯本地轻量、无需联网权重」约束。
- **2026-10-17 · Phase 20 完成（/files/** 鉴权下载 + 流式上传）**：针对复审 v3「本周」第 3 项（即 2.2 第 7 项「/files/** 鉴权下载 + 流式上传」）落地——

  - **签名 URL（鉴权下载）**：新增 `security/SignedFileUrl`——对每个文件路径生成 `?expires=<时间戳>&sign=<HMAC-SHA256>` 签名链接（密钥复用 `jwt.secret`，TTL 外置 `file.sign-url-ttl` 默认 1800s）。前端下载用 `window.open(fileUrl)`，浏览器原生请求无法带 JWT 头，故不用「登录鉴权」而用「签名 URL」：`FileDownloadHeaderFilter` 对 `/files/**` 校验签名与时效，匿名 / 过期 / 篡改的直连下载一律返回业务码 401。
  - **流式上传**：`FileMd5` 新增 `md5Hex(InputStream)` 边读边更新摘要；`ResourceService.upload` 弃用 `file.getBytes()`（原来最多把 100MB 整文件读进堆内存），改为流式算 MD5 + `file.transferTo(dest)` 流式落盘，大文件上传不再出现内存峰值。
  - **响应签名**：`ResourceService.page`/`upload` 返回的 `fileUrl` 改为签名 URL，数据库仍存未签名路径（签名随每次响应新鲜生成）。

  **效果**：`mvn test` 50 测试全绿（新增 `SignedFileUrlTest` 3 + `FileDownloadHeaderFilterTest` 4 + `FileMd5Test` 流式 1）；前端零改动（`window.open` 对签名 URL 透明）。**已知取舍**：① 签名 URL 为「按路径 + 时效」鉴权而非「按用户」鉴权——拿到链接的人在有效期内可下载（时间窗内链接可分享），换取前端零改动的兼容性；② 上传从「一次读入」变为「两遍流式读取」（一遍算 MD5、一遍落盘），以磁盘 I/O 换内存安全，属可接受取舍；③ 若需按用户/按选课做细粒度下载鉴权，可后续改为「下载接口 + @PreAuthorize + 流式响应」，本阶段未做。
