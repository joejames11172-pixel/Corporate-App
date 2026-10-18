#!/usr/bin/env python3
"""Real negative-path regression tests for the TapVoyage fail-closed prebuild gate."""
import importlib.util
import json
import hashlib
import struct
import tempfile
import zlib
from unittest.mock import patch
import unittest
from pathlib import Path

MODULE = Path(__file__).with_name('release_objectives_gate_v3.py')
spec = importlib.util.spec_from_file_location('tapvoyage_release_gate_v3', MODULE)
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)

class GateTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / 'Apps/TapVoyage/Android App'
        self.root.mkdir(parents=True)
        self.board = self.root.parents[1] / gate.BOARD
        self.board.parent.mkdir(parents=True, exist_ok=True)
        self.board.write_bytes(self.make_png())
        self.board_sha = hashlib.sha256(self.board.read_bytes()).hexdigest()
        mocked = patch.object(gate, 'BOARD_SHA256', self.board_sha)
        mocked.start()
        self.addCleanup(mocked.stop)
        self.write('qa/brand_selection.json', json.dumps({'app': 'TapVoyage', 'board_file': gate.BOARD,
            'board_sha256': gate.BOARD_SHA256, 'light_concept': None, 'dark_concept': None,
            'ceo_approved': False, 'approved_light_svg': None, 'approved_dark_svg': None}))
        self.write('qa/acceptance_matrix.json', json.dumps({'project':'TapVoyage', 'requirements':[
            {'id':name,'status':'OPEN','source_revision':None,'test_evidence':[],'reviewer_id':None}
            for name in sorted(gate.MANDATORY)]}))
        self.write('app/src/main/java/com/peostectica/tapvoyage/MainActivity.kt', 'Development build\nTapVoyagePolicy()')
        self.write('app/src/main/AndroidManifest.xml', '<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application><activity android:exported="true"/></application></manifest>')
        self.write('app/build.gradle.kts', 'applicationId = "com.peostectica.tapvoyage"\ngetByName("release")\nisDebuggable = false\nsigningConfig = signingConfigs.getByName("production")\nTAPVOYAGE_RELEASE_KEYSTORE_FILE')

    @staticmethod
    def make_png():
        def chunk(tag, data):
            return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag+data)&0xffffffff)
        w, h = 1000, 800
        ihdr = struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0)
        rows = (b'\x00' + b'\xff' * (w*3)) * h
        return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')

    def test_original_board_bytes_must_match_recorded_sha(self):
        self.assertFalse(any('Original TapVoyage icon board integrity failed' in i for i in self.audit()))
        self.board.write_bytes(self.board.read_bytes() + b'TAMPERED')
        self.assertTrue(any('actual board SHA-256 mismatch' in i for i in self.audit()))

    def test_missing_original_board_rejected(self):
        self.board.unlink()
        self.assertTrue(any('Original TapVoyage icon board integrity failed' in i for i in self.audit()))

    def test_wrong_format_original_board_rejected_even_if_hash_matches(self):
        self.board.write_bytes(b'not-a-png' * 100)
        with patch.object(gate, 'BOARD_SHA256', hashlib.sha256(self.board.read_bytes()).hexdigest()):
            self.assertTrue(any('not a PNG concept board' in i for i in self.audit()))

    def write(self, relative, content):
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content)

    def audit(self):
        return gate.audit(self.root)

    def test_current_unapproved_brand_and_open_objectives_rejected(self):
        issues = self.audit()
        self.assertIn('TapVoyage CEO light/dark icon approval missing', issues)
        self.assertEqual(19, sum(x.startswith('Objective not independently verified: ') for x in issues))
        post=gate.audit(self.root, phase="postbuild")
        self.assertEqual(24, sum(x.startswith('Objective not independently verified: ') for x in post))

    def test_unknown_gate_phase_rejected(self):
        self.assertIn("Invalid acceptance gate phase", gate.audit(self.root, phase="shipping"))

    def test_missing_activity_rejected_without_exception(self):
        (self.root/'app/src/main/java/com/peostectica/tapvoyage/MainActivity.kt').unlink()
        self.assertTrue(any('Missing/unreadable source' in x for x in self.audit()))

    def test_missing_manifest_rejected_without_exception(self):
        (self.root/'app/src/main/AndroidManifest.xml').unlink()
        self.assertTrue(any('AndroidManifest.xml' in x for x in self.audit()))

    def test_debuggable_manifest_rejected(self):
        self.write('app/src/main/AndroidManifest.xml','<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application android:debuggable="true"><activity android:exported="true"/></application></manifest>')
        self.assertIn('FORBIDDEN: debuggable Android manifest', self.audit())

    def test_debuggable_gradle_rejected(self):
        with (self.root/'app/build.gradle.kts').open('a') as f:
            f.write('\nisDebuggable = true\n')
        self.assertIn('FORBIDDEN: debuggable build setting', self.audit())

    def test_inherited_hereshift_board_rejected(self):
        path = self.root/'qa/brand_selection.json'
        obj=json.loads(path.read_text()); obj['board_file']=obj['board_file'].replace('TapVoyage','HereShift')
        path.write_text(json.dumps(obj))
        self.assertTrue(any('Wrong/inherited icon board' in x for x in self.audit()))

    def test_duplicate_requirement_rejected(self):
        path=self.root/'qa/acceptance_matrix.json'
        obj=json.loads(path.read_text()); obj['requirements'][1]['id']=obj['requirements'][0]['id']
        path.write_text(json.dumps(obj))
        self.assertTrue(any('duplicated' in x for x in self.audit()))

    def test_forged_verified_row_rejected(self):
        path=self.root/'qa/acceptance_matrix.json'
        obj=json.loads(path.read_text()); obj['requirements'][0].update(status='VERIFIED',source_revision='fake',test_evidence=['PASS'],reviewer_id='Alex')
        path.write_text(json.dumps(obj))
        issues=self.audit()
        self.assertTrue(any('invalid source commit SHA' in x for x in issues))
        self.assertTrue(any('independent reviewer identity absent' in x for x in issues))
        self.assertTrue(any('evidence must be an object' in x for x in issues))

    def test_evidence_traversal_rejected(self):
        problems=[]
        gate.safe_evidence(self.root, {'path':'../../etc/passwd','sha256':'a'*64}, problems, 'test')
        self.assertTrue(any('evidence verification failed' in x for x in problems))

    def test_malformed_matrix_rejected_without_exception(self):
        self.write('qa/acceptance_matrix.json','{ broken')
        self.assertTrue(any('Malformed JSON' in x for x in self.audit()))

    def test_svg_raster_embedding_rejected(self):
        import hashlib
        svg='<svg xmlns="http://www.w3.org/2000/svg"><path d="M0 0L2 2"/><image href="a.png"/></svg>'
        self.write('qa/light.svg',svg)
        path=self.root/'qa/brand_selection.json'
        obj=json.loads(path.read_text());obj['approved_light_svg']='qa/light.svg';obj['approved_light_svg_sha256']=hashlib.sha256(svg.encode()).hexdigest()
        path.write_text(json.dumps(obj))
        self.assertTrue(any('SVG contains raster image embedding' in x for x in self.audit()))

if __name__=='__main__': unittest.main(verbosity=2)
