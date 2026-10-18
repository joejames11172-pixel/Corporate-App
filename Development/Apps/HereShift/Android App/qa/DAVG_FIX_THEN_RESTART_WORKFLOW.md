# DAVG Execution Controller — Correct BEFORE Restart

**Mandatory for every project.** A DAVG report is a control input, never a permission to stop. Do not publish an end-of-work report with REJECTED or BLOCKED as the final action when an actionable corrective task remains. Do not repeatedly restart the same unchanged failed state.

## State machine

1. **READ** governing Forbidden, Mandatory, Founder and project objectives, previous DA reports and logs. Maintain a unique defect ID and evidence for every open finding.
2. **EXECUTE** the current project work and gather reproducible evidence. If a gate fails, move to **CORRECT**, NOT to STOP and NOT directly to RESTART.
3. **CORRECT** each actionable finding's root cause in code, configuration, assets or process. Preserve originals and log the exact change and evidence. No cosmetic changes unrelated to the actual failing gate.
4. **VERIFY THE FIX** by reproducing the originally failing condition and checking it now passes. Run focused regression tests and relevant integration tests. If verification fails, return to **CORRECT** on the same defect. Do not mark it fixed based on source presence or a self-authored assertion alone.
5. **RESTART ONLY AFTER CORRECTION**: Forbidden -> Mandatory -> Founder objectives -> project objectives -> previous DA/logs -> execute all applicable gates. The restart must consume the corrected revision and explicitly compare defect IDs with prior cycle.
6. If a different or recurrent failure appears, **CORRECT -> VERIFY -> RESTART** again. Never stop at a REJECTED report while a feasible corrective action remains.
7. **SUCCESS** requires all acceptance gates passing with actual evidence and independent auditors. Only then is production promotion eligible; follow CEO approval rules.

## External dependency rule
If a defect cannot be fixed without an owner's credential, hardware, contractual account, third-party approval or access not available to the worker: record exact dependency, owner and proof, request only the necessary authorized action; continue all other independent corrections. Never invent signing keys, Play subscriptions, auditor approvals, device screenshots, test outcomes or deployment evidence. A dependency is not SUCCESS. A worker must not fabricate work or loop infinitely without executing fixes.

## Report contract
Each DAVG report MUST have: iteration/revision, governing files read, previous open defects with IDs, failure evidence, root cause, exact corrective commits/actions, reproduction and verification results, post-fix FULL restart results, independent audit evidence, remaining external dependencies, next actionable correction, verdict. For a failed gate, write a provisional finding immediately and **execute the corrective work before writing the final report**. If an external dependency remains, mark BLOCKED with owner action and report all independent work done; never falsely claim SUCCESS.

## Mechanical checklist
- [ ] Every actionable rejection has an identified root cause and a concrete corrective change
- [ ] Original failure is retested and passes after correction
- [ ] Restart follows, never precedes, correction
- [ ] Every restarted gate has evidence, not assertions
- [ ] Repeated failure triggers another correction, not another identical report
- [ ] No production artifact is published on unverified, unsigned or self-approved evidence
- [ ] Final report describes real work and accurately distinguishes SUCCESS from unresolved external dependencies

This policy supersedes any workflow wording that allows 'REJECTED -> report -> stop' or 'REJECTED -> restart without correction'.
