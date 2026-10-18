"""Regression checks for stale-location protection; device tests still required."""
from pathlib import Path
import re
import unittest

source = (Path(__file__).resolve().parents[1] / "app/src/main/java/com/peostectica/hereshift/MainActivity.kt").read_text(encoding="utf-8")
match = re.search(r"private fun currentPoint\(\):TestPoint\? \{([\s\S]*?)\n    \}", source)
class CurrentPointSafetyTests(unittest.TestCase):
    def test_function_exists(self):
        self.assertIsNotNone(match)
    def test_reads_visible_latitude_and_longitude(self):
        body = match.group(1)
        self.assertIn("latitudeInput?.text?.toString()", body)
        self.assertIn("longitudeInput?.text?.toString()", body)
    def test_invalid_edits_fail_closed(self):
        self.assertIn("ScenarioRules.parse(latitude, longitude) ?: return null", match.group(1))
    def test_no_prior_save_before_validation(self):
        body = match.group(1)
        self.assertNotIn("saveCoordinates()", body)
        self.assertLess(body.index("ScenarioRules.parse("), body.index('prefs.edit().putString("lat"'))
    def test_start_and_preview_use_current_point(self):
        self.assertIn("private fun previewPoint() {\n        val point=currentPoint()", source)
        self.assertIn("private fun startMock() {\n        val point=currentPoint()", source)

if __name__ == "__main__":
    unittest.main()
