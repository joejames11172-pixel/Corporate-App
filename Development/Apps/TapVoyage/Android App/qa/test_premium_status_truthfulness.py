"""Premium UI may only present a verified entitlement status (device tests pending)."""
import unittest
from pathlib import Path
SRC=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/peostectica/tapvoyage/MainActivity.kt").read_text(encoding="utf-8")
class PremiumTruthfulnessTests(unittest.TestCase):
    def test_does_not_claim_free_for_unknown_entitlement(self):
        self.assertNotIn('Free · not subscribed',SRC)
        self.assertIn('Premium status · Not verified',SRC)
    def test_explains_unverified_entitlement_without_guessing(self):
        self.assertIn('cannot currently confirm either Free or Premium status',SRC)
        self.assertIn('Google Play account entitlement is not connected',SRC)
    def test_billing_button_makes_no_fake_purchase(self):
        self.assertIn('Google Play Billing is not configured; no purchase was started and no charge was made',SRC)
    def test_displays_actual_planned_weekly_monthly_prices(self):
        self.assertIn('$1.99 / week',SRC)
        self.assertIn('$4.99 / month',SRC)
if __name__ == "__main__":unittest.main()
