# Security Policy

## Supported versions

Only the latest GitHub Release of this module is considered for security fixes.

## Reporting a vulnerability

Please open a **private** GitHub security advisory on this repository, or contact the maintainer via GitHub if advisories are unavailable.

Include:

- Module version / commit
- Device model and ROM (no serial numbers)
- Reproduction steps
- Impact assessment

Do **not** file public issues for privilege-escalation or remote-exploit reports before coordinated disclosure.

## Trust boundaries

- The module runs with **root** hooks via KernelSU `service.sh` (camera helper, keep-alive).
- The APK requests camera, foreground-service, wake-lock, and notification permissions.
- A project signing keystore under `tools/module.keystore` is used to sign release APKs for sideload/module packaging. The store password is public in `app/build.gradle.kts` for reproducible community builds; **do not** reuse this keystore for unrelated apps or publish additional secrets into it.
- SELinux remains Enforcing on the tested device; this project does not ship `setenforce 0`.

## Out of scope

- Magisk / APatch-specific privilege models (untested)
- Third-party camera apps’ own vulnerabilities
