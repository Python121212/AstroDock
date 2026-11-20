# AstroDock

**AstroDock** は、Minecraft **1.20.1** と **Forge** 用に作られたスターター Mod プロジェクトです。
ForgeGradle による jar 生成環境、Mod 本体、言語ファイル(ja / en)を含んでいます。

> 📌 **このリポジトリは MDK（Mod Development Kit）スタイルの Gradle プロジェクトです。**
> Mod の jar は、ネットワーク接続が可能な環境でビルドすると `build/libs/` に生成されます。

---

## 📋 概要

| 項目 | 内容 |
|------|------|
| Minecraft | 1.20.1 |
| Mod ローダー | Forge（`javafml`）47.1.3 |
| ツールチェーン | Java 17 |
| ビルドツール | Gradle 8.2.1（Gradle Wrapper 付き） |
| Mod ID | `astrodock` |
| マッピング | Parchment（`2023.08.20-1.20.1`） |
| Mod バージョン | 1.0.0 |

---

## 🛠️ 必要環境

- **JDK 17 以上**（Gradle の Java ツールチェーンで自動設定する場合は不要ですが、手動で用意することもできます）
- **Gradle 8.2.1**（`gradlew` ラッパーが自分でダウンロードします。Gradle の公式インストール不要）
- **ネットワーク接続**：最初のビルドでは以下にアクセスして依存をダウンロードします
  - `https://services.gradle.org/`（Gradle 本体）
  - `https://plugins.gradle.org/`（ForgeGradle 等のプラグイン）
  - `https://maven.neoforged.net/`（NeoForged）
  - `https://maven.minecraftforge.net/`（Forge）
  - `https://maven.parchmentmc.org/`（Parchment マッピング）
- バイトコードの再変換には Minecraft 本体の Jar もダウンロードします。

> ⚠️ **重要**：このリポジトリを「ネットワーク遮断のサンドボックス」などでビルドすると依存関係の
> ダウンロードに失敗します。必ずネットワーク接続がある環境（例：ローカルPC・CI 環境）で
> `./gradlew build` を実行してください。

---

## 🚀 セットアップ＆ビルド

### 1. リポジトリを取得

```bash
git clone https://github.com/Python121212/AstroDock.git
cd AstroDock
```

### 2. jar を生成（推奨）

```bash
./build.sh
```

または直接 Gradle を呼び出します。

```bash
./gradlew build
```

> 💡 初回の実行では Gradle（約130 MB）と外部依存が `~/.gradle` にダウンロードされます。
> 二回目以降はキャッシュが使われるため高速です。

### 3. 成果物の場所

```
build/libs/astrodock-1.20.1-1.0.0.jar
```

`build/libs/` 内の jar は Minecraft の `mods/` フォルダにコピーすれば、
Forge 1.20.1 + ForgeGradle の開発環境でそのまま動作確認できます。

### 4. 通常起動（必要なら）

```bash
./gradlew runClient        # クライアント起動（開発用）
./gradlew runServer        # サーバー起動（開発用）
```

インゲームの `/mod` メニュー（Forge の Mod 一覧）で **AstroDock** が表示され、
ログに `AstroDock セットアップ完了！` の旨が出力されれば成功です。

---

## 📁 ディレクトリ構成

```
AstroDock/
├── build.gradle                     # ビルド定義（ForgeGradle）
├── gradle.properties                 # バージョン・Mod 設定
├── settings.gradle                   # プラグイン解決
├── gradlew / gradlew.bat            # Gradle ラッパー（推奨）
├── gradle/wrapper/gradle-wrapper.jar
├── build.sh                         # jar ビルド用スクリプト
├── src/main/java/com/astrodock/astrodock/
│   └── AstroDockMod.java             # Mod 本体（@Mod アノテーション）
├── src/main/resources/
│   ├── META-INF/mods.toml            # Forge メタデータ（${} はビルド時に展開）
│   ├── pack.mcmeta                   # リソースパック形式メタデータ
│   └── assets/astrodock/lang/        # en_us / ja_jp 言語ファイル
└── README.md                         # このファイル（日本語）
```

---

## 🔄 どう動いているか（簡潔な説明）

1. `build.gradle` 内の `plugins { id 'net.neoforged.gradle' }` で **ForgeGradle** を適用します。
2. `minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"` で
   Forge 1.20.1 の userdev を取得します。
3. `${}` で埋め込まれた `mods.toml`／`pack.mcmeta` は `ProcessResources` で
   `gradle.properties` の値に置き換えられて、最終的な jar に格納されます。
4. Gradle の Java ツールチェーンが **Java 17** を使用します。
5. jar は `reobfJar` タスクでバイトコードが再変換（Reobfuscation）されます。

---

## ✅ ビルド時のチェックリスト

- [ ] Gradle Wrapper 経由で `./gradlew build` が成功する
- [ ] `build/libs/astrodock-1.20.1-1.0.0.jar` が生成される
- [ ] `mods/` に jar を配置してゲームを起動し、Mod 一覧に `AstroDock` が表示される
- [ ] ログに `AstroDock セットアップ完了！` の表示がある

---

## 📝 変更履歴・今後やること

- [ ] 実際のクラス（Item/Block/Screen 等）の追加
- [ ] アセット（テクスチャ / サウンド / アイコン）の追加
- [ ] 日本語ログメッセージの国際化（lang ファイルの拡充）
- [ ] CI による自動ビルド（GitHub Actions）

---

## 📄 ライセンス

Mod 本体は `All Rights Reserved` と見なされています（`gradle.properties` の `mod_license` と
`src/main/resources/META-INF/mods.toml` の `license` を編集して変更してください）。
ビルド雛形は Forge MDK / Parchment マッピングを使用しています。
