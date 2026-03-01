<p align="center">
  <img src="../../icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

<h1 align="center">Calisthenics Memory</h1>

<p align="center">
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/README.md">English</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-green.svg" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue.svg" alt="License: GPL-3.0">
  <a href="https://shields.rbtlog.dev/io.github.gonbei774.calisthenicsmemory"><img src="https://shields.rbtlog.dev/simple/io.github.gonbei774.calisthenicsmemory" alt="Reproducible Builds"></a>
</p>

## 概要

プライバシー重視の自重トレーニング記録アプリ。すべてを自分好みにカスタマイズ。

## 機能

- **To Do** - ワークアウトを計画、タップで直接ジャンプ、曜日リピート設定
- **記録モード** - 手動でサクッと記録
- **ワークアウトモード** - タイマーで自動ガイド、セット完了通知
  - シングル - 1種目に集中してトレーニング
  - プログラム - 複数種目のルーティンを作成・実行、柔軟なナビゲーション
  - インターバル - ワーク/レストのタイマーサイクル、ラウンド数カスタマイズ
- **種目作成** - 動的/静的、片側/両側、インターバルなど細かく設定
- **進捗追跡** - カレンダー、一覧、グラフ、課題達成状況
- **データ管理** - JSON/CSVでエクスポート・インポート

## スクリーンショット

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

## 公式配布先について

このアプリの公式配布先はF-Droid、IzzyOnDroid、Codeberg Releasesのみです。
上記以外のサイトからダウンロードしたAPKの安全性は保証できません。

<p align="center">
  <a href="https://apt.izzysoft.de/fdroid/index/apk/io.github.gonbei774.calisthenicsmemory"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButtonGreyBorder_nofont.png" height="80" alt="IzzyOnDroidで入手"></a>
</p>

<p align="center">
  <a href="https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory/"><img src="https://fdroid.org/badge/get-it-on.png" height="119" alt="F-Droidで入手"></a>
</p>

<p align="center">
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/releases"><img src="https://get-it-on.codeberg.org/get-it-on-white-on-black.png" height="80" alt="Codebergで入手"></a>
</p>

### 署名の確認

APKの署名フィンガープリント（SHA-256）:

```
18c00c347ea1001afcdd87258881d24d684047bbeb22c47fbe7b51499516ab54
```

確認方法:

```bash
apksigner verify --print-certs app-release.apk
```

### チェックサムの確認

SHA256チェックサム: リリースページの `app-release.apk.sha256` を参照

```bash
sha256sum -c app-release.apk.sha256
```

## 権限

- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `WAKE_LOCK` - バックグラウンドでタイマーを実行
- `FLASHLIGHT` - 休憩時間のフラッシュ通知

詳細は [IzzyOnDroid Permissions](https://android.izzysoft.de/applists/perms) を参照。

## コントリビュート

ガイドラインは [CONTRIBUTING.md](../../CONTRIBUTING.md) を参照。

## Wiki

- [はじめに](https://codeberg.org/Gonbei774/CalisthenicsMemory/wiki/Getting-Started_ja)

## ライセンス

このプロジェクトはGNU General Public License v3.0の下で公開されています。詳細は[LICENSE](../../LICENSE)をご覧ください。

---

**最終更新**: 2026年3月2日