#!/bin/zsh
# Builds the reference app for the iOS simulator into build/GlassReference.app.
set -e
cd "$(dirname "$0")"
rm -rf build/GlassReference.app
mkdir -p build/GlassReference.app
plutil -replace CFBundleSupportedPlatforms -json '["iPhoneSimulator"]' -o build/GlassReference.app/Info.plist Info.plist
xcrun --sdk iphonesimulator swiftc -parse-as-library -O \
    -target "$(uname -m)-apple-ios26.0-simulator" \
    GlassReference.swift -o build/GlassReference.app/GlassReference
cp ../../fonts/res/font/inter_variable.ttf build/GlassReference.app/
codesign --force --sign - build/GlassReference.app
