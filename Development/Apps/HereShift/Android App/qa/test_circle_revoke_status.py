from pathlib import Path
import unittest

SOURCE=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/peostectica/hereshift/MainActivity.kt").read_text()
class CircleRevokeTests(unittest.TestCase):
    def test_revoke_invokes_stop(self):
        block=SOURCE[SOURCE.index('button("Stop / revoke my sharing"'):SOURCE.index('val policy = innerCard()',SOURCE.index('button("Stop / revoke my sharing"'))]
        self.assertIn("stopMock()",block)
    def test_revoke_does_not_claim_confirmed_stop(self):
        block=SOURCE[SOURCE.index('button("Stop / revoke my sharing"'):SOURCE.index('val policy = innerCard()',SOURCE.index('button("Stop / revoke my sharing"'))]
        self.assertIn("verify service cleanup status",block)
        self.assertNotIn("Local session stopped;",block)
    def test_no_fake_remote_revocation(self):
        self.assertIn("no remote sharing was active",SOURCE)

if __name__=="__main__":unittest.main()
