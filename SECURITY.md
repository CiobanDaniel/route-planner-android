# Security policy

## Supported versions

| Version | Supported |
|---------|-----------|
| 0.5.x   | Yes |
| 0.4.x   | Best effort |
| 0.3.x   | Best effort |
| < 0.3   | No |

## Reporting a vulnerability

Please **do not** open a public GitHub issue for security problems.

Email the maintainer (see GitHub profile for **CiobanDaniel**) with:

- Description of the issue
- Steps to reproduce
- Impact assessment (if known)
- Whether you plan to disclose publicly and on what timeline

We aim to acknowledge reports within a few business days.

## Secrets

Never commit:

- Signing keystores / passwords (`*.jks`, `*.keystore`, `*.pepk`, `keystore.properties`)
- `google-services.json` / Firebase keys (including `google-services.json.bak`)
- API tokens / `.env`
- `local.properties`

Use CI secrets / local ignored files instead.
