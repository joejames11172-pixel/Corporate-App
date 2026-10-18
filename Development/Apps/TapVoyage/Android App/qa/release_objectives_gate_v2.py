#!/usr/bin/env python3
"""TapVoyage *pre-build* source/evidence gate, not a production acceptance authority.

Reads the Google-Drive-canonical Android App tree. Fails closed on missing,
malformed, forged-looking or stale evidence; never creates build artifacts.
"""
from __future__ import annotations
import hashlib
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
POST_BUILD = frozenset({'SIGNED_BUILD', 'RENDER', 'INSTALL', 'AUDIT', 'DEPLOY'})
MANDATORY = frozenset('BRAND CIRCLE CONTACTS INVENTORY GET_PERMISSIONS GIVE_PERMISSIONS FULL_DEVICE SESSION_AUTH REMOTE_VIDEO REMOTE_AUDIO REMOTE_CONTROL FREE_RULE PREMIUM ENTITLEMENT_FAIL_CLOSED DURABLE_REVOCATION DISPLAY AUDIO_THEME DATA_POLICY SIGNED_BUILD RENDER INSTALL SECURITY AUDIT DEPLOY'.split())
BOARD = '_template/01_Brand_Icon_Selection/Examples/TapVoyage_20_Icon_Concepts_RASTER_REFERENCE.png'
BOARD_SHA256 = '1d431e9eb2decbddbad303016fe7b3821a500f0a9c69edd1a604625dea537bd1'
SHA256 = re.compile(r'^[a-fA-F0-9]{64}$')
GIT_SHA = re.compile(r'^[a-fA-F0-9]{40}$')
ANDROID = '{http://schemas.android.com/apk/res/android}'


def read_text(root: Path, relative: str, issues: list[str]) -> str:
    path = root / relative
    try:
        return path.read_text(encoding='utf-8')
    except (OSError, UnicodeError) as exc:
        issues.append('Missing/unreadable source ' + relative + ': ' + type(exc).__name__)
        return ''


def read_json(root: Path, relative: str, issues: list[str]):
    raw = read_text(root, relative, issues)
    if not raw:
        return None
    try:
        return json.loads(raw)
    except (ValueError, TypeError):
        issues.append('Malformed JSON: ' + relative)
        return None


def safe_evidence(root: Path, item, issues: list[str], label: str) -> None:
    if not isinstance(item, dict):
        issues.append(label + ': evidence must be an object with path and sha256')
        return
    relative, expected = item.get('path'), item.get('sha256')
    if not isinstance(relative, str) or not relative or not isinstance(expected, str) or not SHA256.fullmatch(expected):
        issues.append(label + ': evidence path/hash missing or invalid')
        return
    path = root / relative
    try:
        resolved = path.resolve(strict=True)
        if not resolved.is_relative_to(root.resolve()):
            raise ValueError('evidence escapes project root')
        if not resolved.is_file() or resolved.stat().st_size == 0:
            raise ValueError('empty or non-file evidence')
        actual = hashlib.sha256(resolved.read_bytes()).hexdigest()
        if actual.lower() != expected.lower():
            raise ValueError('SHA-256 mismatch')
    except (OSError, ValueError) as exc:
        issues.append(label + ': evidence verification failed: ' + str(exc))


