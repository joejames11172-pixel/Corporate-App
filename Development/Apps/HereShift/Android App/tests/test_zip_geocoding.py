from pathlib import Path
import unittest

UI=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/peostectica/hereshift/MainActivity.kt").read_text(encoding="utf-8")
class ZipGeocodingTests(unittest.TestCase):
    def test_zip_button_starts_lookup(self):
        self.assertIn("else resolveZipArea(entered)",UI)
    def test_zip_validation(self):
        self.assertIn('Regex("[0-9]{5}").matches(entered)',UI)
    def test_off_ui_thread(self):
        body=UI.split("private fun resolveZipArea(zip: String) {",1)[1].split("private fun resolveStreetAddress(",1)[0]
        self.assertLess(body.index("Thread {"),body.index("getFromLocationName("))
    def test_matches_actual_country_and_postal_code(self):
        self.assertIn('item.countryCode.equals("US", ignoreCase = true)',UI)
        self.assertIn("item.postalCode == zip",UI)
    def test_no_match_fails_closed(self):
        self.assertIn("No matching verified ZIP area. Location unchanged.",UI)
    def test_confirmation_only(self):
        self.assertIn('setPositiveButton("Use approximate area")',UI)
        self.assertIn("NOT a verified street address",UI)
if __name__=="__main__": unittest.main()
