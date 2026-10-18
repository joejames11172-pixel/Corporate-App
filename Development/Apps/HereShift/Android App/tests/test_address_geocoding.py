"""Static checks for Android address lookup; not a substitute for device tests."""
from pathlib import Path
import unittest

UI=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/peostectica/hereshift/MainActivity.kt").read_text()

class AddressLookupTests(unittest.TestCase):
    def test_address_button_starts_lookup(self):
        self.assertIn("resolveStreetAddress(addressInput.text.toString().trim())", UI)
    def test_worker_thread_used(self):
        body=UI.split("private fun resolveStreetAddress(query: String) {",1)[1].split("private fun setPoint(",1)[0]
        self.assertIn("Thread {",body)
        self.assertLess(body.index("Thread {"),body.index("getFromLocationName(query, 5)"))
    def test_geocoder_missing_does_not_change_location(self):
        self.assertIn('if (!Geocoder.isPresent())',UI)
    def test_no_matches_fail_closed(self):
        self.assertIn("if (candidates.isEmpty())",UI)
        self.assertIn("No verified address match. Location unchanged.",UI)
    def test_user_must_confirm_selected_result(self):
        self.assertIn("setSingleChoiceItems(labels, -1)",UI)
        self.assertIn('.setPositiveButton("Use location")',UI)
        self.assertIn("setPoint(result.latitude.toString(), result.longitude.toString())",UI)
    def test_old_placeholder_removed(self):
        self.assertNotIn("Address lookup pending",UI)

if __name__=="__main__":
    unittest.main()
