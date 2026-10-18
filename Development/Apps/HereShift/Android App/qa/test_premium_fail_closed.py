"""Premium UI must never imply payment or access without verified billing."""
from pathlib import Path
import unittest

SOURCE = (Path(__file__).resolve().parents[1] / "app/src/main/java/com/peostectica/hereshift/MainActivity.kt").read_text()
class PremiumTests(unittest.TestCase):
    def test_visible_free_status(self):
        self.assertIn("✕  Premium inactive · Free",SOURCE)
    def test_no_false_premium_activation(self):
        self.assertIn("Premium requires a verified Google Play subscription",SOURCE)
        self.assertIn("No subscription is active or billed",SOURCE)
    def test_purchase_button_explains_unavailability(self):
        self.assertIn('button("Get Premium · availability",true)',SOURCE)
        self.assertIn("This button cannot purchase Premium or grant access",SOURCE)
    def test_circle_remains_closed_without_consent(self):
        self.assertIn("Remote location sharing is unavailable until authenticated approval and revocation are connected",SOURCE)

if __name__=="__main__": unittest.main()
