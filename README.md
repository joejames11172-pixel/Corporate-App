# Peostectica — Corporate App Build Workspace

This GitHub repository is a **build and source mirror** of the relevant Google Drive corporate directory hierarchy. It is **not** the authoritative file store.

**Authoritative root:** Google Drive / GPTChat (mounted on Alpine at /mnt/google-drive)
**Path mapping:** Google Drive / GPTChat / Development / Apps / HereShift / Android App → this repo's `Development/Apps/HereShift/Android App/`.

## Governance
- Preserve the corporate folder names, nested structure, existing Drive metadata/objectives and historical records.
- NEVER replicate private accounts, passwords, API keys, TLS material, identity/employee files, user data, or unreviewed restricted records to GitHub.
- GitHub contains only explicitly approved project build inputs and safe, minimal metadata or folder placeholders. Paths here DO NOT imply full-file synchronization from Drive.
- Production company data, checkpoints, approvals, and corporate objectives live on Google Drive.
- Each new Android app gets its own `Development/Apps/<AppName>/Android App/` and corresponding project folder; shared starters are under `Development/Apps/_template/`.
- No automated GitHub → Google Drive overwrite or deletion.
- Build artifacts are debug/test only until signed, independently audited, and owner-authorized.

## Current projects
- [HereShift](Development/Apps/HereShift/)
- [TapVoyage](Development/Apps/TapVoyage/)
- [Reusable app template](Development/Apps/_template/)

The repository started empty. This initial README intentionally establishes the safe root and does not copy unrelated Drive content.
