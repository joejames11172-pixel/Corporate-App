import re, unittest
from pathlib import Path
WF=Path(__file__).with_name('tapvoyage-android-signed-release.v4.yml')
class WorkflowTests(unittest.TestCase):
    def test_unescaped_github_secret_expressions(self):
        s=WF.read_text()
        self.assertNotIn('\\${{',s)
        self.assertIn('KEYSTORE_B64: ${{ secrets.TAPVOYAGE_RELEASE_KEYSTORE_B64 }}',s)
    def test_only_release_build_tasks(self):
        s=WF.read_text()
        self.assertIn('gradle --no-daemon :app:assembleRelease :app:bundleRelease',s)
        self.assertNotRegex(s,r':app:(?:assembleDebug|bundleDebug|testDebugUnitTest)')
    def test_postrelease_reviews_never_block_build_or_delivery(self):
        s=WF.read_text()
        self.assertNotIn(' --phase prebuild',s)
        uploaded=s.index('upload-artifact@v4')
        for review in ('release_objectives_gate_v4.py','test_*.py','test_tap_voyage_backend_mock.py','verify_packaged_audio.py'):
            self.assertGreater(s.index(review),uploaded)
        self.assertIn('continue-on-error: true',s)
        self.assertIn('TapVoyage-signed-release-build',s)

    def test_apk_and_aab_signer_match(self):
        s=WF.read_text()
        self.assertIn('test "$APK_CERT" = "$EXPECTED"',s)
        self.assertIn('test "$AAB_CERT" = "$EXPECTED"',s)
        self.assertIn('apksigner" verify',s)
        self.assertIn('jarsigner -verify -strict',s)
    def test_full_native_source_qa_suite(self):
        s=WF.read_text()
        self.assertIn("-p 'test_*.py'", s)
        self.assertIn('release_objectives_gate_v4.py', s)
    def test_no_public_deployment(self):
        s=WF.read_text()
        self.assertNotIn('deployments/android',s)
        self.assertIn(' --phase postbuild',s)
    def test_audio_generator_is_available_for_build_and_review_is_after_artifact(self):
        s=WF.read_text()
        self.assertIn("'numpy==2.3.5'",s)
        self.assertIn('verify_packaged_audio.py',s)
        self.assertLess(s.index("'numpy==2.3.5'"),s.index('gradle --no-daemon'))
        self.assertLess(s.index('gradle --no-daemon'),s.index('upload-artifact@v4'))
        self.assertLess(s.index('upload-artifact@v4'),s.index('verify_packaged_audio.py'))

    def test_signed_apk_identity_and_distribution_checks(self):
        s=WF.read_text()
        self.assertIn('apksigner" verify --min-sdk-version 26',s)
        self.assertIn('aapt" dump badging',s)
        self.assertIn("name='com\\.peostectica\\.tapvoyage'",s)
        self.assertIn('android:testOnly',s)
        self.assertIn('android:debuggable',s)
        self.assertIn('test "$AAB_CERT" = "$EXPECTED"',s)
if __name__=='__main__':unittest.main(verbosity=2)
