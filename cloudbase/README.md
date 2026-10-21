# 云端与本地开发

Node.js 固定 24.20.0；CloudBase CLI 3.8.7、Bruno CLI 4.2.1 写入本目录 devDependencies。
仓库使用 npm workspace，统一在根目录 `npm ci`，提交根目录 `package-lock.json`。
`.npmrc` 拒绝 Node 版本不匹配；`.nvmrc` 和 `.node-version` 供切换版本，不会自动安装 Node。

根目录 `npm run dev` 使用 Node 24 原生 TypeScript 支持运行 health，仅监听本机。
`npm run typecheck` 另行检查类型。`functions/api/health.ts` 是 health 逻辑，`src/server.ts` 是本地 HTTP 适配层。
尚无 CloudBase 部署配置、云函数 HTTP 事件适配器、数据库或存储调用。
本地 Node 版本不代表云端已支持同版本运行时，部署前 D 需单独核对。

自定义端口时将 `.env.example` 复制为 `.env`，同步覆盖 Bruno 的 API_BASE_URL。
登录或操作云环境前由 D/E 确认目标，通过 `npm run cloudbase -- ...` 使用锁定 CLI。
真实环境 ID、Token 和密钥只在被忽略的私有配置中填写。
