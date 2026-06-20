# android-see-netflow

局域网内查看 Android 设备网卡流量的实验项目。

[![Rust](https://img.shields.io/badge/Rust-core-000000?style=for-the-badge&logo=rust&logoColor=white)](https://www.rust-lang.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-android-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)

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
  - 计划使用 `Kotlin + Rust`
  - Kotlin 负责 Android 生命周期、前台服务、权限和联网
  - Rust 负责采样核心和协议数据生成

## 当前状态

现在仓库已经完成第一版骨架：

- `server/main.py`
  - 启动时强制检查 `config.yaml`
  - 如果没找到，会直接报错，并提示用户从 `config.example.yaml` 复制
  - 启动 WebSocket 服务并打开 Textual TUI
- `server/mock_client.py`
  - 本机模拟一个 Android 设备持续发送测试流量数据
- `client/android/`
  - Android 工程骨架
  - 含 `MainActivity` 和前台服务占位
- `client/rust-core/`
  - Rust 核心占位
  - 目前先生成示例 telemetry JSON

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
  - `android-see-netflow-android-armeabi-v7a-v0.1.0.apk`
  - `android-see-netflow-android-arm64-v8a-v0.1.0.apk`
  - `android-see-netflow-android-x86_64-v0.1.0.apk`
  - `android-see-netflow-android-universal-v0.1.0.apk`
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

`client/` 目前还是第一版工程骨架，还没有接上真实 Android 采样与 WebSocket 上报。

当前已有内容：

- `client/android/`
  - Android Studio 可继续接着完善
- `client/protocol/telemetry.md`
  - 约定了服务端与客户端之间的 JSON 消息格式
- `client/rust-core/`
  - 预留 Rust 动态库核心

后续准备继续完成：

1. Android 前台服务中真实采集网卡流量
2. Kotlin 连接 WebSocket 并向服务端持续上报
3. Rust 与 Kotlin 之间的 JNI 集成
4. 视情况把更多采样逻辑下沉到 Rust

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

## 说明

这个项目目前是实验性质，先以 Android 为主，不先碰 iOS。
