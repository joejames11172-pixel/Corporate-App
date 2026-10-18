#!/usr/bin/env python3
"""Source/pre-sign release gate: FAIL until complete production objectives and brand."""
from pathlib import Path
import json,sys
ROOT=Path(__file__).resolve().parents[1]
def audit():
    issues=[]
    try:brand=json.loads((ROOT/'qa/brand_selection.json').read_text())
    except (OSError,ValueError):return ['CEO TapVoyage brand selection file absent/invalid']
    if brand.get('app')!='TapVoyage' or brand.get('ceo_approved') is not True:
        issues.append('TapVoyage CEO-approved light/dark icon selection missing')
    for field in ('light_concept','dark_concept','approved_light_svg','approved_dark_svg'):
        if not brand.get(field):issues.append('Brand resource missing: '+field)
    ui=(ROOT/'app/src/main/java/com/peostectica/tapvoyage/MainActivity.kt').read_text()
    banned={
        'Development build':'Unfinished customer-facing development interface',
        'Not connected to production':'Unimplemented live connection/action',
        'No authenticated requests received':'No real owner request synchronization',
        'Google Play Billing is not configured':'No real Play Billing',
        'TapVoyagePolicy()':'In-memory trust state in customer-facing Activity'
    }
    for phrase,meaning in banned.items():
        if phrase in ui:issues.append(meaning)
    manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
    if 'android:exported="true"' not in manifest:issues.append('No Android launcher entry')
    gradle=(ROOT/'app/build.gradle.kts').read_text()
    if 'applicationId = "com.peostectica.tapvoyage"' not in gradle:issues.append('Wrong Android package ID')
    if 'isDebuggable = false' not in gradle or 'signingConfig = signingConfigs.getByName("production")' not in gradle:
        issues.append('Signed production release variant not enforced')
    return issues
if __name__=='__main__':
    problems=audit()
    print(json.dumps({'verdict':'REJECT_RESTART' if problems else 'PREBUILD_SOURCE_GATE_PASS_NOT_PRODUCTION_APPROVAL','issues':problems},indent=2))
    sys.exit(bool(problems))
