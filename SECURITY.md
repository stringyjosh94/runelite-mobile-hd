# Credentials and security

This experimental project has not received an independent security audit. AI-generated integration code should be reviewed before trusting it with account access.

## Login files

`credentials.properties` contains login tokens that can grant access without a password. Never commit it, attach it to an issue, send it to another person, or include it in an APK or dependency pack. Use the [login tutorial](docs/LOGIN.md).

The app stores its imported copy in private Android storage. Android backup and debugging are disabled in the release manifest. This is not a guarantee against a compromised device, unsafe plugin or operating-system flaw. After importing, remove the transferred copy from shared storage. Clearing the app's login does not revoke Jagex sessions or remove copies outside the app.

## Reports

Do not post credentials, tokens, account details or exploitable security details in a public issue. Use GitHub's private vulnerability reporting if the repository owner enables it. Otherwise ask the maintainer for a private reporting channel without including sensitive details. No private contact address is currently published in this source tree.

Inspect logs and screenshots before sharing. Plugin Hub plugins execute code in the client; install only plugins you trust. Git ignore rules and packaging checks reduce accidental inclusion of private files but are not a complete secret scanner.

## Maintaining releases

Keep signing keys private and backed up separately. Builds use a local development key unless a separate signing process is arranged. A new key cannot normally update an existing installation signed with another key. Dependency hashes establish file identity, not freedom from vulnerabilities. Dependencies are pinned and need deliberate updates and testing.
