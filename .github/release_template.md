# android-see-netflow v__VERSION__

局域网内查看 Android 设备网卡流量的实验版本发布页。

## 下载

| 平台 / 架构 | ARMv7 | ARM64-v8a | x86_64 | Universal |
|------|------|------|------|------|
| **Android** | [![android-armv7](https://img.shields.io/badge/android-ARMv7.apk-7CB342.svg?logo=android)](__BASE_URL__/android-see-netflow-armeabi-v7a-v__VERSION__.apk) | [![android-arm64-v8a](https://img.shields.io/badge/android-ARM64--v8a.apk-168039.svg?logo=android)](__BASE_URL__/android-see-netflow-arm64-v8a-v__VERSION__.apk) | [![android-x86_64](https://img.shields.io/badge/android-x86__64.apk-43A047.svg?logo=android)](__BASE_URL__/android-see-netflow-x86_64-v__VERSION__.apk) | [![android-universal](https://img.shields.io/badge/android-universal.apk-3DDC84.svg?logo=android)](__BASE_URL__/android-see-netflow-universal-v__VERSION__.apk) |

## 说明

- `universal` 包含多个 ABI，体积更大，但安装最省事。
- 分架构 APK 体积更小，适合按设备架构单独下载。
- 当前 APK 未接入固定 release keystore。若旧版本与新版本签名不一致，安装前可能需要先卸载旧版本。

## 构建来源

- Workflow: `build-android.yml`
- Commit: `__COMMIT__`

## 当前范围

- Android 客户端已具备 Kotlin 侧 MVP：可输入 WebSocket 服务端地址，并通过前台服务持续上报 `TrafficStats` 采样结果。
- 服务端位于仓库中的 `server/`，由 Python + Textual 实现，已可展示真实设备的实时流量与在线状态。
- Rust core 已增加最小 JNI 导出函数，并补入 Android `.so` 构建/打包链路；后续继续完善真实接线与逻辑下沉。
