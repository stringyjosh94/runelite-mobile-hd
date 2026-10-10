# Put the source on GitHub

Upload the repository root only: the folder containing `README.md`, `app`, `hd-plugin`, `docs`, `tests` and `tools`. Do not upload the surrounding development workspace.

## 1. Check the upload list

In that folder (initialising Git is safe for an existing local repository):

```sh
git init -b main
python3 tools/package-source.py --check
git status --short
```

The ignore rules exclude runtime dependencies, generated builds, APKs, private keys, credentials and local logs. The packaging check checks obvious filenames and size limits; it cannot identify every possible secret. Read the files/diff before publishing. Preserve licence headers and vendored source.

## 2. Create an empty repository

On GitHub, choose **New repository**, enter a name and choose visibility. Do not initialise it with a README, licence or .gitignore: this project already has them. Copy the HTTPS repository URL.

## 3. Commit and push

Run these commands in the source folder. Replace `YOUR-NAME/YOUR-REPOSITORY` with the repository you created:

```sh
git init -b main
git add .
git diff --cached --stat
git commit -m "Prepare experimental Android client source"
git remote add origin https://github.com/YOUR-NAME/YOUR-REPOSITORY.git
git push -u origin main
```

An already-initialised local repository can report that it was reinitialised; its files are retained. If an origin remote already exists, inspect `git remote -v` and use `git remote set-url origin YOUR-URL` only when replacing it intentionally. Do not force-push to bypass a non-empty remote.

If Git asks who you are, set your name/email using `git config user.name` and `git config user.email`, then repeat the commit. Authenticate using GitHub's supported browser/credential-manager/token flow; your GitHub account password is not a Git HTTPS password. Do not put tokens into repository URLs, files or shell commands.

Official reference: [GitHub's local-code upload guide](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github).

For later changes:

```sh
git add .
git diff --cached --stat
git commit -m "Describe the change"
git push
```

## 4. Add release downloads manually

Create a GitHub release with tag `v35`, use [Release notes](RELEASE-NOTES.md) as the description, and attach:

| Asset | Needed by |
|---|---|
| `runelite-mobile-hd-v35.apk` | People installing the app |
| `runelite-mobile-hd-runtime-v35.zip` | Contributors building the source |
| `runelite-mobile-hd-source-v35.zip` | Optional source-only download; GitHub also generates source archives |
| `runelite-mobile-hd-release-v35.sha256` | People checking download integrity |

The runtime pack is required for the documented fresh-source build. Uploading only an APK leaves contributors without those pinned dependencies. Keep large binary ZIPs in Releases, not Git commits. Mark this initial experimental build as a **pre-release** until the device checklist and distribution review are complete.

## Maintainer checklist

- Test a fresh source restore/build using the published pack.
- Test fresh install and update, import/cancel/clear/replacement, touch, keyboard, sidebar, profile switching and launcher masks on Android.
- Describe the actual small device sample; do not promise comfortable operation on all phones.
- Inspect APK, source and pack for private data and check signing identity/version/hash.
- Preserve licence notices and verify corresponding-source/redistribution obligations and branding permissions. Read [Legal](../LEGAL.md) and [Third-party components](../THIRD-PARTY.md); a disclaimer is not permission.
- Keep signing keys backed up outside Git. Enable private vulnerability reporting if you can maintain it.
- Publish APKs manually; source packaging and local checks do not upload anything.

## Recreate distribution archives

From a checkout with its dependencies restored:

```sh
python3 tools/package-runtime.py
python3 tools/package-source.py
```

The runtime tool updates `runtime-manifest.json` and `runtime-pack.sha256`, so run it before exporting the matching source. Output goes to ignored `dist/`. Do not create a runtime pack from unknown or partially updated dependencies.
