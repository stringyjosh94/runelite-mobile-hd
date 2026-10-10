# Contributing

This is an experimental AI-generated integration of established third-party projects. Independent code review, testing and clear bug reports are valuable. Upstream authors retain credit for their work.

1. Read [Project map](docs/PROJECT-MAP.md) and [Architecture](docs/ARCHITECTURE.md).
2. Restore dependencies and build using [Building](docs/BUILDING.md).
3. Make a focused change, explain its purpose and run the relevant desktop checks.
4. Test affected behaviour on Android and report the exact device/build/settings. If no device test is available, say so.
5. Keep credentials, signing keys, generated binaries and personal logs out of commits.

Retain upstream licences and file notices. Keep the HD fork's `dev.mobile.rlhd` package and `mobilehd` configuration identity separate from desktop 117HD. Do not claim desktop GL features the actual GLES driver lacks. Dependency updates can change client field mappings and plugin APIs; update regression fixtures carefully.

AI assistance is allowed. Explain substantial generated changes, review them and record validation. Do not present generated code or successful compilation as a substitute for understanding or testing.

Use the device-report issue template for bugs. Include reproduction steps, expected/actual behaviour, device/Android/app/plugin versions and redacted diagnostics. See [Security](SECURITY.md) before reporting anything involving tokens or account access.
