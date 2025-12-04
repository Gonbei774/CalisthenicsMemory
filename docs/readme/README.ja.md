<p align="center">
  <img src="../../icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

# Calisthenics Memory

シンプルでプライバシー重視の自重トレーニング記録アプリ（Android向け）

---

<div align="center">
<table>
<tr><td align="center">
<h3>⚠️ このリポジトリは移転しました ⚠️</h3>
<p>開発は<b><a href="https://codeberg.org/Gonbei774/CalisthenicsMemory">Codeberg</a></b>に移行しました</p>
<p>このGitHubリポジトリは<b>読み取り専用ミラー</b>です。<br>
最新のコード、リリース、コントリビューションはCodebergをご覧ください。<br>
イシューはこちらでも受け付けています。</p>
</td></tr>
</table>
</div>

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

🌐 [English](../../README.md) | [Deutsch](README.de.md) | [Español](README.es.md) | [Français](README.fr.md) | [Italiano](README.it.md) | [简体中文](README.zh-CN.md)

---

## 概要

Calisthenics Memoryは、腕立て伏せ、懸垂、スクワットなどの自重トレーニングを記録・管理するアプリです。カスタム種目を作成し、段階的なレベルで整理し、進捗を追跡できます。

完全オフラインで動作します。インターネット接続不要、広告なし、トラッキングなし。データは端末内にのみ保存されます。

## 特徴

- **完全カスタマイズ** - カスタム種目に制限なし。回数/時間、片側/両側、目標、タイマー設定がすべての種目で利用可能
- **2つの記録モード** - 素早い手動入力またはタイマー付きガイドワークアウト
- **オフライン専用** - データは端末から外に出ません

## 機能一覧

- **ホームダッシュボード** - 今日のトレーニング記録を一覧表示、長押しでコピー
- **完全カスタマイズ可能** - 種目を自由に作成、グループで整理、10段階のレベル管理、矢印ボタンで並び替え、種目ごとに距離と荷重を記録
- **お気に入り** - よく使う種目にすぐアクセス
- **2つの記録モード**
  - 記録モード: 「種目設定を適用」ボタンで素早く手動入力
  - ワークアウトモード: 種目ごとのタイマー設定（休憩時間、レップ時間）による自動ガイド、セット完了時のLEDフラッシュ通知
- **進捗追跡** - 記録を一覧、グラフ、課題の達成状況で確認
- **片側/両側対応** - 片側種目では左右を別々に記録
- **課題目標** - 目標セット数×回数を設定し、達成状況を追跡
- **データ管理** - JSON/CSV形式でエクスポート・インポート（完全バックアップ対応）
- **多言語対応** - 英語、日本語、スペイン語、ドイツ語、中国語（簡体字）、フランス語、イタリア語
- **プライバシー重視** - 完全オフライン動作、実行時権限なし、インターネットアクセスなし

## スクリーンショット

<p align="center">
  <img src="../../screenshots/1.png" width="250"><br>
  <b>ホーム</b> - 今日のトレーニングを一目で確認
</p>

<p align="center">
  <img src="../../screenshots/2.png" width="250"><br>
  <b>種目</b> - グループとお気に入りで整理
</p>

<p align="center">
  <img src="../../screenshots/3.png" width="250"><br>
  <b>記録</b> - 素早い手動入力
</p>

<p align="center">
  <img src="../../screenshots/4.png" width="250"><br>
  <b>ワークアウト</b> - タイマー付きガイド
</p>

<p align="center">
  <img src="../../screenshots/5.png" width="250"><br>
  <b>グラフ</b> - 進捗を追跡
</p>

<p align="center">
  <img src="../../screenshots/6.png" width="250"><br>
  <b>課題</b> - 目標達成状況
</p>

## 動作環境

- **Android** 8.0（API 26）以上
- **ストレージ** 約10MB
- **インターネット** 不要

## 権限

このアプリは**通常権限（インストール時権限）**のみを使用します。これらはインストール時に自動的に付与され、ユーザーへの確認ダイアログは表示されません。

v1.9.0時点で、以下の権限が含まれています：

| 権限 | 用途 | 追加元 | ソース |
|------|------|--------|--------|
| `FOREGROUND_SERVICE` | ワークアウトタイマーをフォアグラウンドサービスとして実行 | アプリ（v1.9.0） | [WorkoutTimerService.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `FOREGROUND_SERVICE_SPECIAL_USE` | ワークアウトタイマー用フォアグラウンドサービスタイプ | アプリ（v1.9.0） | [WorkoutTimerService.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `WAKE_LOCK` | 画面オフ時もタイマーを継続 | アプリ（v1.8.1） | [WorkoutTimerService.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `FLASHLIGHT` | ワークアウトモード中のLEDフラッシュ通知 | アプリ（v1.8.0） | [FlashController.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/util/FlashController.kt) |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | 内部コンポーネントのセキュリティ保護 | AndroidXライブラリ（自動） | - |

### 通常権限とは？

Androidは権限を2種類に分類しています：
- **通常権限**: インストール時に自動付与される低リスクの権限。ユーザーが個別に取り消すことはできません。
- **危険権限**: 明示的なユーザー承認が必要な高リスクの権限（例：カメラ、位置情報、連絡先）。

このアプリは実行時権限を一切要求しません。

詳細情報：
- [Android権限タイプの概要](https://developer.android.com/guide/topics/permissions/overview)
- [通常権限の完全なリスト](https://developer.android.com/reference/android/Manifest.permission)

### 注意

通常権限は自動的に付与されるため、アプリストアの一覧に表示されないことがあります。透明性のため、ここに記載しています。

## ビルド方法

```bash
git clone https://codeberg.org/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

JDK 17以上が必要です。

## ライセンス

このプロジェクトはGNU General Public License v3.0の下で公開されています。詳細は[LICENSE](../../LICENSE)をご覧ください。