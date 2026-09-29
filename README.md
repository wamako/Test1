# Core Patch N

Core Patch N is the official successor to the original Core Patch module, which is an Xposed module that modifies the behavior of Android's package manager service. It can bypass app downgrade restrictions, APK signature checks, and other installation restrictions.

## Requirements

- Android 9 and above
- Xposed framework which supports libxposed API 101

## AxManager package

This project now generates an AxManager-compatible module ZIP:

- `app/build/outputs/axmanager/corepatchn-axmanager-v<version>.zip`

Use that ZIP in AxManager (do not use the source-code ZIP), then reboot.

## License

This project follows GNU General Public License v2.0.
