<p align="center">
  <img src="../../icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

# Calisthenics Memory

---

🌐 [English](../../README.md) | [Deutsch](README.de.md) | [Español](README.es.md) | [Français](README.fr.md) | [Italiano](README.it.md) | [简体中文](README.zh-CN.md)

---

[![Build Status](https://ci.codeberg.org/api/badges/15682/status.svg)](https://ci.codeberg.org/repos/15682)
[![RB Status](https://shields.rbtlog.dev/simple/io.github.gonbei774.calisthenicsmemory)](https://shields.rbtlog.dev/io.github.gonbei774.calisthenicsmemory)
<br>
[![IzzyOnDroid Downloads](https://img.shields.io/badge/dynamic/json?url=https://dlstats.izzyondroid.org/iod-stats-collector/stats/basic/monthly/rolling.json&query=$.['io.github.gonbei774.calisthenicsmemory']&label=IzzyOnDroid%20monthly%20downloads)](https://apt.izzysoft.de/packages/io.github.gonbei774.calisthenicsmemory)

---

<p align="center">
  <a href="https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory/">
    <img src="https://fdroid.org/badge/get-it-on.png" alt="F-Droidで入手" height="80">
  </a>
</p>
<p align="center">
  <a href="https://apt.izzysoft.de/fdroid/index/apk/io.github.gonbei774.calisthenicsmemory">
    <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButton.png" alt="IzzyOnDroidで入手" height="54">
  </a>
</p>

---

## 概要

自重トレーニング記録アプリ。カスタム種目を作成し、グループとレベルで整理し、進捗を追跡。完全オフラインで動作します。

## 機能

- **ホームダッシュボード** - 今日のトレーニング記録を一覧表示、長押しでコピー
- **完全カスタマイズ** - 種目を自由に作成、グループで整理、10段階のレベル管理、種目ごとに距離と荷重を記録
- **お気に入り** - よく使う種目にすぐアクセス
- **2つの記録モード**
  - 記録モード: 「種目設定を適用」ボタンで素早く手動入力
  - ワークアウトモード: 種目ごとのタイマー設定（休憩時間、レップ時間）による自動ガイド、セット完了時のLEDフラッシュ通知
- **プログラム** - 複数種目のルーティンを作成、タイマーON（カウントダウン）またはタイマーOFF（自分のペース）モード、セット間のインターバル設定
- **進捗追跡** - 記録を一覧、グラフ、課題の達成状況で確認
- **片側/両側対応** - 片側種目では左右を別々に記録
- **課題目標** - 目標セット数×回数を設定し、達成状況を追跡
- **データ管理** - JSON/CSV形式でエクスポート・インポート（完全バックアップ対応）
- **多言語対応** - 英語、日本語、スペイン語、ドイツ語、中国語（簡体字）、フランス語、イタリア語
- **プライバシー重視** - 完全オフライン動作、危険な権限なし、インターネットアクセスなし

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

## 動作環境

- **Android** 8.0（API 26）以上
- **ストレージ** 約10MB
- **インターネット** 不要

## 権限

- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `WAKE_LOCK` - バックグラウンドでタイマーを実行
- `FLASHLIGHT` - 休憩時間のフラッシュ通知

## ビルド方法

```bash
git clone https://codeberg.org/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

JDK 17以上が必要です。

## ライセンス

このプロジェクトはGNU General Public License v3.0の下で公開されています。詳細は[LICENSE](../../LICENSE)をご覧ください。

---

**最終更新**: 2025年12月6日