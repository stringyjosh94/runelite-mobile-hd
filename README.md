<img width="192" height="192" alt="image" src="https://github.com/user-attachments/assets/4e4713ab-5fc0-4b78-a994-b7c86aa3f144" />

# Runelite Mobile (HD)


https://github.com/user-attachments/assets/9ab506fa-9894-4857-af6a-55c97527190a


An unofficial, experimental Android adaptation of RuneLite with touch controls and a modified 117HD renderer. It runs an embedded Java runtime and native OpenGL ES graphics inside the app. A separate Linux installation is not required.

This is a personal education and hobby project, started out of enthusiasm for Old School RuneScape. The maintainer supplied ideas, direction and device testing. Project-specific implementation and integration were generated with AI coding tools. The underlying clients, libraries, renderer and artwork come from their respective creators; this project does not claim to have created them. AI-generated changes can contain mistakes and need independent review.

## Install and play

Install the APK from this repository's **Releases** page. Follow [Getting started](docs/GETTING-STARTED.md), including the [desktop login-file tutorial](docs/LOGIN.md). After setup, the app starts the client automatically. Android Back opens the app menu.

The current release candidate is **v35**. It has Low / Medium / High graphics profiles, a Mobile HD launcher icon, manual sidebar enlargement and a simplified setup/menu. See [Release notes](docs/RELEASE-NOTES.md).

## Compatibility

The APK requires Android 8 or newer, a 64-bit ARM Android installation and a working OpenGL ES 3.2 driver. Meeting these requirements does not guarantee comfortable gameplay.

Testing has been limited to a Pixel 10a and Lenovo Tab M10 (3rd Gen), with different development builds and settings. Performance, heat, map coverage and plugin compatibility have not been established across Android devices. No minimum FPS, universal compatibility or account safety is promised. See [Device testing](docs/DEVICE-TESTING.md) and [Plugin compatibility](docs/PLUGIN-COMPATIBILITY.md).

## Work on the source

- [Build an APK](docs/BUILDING.md)
- [Find the relevant files](docs/PROJECT-MAP.md)
- [Understand the renderer and bridge](docs/ARCHITECTURE.md)
- [Graphics profiles and device starting points](docs/GRAPHICS-PROFILES.md)
- [Contribute or report a problem](CONTRIBUTING.md)
- [Publish the source and release assets](docs/PUBLISHING.md)

The repository contains source, resources, build scripts, tests and licence notices. Large build dependencies are supplied as a separate release download. APKs, credentials, signing keys and generated build folders do not belong in Git.

## Credits and terms

Built on [RuneLite](https://github.com/runelite/runelite), [117HD](https://github.com/117HD/RLHD), OpenJDK, Caciocavallo, Fold Craft Launcher runtime components, LWJGL and other open-source projects. [zeruth/runelite-android](https://github.com/zeruth/runelite-android) informed the touch and credential-import workflow.

Read [Credits](CREDITS.md), [Third-party components](THIRD-PARTY.md), [Legal disclaimer](LEGAL.md) and [Security](SECURITY.md). This project is not affiliated with, endorsed by or an official release of Jagex, RuneLite or 117HD. Its approval as a separate game client has not been established.
