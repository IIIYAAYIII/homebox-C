# Homebox Android 移动客户端

专为 Homebox 资产管理系统打造的原生 Android 客户端，支持局域网直接连接运行在 ARM 服务器 / NAS 上的 Homebox Docker 容器。

## 核心特性
- **局域网直连优化**：支持 HTTP 明文连接与连通性自动测试探测，杜绝 Android 9+ 的 Cleartext 限制。
- **资产与物品全生命周期管理**：支持物品浏览、多条件搜索过滤、查看详情（基本信息、规格型号、序列号SN、购入价格）、创建与删除。
- **存放位置与库位浏览**：以库位树形式浏览各个存放位置及其下属资产。
- **硬件级相机扫码**：集成条形码与二维码扫描，扫标签直达资产详情，录入资产时扫商品码自动填入序列号/条形码。
- **深度本土化简体中文**：采用贴合国内仓储与资产管理习惯的地道用语。
- **GitHub Actions 远程自动编译**：通过 GitHub Actions 云端构建，自动使用专用 Keystore 签名并生成可直接覆盖安装的 Release APK。

## 构建方式
本应用主要设计为在 **GitHub Actions** 中自动打包：
1. 推送代码到 GitHub 或创建 Tag（如 `v1.0.0`）。
2. 在 GitHub 仓库页面的 `Actions` -> `Build Android APK` 点击 `Run workflow` 即可一键生成 APK 并下载。
