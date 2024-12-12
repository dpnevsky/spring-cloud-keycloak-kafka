#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
for module in CvsAppConfigServer apigateway ms-country ms-curriculum-vitae; do
    (cd "$module" && sh ./mvnw -B -ntp test)
done
for module in eureka-server ms-identity; do
    (cd "$module" && sh ./gradlew --no-daemon test)
done
