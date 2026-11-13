# Rebrand audit — Onyx Gestures

Date: 2026-11-13

## Inventory

- [x] Remote (legacy): `GustavoCaruzoGoncalves/moto-actions-poco-f7`
- [x] Target: `GustavoCaruzoGoncalves/onyx-gestures`
- [x] Branches: `main`, `feature/twist-classifier-research`
- [x] Tags: `v1.1.2` … `v1.2.3` (+ `v1.3.0` on publish)
- [x] Releases: recreate on new repo with identical historical ZIPs (SHA-256 verified)

## Commercial / branding refs

- [x] README / docs: product identity → Onyx Gestures; OEM names only in disclaimer / technical background
- [x] UI strings: app name Onyx Gestures; neutral gesture descriptions
- [x] module.prop / updateJson → onyx-gestures URLs; **module id kept** `f7_moto_actions`
- [x] ZIP filename kept `f7_moto_actions-*.zip` for upgrade / API match
- [x] applicationId kept `org.f7.motoactions` for upgrade compatibility
- [x] KernelSU button: package launch only (no GitHub fallback)
- [x] Root: `su` probe + contextual KernelSU Manager authorize message

## Preserved

- Third-party licenses / NOTICE attributions
- Git history (no history rewrite)
- Module ID and signing keystore for in-place updates
