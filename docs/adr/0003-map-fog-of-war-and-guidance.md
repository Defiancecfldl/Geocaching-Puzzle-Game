# 地图页战争迷雾与引导设计

日期：2026-10-22

## 1. 目标

战争迷雾属于地图页的探索反馈，不放在地标详情页。用户在地图上移动时，当前位置周围的侦查范围逐步打开；已打开的区域在当前探索会话中保持可见，未探索区域显示为灰色半透明遮罩。

首版侦查半径为 120 米，只有定位精度不高于 50 米的位置样本才能产生新的探索记录。这个数值集中在地图功能代码中，后续可以改成远程配置或关卡配置。

## 2. 当前实现

位置数据流为：

```text
LocationProvider
    -> MapViewModel
    -> MapUiState.exploredCenters
    -> MapRenderState
    -> MapAdapter / DemoMapContent
```

- `MapViewModel` 收到可靠定位后，按 60 米的最小间隔记录探索中心。
- 高德地图使用一个覆盖全图的灰色 `Polygon`，使用 `CircleHoleOptions` 为每个探索中心挖出可见区域。
- 普通玩家界面不绘制侦查圈，只显示侦查范围内已经被驱散的正常地图；开发者调试模式可以单独打开侦查圈。
- 离线演示地图使用 Compose 的灰色遮罩和透明圆孔，保持与真实地图相同的交互概念。
- 地标覆盖物仍由地图适配器管理。未探索地标会被迷雾遮挡，进入侦查范围后自然显示。
- 完成地标任务后，地图为该地标增加一个更大的区域孔洞，直接驱散该区域的迷雾。

当前探索记录和演示版完成地标记录保存在应用内存中，应用进程结束后会清空。正式版本需要把 `exploredCenters` 与完成地标状态持久化到本地数据库或按关卡保存到服务端。

## 3.1 开发者调试模式

Debug 包的地图页提供开发者调试面板，可以：

- 临时显示全图，关闭迷雾；
- 显示或隐藏侦查范围圈；
- 调整当前选中地标的打卡范围；
- 在圆形和矩形范围之间切换；
- 直接标记当前地标完成，验证区域驱散效果。

调试覆盖只存在于当前进程状态，不改变正式地图数据。Release 包不显示该入口。

## 3. 引导功能的边界

引导箭头是可选的辅助道具，不能自动替代寻宝过程。建议按以下顺序开发：

1. 首版只提供手动“显示线索”按钮，不加入自动计时，先验证箭头是否容易理解。
2. 每局设置有限次数，例如 3 次；次数写入 `GuideState.remainingUses`。
3. 引导目标从未完成地标中选择距离当前位置最近的一个，使用 `distanceMeters` 计算。
4. 箭头方向需要当前位置到目标点的方位角，以及手机当前朝向。位置模型建议增加 `bearingDegrees`，朝向由 `SensorManager` 或高德定位结果提供。
5. 以后再接入谜语：服务端返回 `HintChallenge`，玩家回答正确后生成 `GuideGrant`；客户端只负责展示获准的箭头，不在客户端保存正确答案。

建议的接口命名：

```kotlin
data class GuideState(
    val visible: Boolean = false,
    val targetLandmarkId: String? = null,
    val bearingDegrees: Float? = null,
    val distanceMeters: Double? = null,
    val remainingUses: Int = 3,
)

sealed interface GuideAction {
    data object RequestHint : GuideAction
    data class SubmitRiddle(val answer: String) : GuideAction
    data object Dismiss : GuideAction
}

interface GuidePolicy {
    fun chooseNearestTarget(
        location: GeoPoint,
        landmarks: List<Landmark>,
        completedLandmarkIds: Set<String>,
    ): Landmark?
}
```

## 4. 分工

- B：`MapViewModel` 中的探索状态、地图迷雾覆盖物、侦查圈、箭头绘制和手机朝向适配。
- D：引导次数、谜语挑战、答案校验和服务端防重复使用接口。
- C：拍照完成后更新已完成地标，使引导目标能够排除已完成任务。
- E：灰色透明度、侦查圈颜色、箭头图标、按钮和动画规范。

## 5. 暂缓项

- 真实校园地图的遮罩边界和道路裁切需要最终地图底图后再调整。
- 探索记录跨进程恢复需要本地存储或服务端进度接口。
- 自动卡关计时、谜语校验和引导次数不能在 B 当前的 Fake Repository 阶段定稿。
- 正式开放前，地标坐标和打卡范围必须完成实地核验，避免迷雾已经显示但目标点位置不准确。
