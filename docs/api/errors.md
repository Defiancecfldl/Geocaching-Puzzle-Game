# 错误约定

本地开发服务的拟定失败响应为 `{ "requestId": "UUID", "error": { "code": "...", "message": "..." } }`，结构见 OpenAPI 的 `ApiError`。
客户端按 HTTP 状态和 `error.code` 判断，`message` 仅作说明。

| HTTP | code | 当前用途 |
| --- | --- | --- |
| 404 | NOT_FOUND | 本地服务未实现的路径或方法；随实现PR交付，当前契约PR不代表已部署 |

业务鉴权、定位不准、距离过远、重复打卡、限流等错误码尚未定稿。
D 提出契约，相关 A～C 复核字段，E 审核；定稿时同步写入 OpenAPI 并添加 Bruno 失败回归。
日志可记录 requestId，不记录 Token、真实环境 ID、密钥或完整定位信息。
