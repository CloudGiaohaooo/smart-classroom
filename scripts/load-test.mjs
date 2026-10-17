#!/usr/bin/env node
/**
 * 零依赖压测脚本（Node 24 内置 http + 全局 fetch，无需 npm install）。
 *
 * 用法：node scripts/load-test.mjs [baseUrl] [并发] [课程列表请求数]
 *   baseUrl 默认 http://localhost:8080（后端直连；可改为 http://localhost 走 Nginx 反代）
 *   并发 默认 20
 *   请求数 默认 1000（仅作用于「课程列表」并发场景）
 *
 * 三个场景：
 *   S1 登录接口（顺序 ×100）：测 BCrypt 密码校验的每请求成本与顺序吞吐（顺序下不触限流）。
 *   S2 登录接口（并发突发 ×20）：观察固定窗口限流（账号+IP）在并发下的 429 表现。
 *   S3 课程列表（并发，带 JWT）：测 Redis 缓存读路径的 QPS / P50 / P90 / P99。
 */
import http from 'node:http';
import https from 'node:https';

const BASE = (process.argv[2] || 'http://localhost:8080').replace(/\/$/, '');
const CONCURRENCY = Number(process.argv[3] || 20);
const COURSE_REQUESTS = Number(process.argv[4] || 1000);

const agent = new http.Agent({ keepAlive: true, maxSockets: CONCURRENCY + 10 });

function request(path, { method = 'GET', body, headers = {}, timeoutMs = 15000 } = {}) {
  const url = new URL(BASE + path);
  const lib = url.protocol === 'https:' ? https : http;
  const payload = body ? JSON.stringify(body) : undefined;
  return new Promise((resolve) => {
    const started = Date.now();
    const req = lib.request(url, {
      method,
      agent,
      headers: {
        'Content-Type': 'application/json',
        ...(payload ? { 'Content-Length': Buffer.byteLength(payload) } : {}),
        ...headers,
      },
    }, (res) => {
      let data = '';
      res.on('data', (c) => (data += c));
      res.on('end', () => {
        let json = null;
        try { json = JSON.parse(data); } catch { /* 非 JSON 响应 */ }
        resolve({ status: res.statusCode, json, ms: Date.now() - started });
      });
    });
    req.setTimeout(timeoutMs, () => req.destroy(new Error('timeout')));
    req.on('error', (e) => resolve({ status: 0, json: null, error: e.message, ms: Date.now() - started }));
    if (payload) req.write(payload);
    req.end();
  });
}

function percentile(sorted, p) {
  if (!sorted.length) return 0;
  const idx = Math.min(sorted.length - 1, Math.max(0, Math.ceil(p * sorted.length) - 1));
  return sorted[idx];
}

function summarize(latencies) {
  const sorted = [...latencies].sort((a, b) => a - b);
  const n = sorted.length;
  const sum = sorted.reduce((a, b) => a + b, 0);
  return {
    n,
    meanMs: +(sum / n).toFixed(2),
    p50: sorted.length ? percentile(sorted, 0.50) : 0,
    p90: sorted.length ? percentile(sorted, 0.90) : 0,
    p99: sorted.length ? percentile(sorted, 0.99) : 0,
    min: sorted[0] ?? 0,
    max: sorted[n - 1] ?? 0,
    qps: n > 0 && sum > 0 ? +((n * 1000) / sum).toFixed(1) : 0,
  };
}

async function runConcurrent(fn, n, concurrency) {
  let idx = 0;
  const latencies = [];
  const results = [];
  async function worker() {
    while (idx < n) {
      idx++;
      const r = await fn();
      if (r.ms != null) latencies.push(r.ms);
      results.push(r);
    }
  }
  await Promise.all(Array.from({ length: Math.min(concurrency, n) }, worker));
  return { latencies, results };
}

async function main() {
  console.log(`压测目标：${BASE}  并发=${CONCURRENCY}  课程列表请求数=${COURSE_REQUESTS}\n`);

  // 预热登录，拿到有效 JWT
  const warm = await request('/api/auth/login', {
    method: 'POST',
    body: { username: 'teacher', password: '123456' },
  });
  const token = warm.json?.data?.token;
  console.log(`[预热] 登录 HTTP=${warm.status} code=${warm.json?.code} 有token=${!!token} 耗时=${warm.ms}ms`);
  if (!token) {
    console.error('登录失败，请确认后端已启动且账号 teacher/123456 存在：', JSON.stringify(warm.json));
    process.exit(1);
  }

  // S1：登录顺序 ×100（测 BCrypt 成本，顺序不触限流）
  const s1 = await runConcurrent(
    () => request('/api/auth/login', { method: 'POST', body: { username: 'teacher', password: '123456' } }),
    100, 1,
  );
  const s1sum = summarize(s1.latencies);
  console.log(`\n[S1] 登录（顺序 ×100）：QPS=${s1sum.qps}  mean=${s1sum.meanMs}ms  P50=${s1sum.p50}ms  P90=${s1sum.p90}ms  P99=${s1sum.p99}ms  max=${s1sum.max}ms`);

  // S2：登录并发突发 ×20（观察限流）
  const s2 = await runConcurrent(
    () => request('/api/auth/login', { method: 'POST', body: { username: 'teacher', password: '123456' } }),
    20, 20,
  );
  const s2ok = s2.results.filter((r) => r.json?.data?.token).length;
  const s2limited = s2.results.filter((r) => r.json?.code === 429).length;
  const s2other = s2.results.length - s2ok - s2limited;
  console.log(`[S2] 登录（并发突发 ×20）：成功=${s2ok}  限流429=${s2limited}  其他=${s2other}`);

  // S3：课程列表并发（带 JWT，测缓存读路径）
  const s3 = await runConcurrent(
    () => request('/api/courses?current=1&size=10', { headers: { Authorization: `Bearer ${token}` } }),
    COURSE_REQUESTS, CONCURRENCY,
  );
  const s3sum = summarize(s3.latencies);
  const s3non200 = s3.results.filter((r) => r.status !== 200).length;
  console.log(`\n[S3] 课程列表（并发 ×${COURSE_REQUESTS}）：QPS=${s3sum.qps}  mean=${s3sum.meanMs}ms  P50=${s3sum.p50}ms  P90=${s3sum.p90}ms  P99=${s3sum.p99}ms  max=${s3sum.max}ms  非200=${s3non200}`);

  console.log('\n压测完成。');
}

main().catch((e) => {
  console.error('压测异常：', e);
  process.exit(1);
});
