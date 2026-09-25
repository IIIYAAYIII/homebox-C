# Homebox Android 签名密钥说明

本项目已为你预先生成了有效期长达 10,000 天（约 27 年）的标准 PKCS12 格式 Android 正式签名秘钥。

## GitHub Secrets 配置说明

进入你的 GitHub 仓库页面：`Settings` -> `Secrets and variables` -> `Actions` -> `New repository secret`，添加以下 4 个密钥：

| Secret 名称 | 取值说明 | 建议填入值 |
| :--- | :--- | :--- |
| `KEYSTORE_BASE64` | `homebox-release.keystore.base64.txt` 中的**完整文本**（单行无换行） | 复制 `homebox-release.keystore.base64.txt` 内的全部字符 |
| `KEYSTORE_PASSWORD` | 签名库密码 | `HomeboxRelease@2026` |
| `KEY_ALIAS` | 签名别名 | `homebox` |
| `KEY_PASSWORD` | 私钥密码 | `HomeboxRelease@2026` |

配置好这 4 个 Secrets 后，每次 GitHub Actions 打包都会使用这套统一密钥进行 APK 签名，你的手机在后续收到新版本时可以直接**覆盖安装升级**，绝不会出现“签名不一致”或需要卸载丢失配置的问题。
