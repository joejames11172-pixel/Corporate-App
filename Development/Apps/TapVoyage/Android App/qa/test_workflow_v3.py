import re, unittest
from pathlib import Path
WF=Path(__file__).with_name('tapvoyage-android-signed-release.v3.yml')
class WorkflowTests(unittest.TestCase):
    def test_unescaped_github_secret_expressions(self):
        s=WF.read_text()
        self.assertNotIn('\\${{',s)
        self.assertIn('KEYSTORE_B64: ${{ secrets.TAPVOYAGE_RELEASE_KEYSTORE_B64 }}',s)
    def test_only_release_build_tasks(self):
        s=WF.read_text()
        self.assertRegex(s,r'gradle --no-daemon :app:testReleaseUnitTest :app:assembleRelease :app:bundleRelease')
        self.assertNotRegex(s,r':app:(?:assembleDebug|bundleDebug|testDebugUnitTest)')
    def test_prebuild_gate_before_signing_and_compilation(self):
        s=WF.read_text()
        self.assertLess(s.index(' --phase prebuild'),s.index('Require approved signing identity'))
        self.assertLess(s.index('Require approved signing identity'),s.index('gradle --no-daemon'))
    def test_apk_and_aab_signer_match(self):
        s=WF.read_text()
        self.assertIn('test "$APK_CERT" = "$EXPECTED"',s)
        self.assertIn('test "$AAB_CERT" = "$EXPECTED"',s)
        self.assertIn('apksigner" verify',s)
        self.assertIn('jarsigner -verify -strict',s)
    def test_full_native_source_qa_suite(self):
        s=WF.read_text()
        self.assertIn("-p 'test_*.py'", s)
        self.assertIn('release_objectives_gate_v3.py', s)
    def test_backend_consent_and_session_ttl_suite_enforced(self):
        s=WF.read_text()
        self.assertIn("'Development/Apps/TapVoyage/Backend Prototype/**'",s)
        self.assertIn('Run owner consent/session/expiry backend simulator regressions',s)
        self.assertIn('run: python3 test_tap_voyage_backend_mock.py',s)
        self.assertLess(s.index('run: python3 test_tap_voyage_backend_mock.py'),s.index(' --phase prebuild'))

    def test_approved_original_audio_packaging_and_generator_enforced(self):
        s=WF.read_text()
        self.assertIn("numpy==2.3.5",s)
        self.assertIn('qa/verify_packaged_audio.py',s)
        self.assertLess(s.index('gradle --no-daemon'),s.index('qa/verify_packaged_audio.py'))
        self.assertLess(s.index('qa/verify_packaged_audio.py'),s.index('upload-artifact@v4'))

    def test_no_public_deployment(self):
        s=WF.read_text()
        self.assertNotIn('deployments/android',s)
        self.assertIn(' --phase postbuild',s)
if __name__=='__main__':unittest.main(verbosity=2)
