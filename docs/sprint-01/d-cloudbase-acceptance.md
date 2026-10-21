# D 第一周 CloudBase 验收证据

负责人：张博淳（D）

## 基线

- 日期：2026-10-21
- 环境：`cloud-d0g0u4r64556b8177`
- 区域：`ap-shanghai`
- 数据库：CloudBase PostgreSQL 17.11
- 函数：`api`，HTTP，Node.js 20.19
- 契约版本：`1.0.0-poc`
- OpenAPI：`docs/api/openapi.yaml`

## 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| 健康检查 | 通过 | `GET /api/v1/health` 返回 200 和 `requestId` |
| 地标接口 | 通过 | 返回 `lmk_library`、真实难度和提示字段 |
| Challenge | 通过 | 创建后返回 `chl_` 前缀 ID 和 UTC 过期时间 |
| 照片上传 | 通过 | JPEG 上传后返回 `pho_` 前缀 ID 和对象路径 |
| 打卡与碎片 | 通过 | 正常打卡返回 200 和 `frg_campus_01_01` |
| 重复点击 | 通过 | 返回同一 checkin 的 `ALREADY_COMPLETED` |
| 幂等键冲突 | 通过 | 相同 key 配不同请求返回 409 `IDEMPOTENCY_CONFLICT` |
| 鉴权 | 通过 | 无 Token 返回 401 `AUTH_REQUIRED` |
| 数据一致性 | 通过 | 数据库保留 1 条 checkin、1 条 fragment grant |
| 日志追踪 | 通过 | requestId 可查到 START、Response、END、Report |
| 审计记录 | 通过 | `audit_logs` 存在 `CHECKIN_GRANTED` 记录 |
| 数据库迁移 | 通过 | `init_schema`、`seed_first_week`、`add_landmark_display_fields` 均已应用 |

## 自动化验证

```text
Test Files  5 passed
Tests       23 passed
OpenAPI     valid
Audit       16 repository toolchain advisories
```

测试覆盖：

- 正常打卡
- 范围外
- 低定位精度
- challenge 过期
- 照片缺失
- 连续点击
- 超时重试
- 同一幂等键配不同请求
- 照片 MIME、大小、objectKey 和用户目录
- 固定契约辅助函数

## 已知边界

- 代码和契约通过 `feat/cloudbase-api-poc` 分支提交 PR，目标分支为 `develop`。
- 第一周只使用一个测试账号、一个测试地标、一个任务、一个拼图和一块碎片。