def audit(root: Path = ROOT, phase: str = "prebuild") -> list[str]:
    if phase not in ("prebuild", "postbuild"):
        return ["Invalid acceptance gate phase"]
    issues: list[str] = []
    brand = read_json(root, 'qa/brand_selection.json', issues)
    if not isinstance(brand, dict):
        issues.append('TapVoyage CEO brand record absent/invalid')
    else:
        if brand.get('app') != 'TapVoyage' or brand.get('board_file') != BOARD or brand.get('board_sha256') != BOARD_SHA256:
            issues.append('Wrong/inherited icon board or unverified TapVoyage board fingerprint')
        if brand.get('ceo_approved') is not True:
            issues.append('TapVoyage CEO light/dark icon approval missing')
        if not isinstance(brand.get('approval_evidence'), dict):
            issues.append('CEO icon approval evidence reference missing')
        else:
            proof = brand['approval_evidence']
            if not all(isinstance(proof.get(k), str) and proof[k].strip() for k in ('drive_file_id', 'approved_utc', 'board_revision')):
                issues.append('Incomplete CEO icon approval evidence reference')
        for mode in ('light', 'dark'):
            concept = brand.get(mode + '_concept')
            if type(concept) is not int or not 1 <= concept <= 20:
                issues.append('Unselected/invalid ' + mode + ' concept number')
            svg = brand.get('approved_' + mode + '_svg')
            sha = brand.get('approved_' + mode + '_svg_sha256')
            if not svg or not sha:
                issues.append('Missing approved editable ' + mode + ' SVG + SHA-256')
            else:
                safe_evidence(root, {'path': svg, 'sha256': sha}, issues, mode + ' SVG')
                try:
                    xml = ET.parse(root / svg).getroot()
                    if not xml.tag.endswith('svg') or not any(n.tag.endswith('path') and n.get('d') for n in xml.iter()):
                        issues.append(mode + ' SVG lacks editable path geometry')
                    if any(n.tag.endswith('image') for n in xml.iter()):
                        issues.append(mode + ' SVG contains raster image embedding')
                except (OSError, ET.ParseError, TypeError):
                    issues.append(mode + ' SVG is not a parseable editable SVG')

    ui = read_text(root, 'app/src/main/java/com/peostectica/tapvoyage/MainActivity.kt', issues)
    for phrase, reason in {
        'Development build': 'Developer-only customer interface',
        'Not connected to production': 'No authenticated live connection',
        'No authenticated requests received': 'Owner request synchronization absent',
        'Google Play Billing is not configured': 'Server-verified Play Billing absent',
        'TapVoyagePolicy()': 'In-memory trust state in customer Activity',
    }.items():
        if phrase in ui:
            issues.append(reason)

    manifest = read_text(root, 'app/src/main/AndroidManifest.xml', issues)
    if manifest:
        try:
            parsed = ET.fromstring(manifest)
            application = parsed.find('application')
            if application is None:
                issues.append('Android application manifest entry missing')
            else:
                if application.get(ANDROID + 'debuggable') == 'true':
                    issues.append('FORBIDDEN: debuggable Android manifest')
                if not any(activity.get(ANDROID + 'exported') == 'true' for activity in application.findall('activity')):
                    issues.append('Launcher exported activity missing')
        except ET.ParseError:
            issues.append('Android manifest invalid XML')

    gradle = read_text(root, 'app/build.gradle.kts', issues)
    for fragment, reason in {
        'applicationId = "com.peostectica.tapvoyage"': 'Wrong Android application identity',
        'getByName("release")': 'No explicit release build type',
        'isDebuggable = false': 'Release debuggable=false not explicit',
        'signingConfig = signingConfigs.getByName("production")': 'Approved production signing not configured',
        'TAPVOYAGE_RELEASE_KEYSTORE_FILE': 'Signing identity not required',
    }.items():
        if fragment not in gradle:
            issues.append(reason)
    if re.search(r'isDebuggable\s*=\s*true', gradle):
        issues.append('FORBIDDEN: debuggable build setting')

    matrix = read_json(root, 'qa/acceptance_matrix.json', issues)
    if not isinstance(matrix, dict) or matrix.get('project') != 'TapVoyage':
        issues.append('Missing/foreign TapVoyage acceptance matrix')
    else:
        rows = matrix.get('requirements')
        if not isinstance(rows, list):
            issues.append('Acceptance matrix requirements not an array')
        else:
            ids = [row.get('id') for row in rows if isinstance(row, dict)]
            if len(rows) != len(MANDATORY) or len(ids) != len(rows) or len(set(ids)) != len(ids) or set(ids) != MANDATORY:
                issues.append('Missing, duplicated or extra TapVoyage requirements')
            for row in rows:
                if not isinstance(row, dict):
                    issues.append('Invalid acceptance row type')
                    continue
                label = str(row.get('id', '?'))
                if phase == 'prebuild' and label in POST_BUILD:
                    continue  # Physical installation, audits and deployment are POST-build gates.
                if row.get('status') != 'VERIFIED':
                    issues.append('Objective not independently verified: ' + label)
                    continue
                if not isinstance(row.get('source_revision'), str) or not GIT_SHA.fullmatch(row['source_revision']):
                    issues.append(label + ': invalid source commit SHA')
                reviewer = row.get('reviewer_id')
                if not isinstance(reviewer, str) or not reviewer.strip() or reviewer.strip().lower() in ('alex', 'self', 'assistant', 'author'):
                    issues.append(label + ': independent reviewer identity absent')
                evidence = row.get('test_evidence')
                if not isinstance(evidence, list) or not evidence:
                    issues.append(label + ': test evidence manifest absent')
                else:
                    for item in evidence:
                        safe_evidence(root, item, issues, label)
    return issues


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--phase', choices=('prebuild', 'postbuild'), default='prebuild')
    args = parser.parse_args()
    problems = audit(phase=args.phase)
    print(json.dumps({'phase': args.phase, 'verdict': 'REJECT_RESTART' if problems else 'SOURCE_EVIDENCE_GATE_PASS_NOT_PRODUCTION_APPROVAL',
                      'issues': problems}, indent=2))
    sys.exit(1 if problems else 0)
