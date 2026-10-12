# Update the existing GitHub repository

Use the `runelite-mobile-hd` folder containing `README.md`, `app`, `hd-plugin`, `docs`, `tests` and `tools`. Do not upload the surrounding workspace.

## Review and push source changes

This checkout already uses branch `main` and remote `https://github.com/stringyjosh94/runelite-mobile-hd.git`. There is no need to initialise Git or add the remote again.

```sh
python3 tools/package-source.py --check
git status --short
git --no-pager diff --stat
git --no-pager diff
```

Review new files shown by `git status` as well; ordinary `git diff` does not display their contents. Keep credentials, signing keys, APKs, logs and build dependencies out of the commit. The packaging check checks filenames and sizes, not every possible secret in file contents.

When the changes are ready:

```sh
git add -A
git --no-pager diff --cached --stat
git commit -m "Prepare v36.4 touch controls and Android notifications"
git push origin main
```

`--no-pager` prevents Git from opening a scrolling viewer. If a viewer is already open, press `q` to return to the terminal.

If Git requests an identity, set `git config user.name "Your Name"` and `git config user.email "Your GitHub email"`, then repeat the commit. These commands set the identity for this repository only. HTTPS authentication uses a supported credential manager or a GitHub personal access token instead of an account password. Never place tokens in repository files or remote URLs. GitHub CLI is not required.

If Git rejects the push because the remote has new commits, fetch and inspect those changes before merging them. Do not force-push over another change.

## Upload release downloads manually

On GitHub, open **Releases → Draft a new release**. Create tag `v36.4` targeting the updated `main` branch. Suggested title: **v36.4 — Touch controls and Android notifications**. Use [Release notes](RELEASE-NOTES.md) for the description.

| Asset | Purpose |
|---|---|
| `runelite-mobile-hd-v36.4-touch-arm64-v8a.apk` | Android installation |
| `runelite-mobile-hd-runtime-v35.zip` | Dependencies required to build the source |
| `runelite-mobile-hd-source-v36.4.zip` | Optional source snapshot |
| `runelite-mobile-hd-release-v36.4.sha256` | Download integrity checks |

The runtime pack keeps its v35 name because v36.4 reuses those dependencies. Include it as a release asset or clearly link to the existing download. Large archives and APKs belong in Releases, not source commits. Mark the release as a pre-release while device testing remains limited.

## Prepare a source archive

```sh
python3 tools/package-source.py
```

Output goes to ignored `dist/`. The archive includes Git-visible source and new non-ignored files. It does not upload anything.

Do not regenerate the runtime pack for a documentation update. `tools/package-runtime.py` changes its manifest and checksum and should only be used when deliberately publishing a verified dependency snapshot.

## Before publishing

- Test installation and updates, credentials, touch controls, keyboard, sidebar, profiles and gameplay notifications.
- Record the tested devices, build and settings without promising a minimum FPS.
- Check signing identity, version, download hashes and private-data exclusions.
- Preserve upstream notices and review [Legal](../LEGAL.md), [Third-party components](../THIRD-PARTY.md) and [Security](../SECURITY.md).
- Back up the signing key outside Git. A different key cannot update an existing installation.
