#!/bin/zsh
# Builds the reference app for a device, signed with the team's development identity and wildcard profile.
# Usage: build-device.sh <profile.mobileprovision> <signing identity>
set -e
cd "$(dirname "$0")"
profile=$1
identity=$2
rm -rf build/device/GlassReference.app
mkdir -p build/device/GlassReference.app
plutil -replace CFBundleSupportedPlatforms -json '["iPhoneOS"]' -o build/device/GlassReference.app/Info.plist Info.plist
xcrun --sdk iphoneos swiftc -parse-as-library -O -target arm64-apple-ios26.0 \
    GlassReference.swift -o build/device/GlassReference.app/GlassReference
cp "$profile" build/device/GlassReference.app/embedded.mobileprovision
team=$(security cms -D -i "$profile" | plutil -extract TeamIdentifier.0 raw -)
cat > build/device/entitlements.plist <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>application-identifier</key><string>$team.tv.quven.glass.reference</string>
<key>com.apple.developer.team-identifier</key><string>$team</string>
<key>get-task-allow</key><true/>
</dict></plist>
PLIST
codesign --force --sign "$identity" --entitlements build/device/entitlements.plist build/device/GlassReference.app
