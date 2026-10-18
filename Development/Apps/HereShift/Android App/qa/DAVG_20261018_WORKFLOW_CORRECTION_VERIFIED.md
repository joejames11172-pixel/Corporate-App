# DAVG — workflow defect correction and restart — 2026-10-18

## Failure identified
Previous DAVG reports ended on a rejection, or restarted without correcting the rejection's actual cause. This is a process failure, not an acceptable completion condition.

## Root-cause correction FIRST
New policy qa/DAVG_FIX_THEN_RESTART_WORKFLOW.md at commit 9e6d212e4a227196287b37a5a811f1d01546a728 requires detect -> correct root cause -> verify original failure -> full restart -> independent acceptance, with defect IDs, exact evidence and no repeated identical rejected stopping points. Owner-controlled external dependencies are escalated explicitly while independent work continues.

## Verification SECOND
Regression tests qa/test_davg_fix_then_restart.py committed abaccf4e8c2bb8fd7e2c4f7498c9d0b0ba006ba1. Alpine fetched exact revision and ran Python unittest: six of six PASS, exit 0. These tests verify the policy text structure, not organizational adoption or application release success.

## Restart THIRD
Restart inputs: governing Forbidden, Mandatory, Founder and HereShift objectives, previous DA and logs. Compare remaining application defect IDs from earlier DAVG: Circle auth/revocation, Play Billing/verified entitlements, signing compatibility, installed-app screenshots/tests, five independent auditors and CEO authorization. These remain unresolved and require actual engineering. No signed release or independent acceptance claimed. This policy is committed in the HereShift GitHub source mirror, not yet confirmed propagated to corporate-wide Google Drive templates.

## Verdict
Workflow document and structural tests: PASS. Corporate-wide enforcement and HereShift production release: NOT VERIFIED / OPEN. Continue corrective engineering, do not terminate solely because a report is rejected.
