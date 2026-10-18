#!/usr/bin/env python3
"""Fail closed on known incomplete HereShift objectives. Run before signing/publishing."""
from pathlib import Path
import sys

root = Path(__file__).resolve().parent
ui = (root / "app/src/main/java/com/peostectica/hereshift/MainActivity.kt").read_text()
gradle = (root / "app/build.gradle.kts").read_text()
failures = []
required = {
    "native circle navigation": 'Page.CIRCLE',
    "manual latitude": 'latitudeInput=field("Latitude"',
    "manual longitude": 'longitudeInput=field("Longitude"',
    "persistent native navigation": 'navigationBar',
    "mock stop": 'stopMock()',
    "release non-debuggable": 'isDebuggable = false',
}
for name, token in required.items():
    if token not in (gradle if name == "release non-debuggable" else ui):
        failures.append(f"Missing: {name}")
prohibited = {
    "unimplemented Circle permission": "Authenticated Circle invitations and contact-approved live sharing are not connected",
    "unimplemented subscriptions": "Premium · planned, not billed",
    "demo-only sharing": "Not connected · no contact location is being received",
    "unfinished developer UI": "Development mode · UI testing",
}
for name, token in prohibited.items():
    if token in ui:
        failures.append(f"Unfinished production functionality: {name}")
for name, token in {
    "address entry": "addressInput",
    "ZIP code entry": "zipInput",
    "authenticated permission/consent": "verifiedConsent",
    "revocation enforcement": "revokeSharingGrant",
    "verified premium entitlement": "verifiedPremiumEntitlement",
}.items():
    if token not in ui:
        failures.append(f"Missing required integration: {name}")
if "versionCode = 8" not in gradle:
    failures.append("Expected upgrade versionCode 8")
for failure in failures:
    print("REJECT:", failure)
if failures:
    print(f"DAVG RESTART REQUIRED: {len(failures)} objective gates failed")
    sys.exit(1)
print("STATIC OBJECTIVES GATE PASS; device-render and independent audit still required")
