#!/usr/bin/env bash
# AstroDock - jar ビルドスクリプト（Forge 1.20.1）
#
# このスクリプトは Gradle ラッパー経由で mod の jar を作成します。
# 注意: 最初の実行では Gradle と Forge / Minecraft の外部依存をネットワークから
# ダウンロードする必要があります（services.gradle.org, maven.minecraftforge.net,
# maven.neoforged.net, maven.parchmentmc.org）。オフライン環境ではビルドできません。

set -euo pipefail
cd "$(dirname "$0")"

# Java 17 は Gradle のツールチェーン経由で自動プロビジョニングされます。
# 手動で用意したい場合はプロジェクト外のビルドに合わせて下記を設定してください。
# 例: export JAVA_HOME=/path/to/jdk-17

echo "== AstroDock: jar ビルド =="
echo "   Forge 1.20.1 用の Mod 'astrodock'"

# gradle-wrapper があればそれを使う（推奨）
if [ -f ./gradlew ]; then
    ./gradlew --no-daemon build
else
    echo "gradlew が見つかりません。システムの gradle を使います。"
    gradle --no-daemon build
fi

echo
echo "== ビルド完了 =="
echo "jar は build/libs/ に 作成されます。"
ls -1 build/libs/ 2>/dev/null | sed 's/^/   /' || echo "   (build/libs/ が見つかりませんでした)"
