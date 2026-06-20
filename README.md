![android-see-netflow](https://socialify.git.ci/VincentZyu233/android-see-netflow/image?custom_description=+Rust+%2B+Kotlin+%E9%A9%B1%E5%8A%A8%E7%9A%84+Android+%E7%BD%91%E5%8D%A1%E6%B5%81%E9%87%8F%E7%9B%91%E6%8E%A7%E5%AE%9E%E9%AA%8C+%E5%AE%89%E5%8D%93GUI%E7%A8%8B%E5%BA%8F%EF%BC%8C%E9%80%9A%E8%BF%87+WebSocket+%E5%B0%86%E5%AE%9E%E6%97%B6%E7%BB%9F%E8%AE%A1%E6%8E%A8%E9%80%81%E5%88%B0%E5%B1%80%E5%9F%9F%E7%BD%91+TUI+%E6%9C%8D%E5%8A%A1%E7%AB%AF+%F0%9F%93%A1%F0%9F%93%8A%F0%9F%A6%80%F0%9F%A4%96&custom_language=Kotlin&description=1&forks=1&issues=1&language=1&logo=https%3A%2F%2Fupload.wikimedia.org%2Fwikipedia%2Fcommons%2Fd%2Fd5%2FRust_programming_language_black_logo.svg&name=1&owner=1&pulls=1&stargazers=1&theme=Light)

# android-see-netflow

局域网内查看 Android 设备网卡流量的实验项目。

[![Rust](https://img.shields.io/static/v1?label=Rust&message=Core%20Backend&labelColor=5c5c5c&color=DEA584&style=for-the-badge&logo=rust&logoColor=white)](https://www.rust-lang.org/)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=Android%20GUI&labelColor=5c5c5c&color=7F52FF&style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)

[![Last Commit](https://img.shields.io/github/last-commit/VincentZyuApps/android-see-netflow?style=flat-square&logo=github&color=181717&logoColor=white)](https://github.com/VincentZyuApps/android-see-netflow/commits/main/)
[![Build](https://img.shields.io/github/actions/workflow/status/VincentZyuApps/android-see-netflow/build-android.yml?style=flat-square&logo=githubactions&logoColor=white&label=build)](https://github.com/VincentZyuApps/android-see-netflow/actions)

[![Android ARMv7](https://img.shields.io/badge/Android-ARMv7-7CB342?style=for-the-badge&logo=android&logoColor=white)](https://github.com/VincentZyuApps/android-see-netflow/releases)
[![Android ARM64-v8a](https://img.shields.io/badge/Android-ARM64--v8a-168039?style=for-the-badge&logo=android&logoColor=white)](https://github.com/VincentZyuApps/android-see-netflow/releases)
[![Android x86_64](https://img.shields.io/badge/Android-x86__64-43A047?style=for-the-badge&logo=android&logoColor=white)](https://github.com/VincentZyuApps/android-see-netflow/releases)
[![Android Universal](https://img.shields.io/badge/Android-Universal-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/VincentZyuApps/android-see-netflow/releases)

当前设计分为两部分：

- `server/`
  - Python + Textual 编写的 TUI 服务端
  - 负责监听 WebSocket、接收设备遥测数据、在终端里展示
- `client/`
  - Android 客户端
  - 使用 `Kotlin + Rust`
  - Kotlin 负责 Android 生命周期、前台服务、权限和联网
  - Rust 目前提供 JNI 桥骨架，后续继续下沉采样核心与协议逻辑

## 当前状态

现在仓库已经完成第一版可运行 MVP：

- `server/main.py`
  - 启动时强制检查 `config.yaml`
  - 如果没找到，会直接报错，并提示用户从 `config.example.yaml` 复制
  - 启动 WebSocket 服务并打开 Textual TUI
  - 会展示设备名、主接口、实时上下行、累计流量和在线状态
- `server/mock_client.py`
  - 本机模拟一个 Android 设备持续发送测试流量数据
- `client/android/`
  - Kotlin 版 Android MVP 已可运行
  - `MainActivity` 可输入 WebSocket 服务端地址并启动/停止前台服务
  - 前台服务会使用 `TrafficStats` 采集总上下行并通过 WebSocket 持续上报
- `client/rust-core/`
  - Rust 核心已增加最小 JNI 导出函数
  - CI 现在会尝试编译 Android `.so` 并打进 APK
  - 目前仍未接入真实采样链路

## CI / Release

仓库已增加 GitHub Actions workflow：

- `.github/workflows/build-android.yml`

当前目标：

- 在 GitHub Actions 上构建 Android release APK
- 产出以下 4 个包：
  - `ARMv7 (armeabi-v7a)`
  - `ARM64-v8a (arm64-v8a)`
  - `x86_64`
  - `universal`
- APK 文件名直接带版本号与架构，例如：
  - `android-see-netflow-armeabi-v7a-vx.y.z.apk`
  - `android-see-netflow-arm64-v8a-vx.y.z.apk`
  - `android-see-netflow-x86_64-vx.y.z.apk`
  - `android-see-netflow-universal-vx.y.z.apk`
- 如果是 GitHub Release 事件，会自动渲染 release notes 并上传 APK

Release 模板位于：

- `.github/release_template.md`

当前 workflow 触发策略参考了 `dart-flutter-demo` 的思路：

- `push` 到 `main` 时：
  - commit message 包含 `build action` -> 只构建并上传 CI artifact
  - commit message 包含 `build release` -> 构建，并进入 release 发布阶段
  - 其他 commit -> 跳过构建
- `pull_request`
  - 默认会构建并上传 artifact，但不会发布 release
- `workflow_dispatch`
  - 默认会构建并上传 artifact，但不会发布 release
- `release`
  - 会构建并上传 GitHub Release 资产

版本号默认值可通过脚本一键更新：

```powershell
python .\scripts\bump.py x.y.z
```

Rust Android JNI 库可通过脚本单独构建：

```powershell
python .\scripts\build_android_rust.py
```

## 目录结构

```text
android-see-netflow/
  README.md
  server/
    config.example.yaml
    main.py
    mock_client.py
    requirements.txt
  client/
    android/
    protocol/
    rust-core/
  scripts/
```

## 服务端用法

先进入服务端目录：

```powershell
cd D:\aaaStuffsaaa\from_git\github\android-see-netflow\server
```

创建虚拟环境并安装依赖：

```powershell
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

复制配置文件：

```powershell
copy config.example.yaml config.yaml
```

启动服务端：

```powershell
python main.py
```

如果你还没有 Android 客户端真实发数据，可以先开一个模拟发送端：

```powershell
python mock_client.py
```

默认情况下，服务端会监听：

```text
ws://0.0.0.0:8765
```

你可以在 `server/config.yaml` 中修改监听地址、端口、历史长度和界面标题。

## 客户端说明

`client/` 现在已经具备 Kotlin 侧的最短链路 MVP，但 Rust core 和 JNI 还未接入真实采样路径。

当前已有内容：

- `client/android/`
  - 可输入 `ws://<server-ip>:8765`
  - 可启动/停止前台服务
  - 使用 `TrafficStats` 采集总上下行
  - 使用 OkHttp WebSocket 持续上报 JSON
- `client/protocol/telemetry.md`
  - 约定了服务端与客户端之间的 JSON 消息格式
- `client/rust-core/`
  - 已增加最小 JNI 桥接函数
  - CI 会尝试构建 `.so` 并打进 APK

后续准备继续完成：

1. Rust 与 Kotlin 之间的 JNI 调用接入真实链路
2. 服务端增加更强的设备管理和历史存储
3. 视情况把更多采样逻辑下沉到 Rust
4. 梳理本地 Android Rust 构建体验

## 配置约定

服务端要求必须存在：

```text
server/config.yaml
```

仓库里只提供示例文件：

```text
server/config.example.yaml
```

也就是：

1. 用户先复制一份示例配置
2. 再按自己需要修改
3. 没有 `config.yaml` 就不允许启动

## 协议格式

当前约定客户端通过 WebSocket 发送 JSON，例如：

```json
{
  "device_id": "pixel-7a",
  "device_name": "Pixel 7a",
  "timestamp": 1781922334123,
  "network_type": "wifi",
  "interfaces": [
    {
      "name": "wlan0",
      "rx_bytes": 123456789,
      "tx_bytes": 45678901,
      "rx_rate": 182340,
      "tx_rate": 92341
    }
  ]
}
```

字段含义：

- `timestamp`: Unix 毫秒时间戳
- `rx_rate` / `tx_rate`: 每秒字节数
- `interfaces`: 后续可同时包含 `wlan0`、蜂窝网卡等接口

## 当前限制

- 当前 Android 侧真实采样仍然主要使用 Kotlin + `TrafficStats`，不是 Rust core。
- 当前统计更接近“设备总上下行”的 MVP，不是完整的多接口精细采样器。
- 当前 CI 会使用临时 debug keystore 给 release APK 签名。
- 由于每次 CI 生成的 debug keystore 都不同，用户升级新版本前通常需要先卸载旧版本。

## 说明

这个项目目前是实验性质，先以 Android 为主，不先碰 iOS。
