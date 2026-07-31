# Security policy

## Supported versions

| Version | Supported |
|---------|-----------|
| 0.3.x   | Yes |
| 0.2.x   | Best effort |
| < 0.2   | No |

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

- Signing keystores / passwords
- `google-services.json` / Firebase keys
- API tokens
- `local.properties`

Use CI secrets / local ignored files instead.
