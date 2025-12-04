<p align="center">
  <img src="../../icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

# Calisthenics Memory

一款简单、注重隐私的 Android 自重训练记录应用。

---

<div align="center">
<table>
<tr><td align="center">
<h3>⚠️ 此仓库已迁移 ⚠️</h3>
<p>开发已迁移至 <b><a href="https://codeberg.org/Gonbei774/CalisthenicsMemory">Codeberg</a></b></p>
<p>此 GitHub 仓库为<b>只读镜像</b>。<br>
如需最新代码、发布版本和贡献，请访问 Codeberg。<br>
为方便起见，此处也接受 Issue。</p>
</td></tr>
</table>
</div>

---

<p align="center">
  <a href="https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory/">
    <img src="https://fdroid.org/badge/get-it-on.png" alt="在 F-Droid 下载" height="80">
  </a>
</p>
<p align="center">
  <a href="https://apt.izzysoft.de/fdroid/index/apk/io.github.gonbei774.calisthenicsmemory">
    <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButton.png" alt="在 IzzyOnDroid 下载" height="54">
  </a>
</p>

---

🌐 [English](../../README.md) | [日本語](README.ja.md) | [Deutsch](README.de.md) | [Español](README.es.md) | [Français](README.fr.md) | [Italiano](README.it.md)

---

## 关于

Calisthenics Memory 帮助您记录和管理俯卧撑、引体向上、深蹲等自重训练。创建自定义动作，按渐进级别组织，并跟踪您的进步。

应用完全离线运行——无需网络连接，无广告，无追踪。您的数据仅保存在设备上。

## 要点

- **完全自定义** - 自定义动作无任何功能限制。次数/时间、单侧/双侧、目标、计时器 - 所有功能对每个动作都可用
- **两种记录模式** - 快速手动输入或带计时器的引导训练
- **仅离线使用** - 您的数据永远不会离开设备

## 功能

- **主页仪表板** - 一目了然查看今日训练记录，长按复制
- **完全可自定义** - 自由创建动作，按组别整理，10级管理，箭头按钮重新排序，按动作记录距离和重量
- **收藏夹** - 快速访问常用动作
- **两种记录模式**
  - 记录模式：使用"应用动作设置"按钮快速手动输入
  - 训练模式：按动作设置计时器（休息间隔、每次时长）的自动引导训练，组完成时LED闪光通知
- **进度跟踪** - 以列表、图表或挑战进度条查看记录
- **单侧/双侧支持** - 单侧动作可分别记录左右两侧
- **挑战目标** - 设置目标组数×次数并跟踪达成状态
- **数据管理** - 以 JSON 或 CSV 格式导出/导入（完整备份支持）
- **多语言** - 英语、日语、西班牙语、德语、简体中文、法语、意大利语
- **隐私优先** - 完全离线运行，无运行时权限，无网络访问

## 截图

<p align="center">
  <img src="../../screenshots/1.png" width="250"><br>
  <b>主页</b> - 一目了然的今日训练
</p>

<p align="center">
  <img src="../../screenshots/2.png" width="250"><br>
  <b>动作</b> - 用组别和收藏整理
</p>

<p align="center">
  <img src="../../screenshots/3.png" width="250"><br>
  <b>记录</b> - 快速手动输入
</p>

<p align="center">
  <img src="../../screenshots/4.png" width="250"><br>
  <b>训练</b> - 带计时器的引导训练
</p>

<p align="center">
  <img src="../../screenshots/5.png" width="250"><br>
  <b>图表</b> - 跟踪您的进步
</p>

<p align="center">
  <img src="../../screenshots/6.png" width="250"><br>
  <b>挑战</b> - 目标达成状态
</p>

## 系统要求

- **Android** 8.0（API 26）或更高版本
- **存储** 约 10MB
- **网络** 不需要

## 权限

本应用仅使用**普通权限（安装时权限）**，这些权限在安装时自动授予，无需用户确认。

截至 v1.9.0，包含以下权限：

| 权限 | 用途 | 添加者 | 源码 |
|------|------|--------|------|
| `FOREGROUND_SERVICE` | 将训练计时器作为前台服务运行 | 应用（v1.9.0） | [WorkoutTimerService.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `FOREGROUND_SERVICE_SPECIAL_USE` | 训练计时器前台服务类型 | 应用（v1.9.0） | [WorkoutTimerService.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `WAKE_LOCK` | 屏幕关闭时保持计时器运行 | 应用（v1.8.1） | [WorkoutTimerService.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `FLASHLIGHT` | 训练模式中的LED闪光通知 | 应用（v1.8.0） | [FlashController.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/util/FlashController.kt) |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | 内部组件的安全保护 | AndroidX 库（自动） | - |

### 什么是普通权限？

Android 将权限分为两类：
- **普通权限**：安装时自动授予的低风险权限。用户无法单独撤销。
- **危险权限**：需要用户明确批准的高风险权限（如：相机、位置、通讯录）。

本应用不请求任何运行时权限。

更多信息：
- [Android 权限类型概述](https://developer.android.com/guide/topics/permissions/overview)
- [普通权限完整列表](https://developer.android.com/reference/android/Manifest.permission)

### 注意

普通权限会自动授予，可能不会显示在应用商店列表中。为了透明起见，我们在此记录。

## 构建

```bash
git clone https://codeberg.org/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

需要 JDK 17 或更高版本。

## 许可证

本项目采用 GNU 通用公共许可证 v3.0。详见 [LICENSE](../../LICENSE)。