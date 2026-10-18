#!/usr/bin/env python3
"""DAVG corrective-loop transition gate. Fails closed on premature restart/closure.
Usage: python3 qa/davg_transition_gate.py path/to/iteration.json
"""
import json
import sys
from pathlib import Path

VALID = {"OPEN", "CORRECTING", "VERIFIED", "RESTARTED", "SUCCESS", "EXTERNAL_BLOCKED"}

def validate(record):
    errors = []
    defects = record.get("defects")
    if not isinstance(defects, list) or not defects:
        return ["Missing defect register"]
    seen = set()
    for item in defects:
        ident = item.get("id", "")
        if not ident or ident in seen:
            errors.append("Missing or duplicate defect ID")
        seen.add(ident)
        state = item.get("state")
        if state not in VALID:
            errors.append(f"{ident}: invalid state")
            continue
        if state in {"VERIFIED", "RESTARTED", "SUCCESS"}:
            for key in ("root_cause", "correction_ref", "reproduction", "verification_ref"):
                if not item.get(key):
                    errors.append(f"{ident}: missing {key} before {state}")
        if state in {"RESTARTED", "SUCCESS"}:
            if not item.get("restart_evidence"):
                errors.append(f"{ident}: restart lacks evidence")
        if state == "SUCCESS" and not item.get("independent_auditor_refs"):
            errors.append(f"{ident}: no independent audit evidence")
        if state == "EXTERNAL_BLOCKED":
            for key in ("owner", "required_owner_action", "dependency_evidence"):
                if not item.get(key):
                    errors.append(f"{ident}: missing external dependency {key}")
        if state in {"OPEN", "CORRECTING"} and record.get("attempt_close"):
            errors.append(f"{ident}: cannot close with actionable defect {state}")
        if state == "EXTERNAL_BLOCKED" and record.get("attempt_close") and not record.get("all_independent_work_completed"):
            errors.append(f"{ident}: independent corrective work not completed")
    if record.get("attempt_promote"):
        if any(item.get("state") != "SUCCESS" for item in defects):
            errors.append("Production promotion prohibited until every defect is SUCCESS")
        for key in ("signed_release_evidence", "installed_device_evidence", "five_auditor_refs", "ceo_approval_ref"):
            if not record.get(key):
                errors.append(f"Production promotion missing {key}")
        if len(record.get("five_auditor_refs", [])) < 5:
            errors.append("Five distinct auditor acceptances required")
        if len(set(record.get("five_auditor_refs", []))) != len(record.get("five_auditor_refs", [])):
            errors.append("Duplicate auditor acceptance")
    return errors

def main():
    if len(sys.argv) != 2:
        print("Usage: davg_transition_gate.py iteration.json", file=sys.stderr)
        return 2
    try:
        record = json.loads(Path(sys.argv[1]).read_text())
        errors = validate(record)
    except (OSError, ValueError, TypeError) as exc:
        print(f"INVALID DAVG record: {exc}", file=sys.stderr)
        return 2
    for error in errors:
        print("FAIL:", error)
    if errors:
        return 1
    print("PASS: requested DAVG transition valid; no claim of production acceptance")
    return 0

if __name__ == "__main__":
    sys.exit(main())
