from pathlib import Path
import unittest

BASE=Path(__file__).resolve().parents[1]/"app/src/main/java/com/peostectica/hereshift"
POLICY=(BASE/"CircleConsentPolicy.kt").read_text()
MAIN=(BASE/"MainActivity.kt").read_text()
class CircleConsentTests(unittest.TestCase):
    def test_premium_alone_cannot_grant_access(self):
        self.assertIn("if (!premiumAuthorized) return false",POLICY)
        self.assertIn("val g = grant ?: return false",POLICY)
    def test_owner_and_viewer_bound(self):
        self.assertIn("g.viewerAccountId != signedInViewerAccountId",POLICY)
        self.assertIn("g.ownerAccountId != selectedOwnerAccountId",POLICY)
    def test_revocation_and_approval(self):
        self.assertIn("!g.approvedByOwner || g.revoked",POLICY)
    def test_expiration_and_freshness(self):
        self.assertIn("g.expiresAtEpochMillis <= nowEpochMillis",POLICY)
        self.assertIn("MAX_GRANT_VERIFICATION_AGE_MS",POLICY)
    def test_build_fails_closed_without_server(self):
        self.assertIn("canView(null, null, null, System.currentTimeMillis(), false)",POLICY)
    def test_ui_uses_policy_for_status(self):
        self.assertIn("CircleConsentPolicy.currentBuildCanView()",MAIN)

if __name__=="__main__":unittest.main()
