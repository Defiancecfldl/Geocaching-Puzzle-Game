# API 契约

对应验收项：`S1-API-01`，本阶段仅交付 health 契约，不含实现、测试或云端部署。

`openapi.yaml` 是 Android、云端和测试共用的接口标准；`errors.md` 解释错误约定。
按 CONTRIBUTING.md：先评审合并契约，再分别交付实现与测试；后续PR引用该契约并验证一致性。
契约需由 D 及相关 B/C 评审，E 核对回归。没有非作者审核时不能自行合并。

## health 0.1.0

GET `/health`，不需要认证；成功返回HTTP200、JSON和响应头 `X-Request-Id`、`Cache-Control: no-store`。

```json
{
  "requestId": "123e4567-e89b-42d3-a456-426614174000",
  "data": {
    "status": "ok",
    "service": "geotreasure-api",
    "version": "0.1.0",
    "timestamp": "2026-10-21T00:00:00.000Z"
  }
}
```

requestId为UUID v4，响应头和响应体相同；时间必须是带毫秒的UTC ISO8601。
服务健康只证明API进程可用，不证明数据库、存储、鉴权或云端部署可用。
后续实现与Bruno回归位于 `chore/quality-baseline`，必须等待本契约合并后才能合入develop。

## 后续业务契约

`Landmark`、`CacheTask`、`CheckInRecord`、`PuzzleProgress`、分页、定位单位/精度/距离阈值、业务鉴权和错误码仍需定稿。
地标、打卡、重复打卡、进度等请求模板不是正式契约，也不能作为已通过回归的证据。
