# API 契约

第 1 周客户端与云端共同遵守的 HTTP 契约。

- 负责人：张博淳（D）
- 契约版本：`1.0.0-poc`
- 正式契约：[openapi.yaml](./openapi.yaml)
- 服务端实现：`cloudbase/functions/api/`
- 客户端调用方：B 的地图与主流程、C 的相机与上传流程
- 团队接入与凭据共享规则：[CloudBase 协作接入包](../sprint-01/cloudbase-collaboration-handoff.md)

坐标统一为 `GCJ02`，时间统一为 UTC ISO 8601，距离字段统一以 `Meters` 结尾，JSON 字段统一使用 `camelCase`。

修改契约前先提交 `[S1][Contract]` Issue，并通知 B、C、D、E；涉及 UI 时同时通知 A。
