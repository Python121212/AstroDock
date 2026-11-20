# AstroDock

Minecraft **1.20.1**・Forge **47.3.0** 用の Mod です。現在は、ゲーム内機能を追加せず、Forge が読み込める JAR を作るための最小構成です。

## 手元で JAR を作る

JDK 17 をインストールし、`java -version` が 17 を示すことを確認してください。Gradle は同梱のラッパーが取得するため、別途インストールする必要はありません。初回は Gradle、Forge、Minecraft の依存ファイルをダウンロードするため、インターネット接続が必要です。

リポジトリのルートで、次のコマンドを実行します。

| 環境 | ビルドコマンド |
| --- | --- |
| macOS / Linux | `./gradlew clean build` |
| Windows（PowerShell） | `.\gradlew.bat clean build` |

成功すると `build/libs/astrodock-0.1.0.jar` が生成されます。Forge 47.3.0 を導入した Minecraft 1.20.1 のゲームディレクトリ内にある `mods` フォルダーへ JAR を入れて起動し、「Mods」一覧に **AstroDock** が表示されることを確認してください。

## 開発と自動ビルド

- 開発用クライアントは `./gradlew runClient`（Windows は `.\gradlew.bat runClient`）で起動できます。
- Mod のエントリーポイントは `src/main/java/io/github/python121212/astrodock/AstroDock.java`、表示情報は `src/main/resources/META-INF/mods.toml` にあります。バージョン番号は `gradle.properties` で管理します。
- プッシュすると GitHub Actions の「Mod のビルド」が実行されます。成功した実行の「Artifacts」から `AstroDock` をダウンロードし、ZIP を展開すると JAR を取り出せます。
