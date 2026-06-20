# android-see-netflow

局域网内查看 Android 设备网卡流量的实验项目。

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
