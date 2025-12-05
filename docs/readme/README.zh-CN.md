<p align="center">
  <img src="../../icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

# Calisthenics Memory

---

🌐 [English](../../README.md) | [日本語](README.ja.md) | [Deutsch](README.de.md) | [Español](README.es.md) | [Français](README.fr.md) | [Italiano](README.it.md)

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

## 关于

自重训练记录应用。创建自定义动作，按组别和级别整理，跟踪进度 - 完全离线运行。

## 功能

- **主页仪表板** - 一目了然查看今日训练记录，长按复制
- **完全可自定义** - 自由创建动作，按组别整理，10级管理，按动作记录距离和重量
- **收藏夹** - 快速访问常用动作
- **两种记录模式**
  - 记录模式：使用"应用动作设置"按钮快速手动输入
  - 训练模式：按动作设置计时器（休息间隔、每次时长）的自动引导训练，组完成时LED闪光通知
- **进度跟踪** - 以列表、图表或挑战进度条查看记录
- **单侧/双侧支持** - 单侧动作可分别记录左右两侧
- **挑战目标** - 设置目标组数×次数并跟踪达成状态
- **数据管理** - 以 JSON 或 CSV 格式导出/导入（完整备份支持）
- **多语言** - 英语、日语、西班牙语、德语、简体中文、法语、意大利语
- **隐私优先** - 完全离线运行，无危险权限，无网络访问

## 截图

<p align="center">
  <img src="../../screenshots/1.png" width="250">
  <img src="../../screenshots/2.png" width="250">
  <img src="../../screenshots/3.png" width="250">
</p>
<p align="center">
  <img src="../../screenshots/4.png" width="250">
  <img src="../../screenshots/5.png" width="250">
  <img src="../../screenshots/6.png" width="250">
</p>

## 系统要求

- **Android** 8.0（API 26）或更高版本
- **存储** 约 10MB
- **网络** 不需要

## 权限

- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `WAKE_LOCK` - 在后台运行计时器
- `FLASHLIGHT` - 休息间隔的闪光通知

## 构建

```bash
git clone https://codeberg.org/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

需要 JDK 17 或更高版本。

## 许可证

本项目采用 GNU 通用公共许可证 v3.0。详见 [LICENSE](../../LICENSE)。

---

**最后更新**: 2025年12月6日