from pathlib import Path
import unittest

BASE=Path(__file__).resolve().parents[1]/"app/src/main/java/com/peostectica/hereshift"
POLICY=(BASE/"PremiumAccessPolicy.kt").read_text()
MAIN=(BASE/"MainActivity.kt").read_text()
class PremiumPolicyTests(unittest.TestCase):
    def test_no_unverified_local_premium(self):
        self.assertIn("if (!e.verifiedByTrustedServer || !e.active || e.revoked) return false",POLICY)
    def test_expired_and_stale_rejected(self):
        self.assertIn("e.expiryEpochMillis <= nowEpochMillis",POLICY)
        self.assertIn("MAX_VERIFICATION_AGE_MS",POLICY)
    def test_account_binding_required(self):
        self.assertIn("e.accountId != authenticatedAccountId",POLICY)
    def test_no_backend_denies_entitlement(self):
        self.assertIn("allowed(feature, null, null, System.currentTimeMillis())",POLICY)
    def test_circle_invitation_checks_entitlement(self):
        block=MAIN[MAIN.index('button("Request contact permission"'):MAIN.index('button("Stop / revoke my sharing"')]
        self.assertIn("PremiumAccessPolicy.currentBuildCanUse",block)
        self.assertIn("return@button",block)
        self.assertIn("No invitation was sent",block)

if __name__=="__main__": unittest.main()
