import re, unittest
from pathlib import Path
WF=Path(__file__).with_name('tapvoyage-android-signed-release.v2.yml')
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
    def test_no_public_deployment(self):
        s=WF.read_text()
        self.assertNotIn('deployments/android',s)
        self.assertIn(' --phase postbuild',s)
if __name__=='__main__':unittest.main(verbosity=2)
