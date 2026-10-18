"""Regression guards for mock-provider ownership and fail-closed restart.

Run with python3 -m unittest discover -s qa -p 'test_mock_provider_ownership.py'.
Source-level tests only: Android runtime testing is still mandatory.
"""
from pathlib import Path
import unittest

SOURCE = (Path(__file__).resolve().parents[1] / "app/src/main/java/com/peostectica/hereshift/MockLocationService.kt").read_text()

class ProviderOwnershipTests(unittest.TestCase):
    def test_registration_marks_ownership_only_after_add(self):
        self.assertLess(SOURCE.index("manager.addTestProvider("), SOURCE.index("ownsTestProvider = true"))
    def test_cleanup_only_removes_owned_provider(self):
        self.assertIn("if (!ownsTestProvider) return true", SOURCE)
        self.assertLess(SOURCE.index("if (!ownsTestProvider) return true"), SOURCE.index("manager.removeTestProvider("))
    def test_failed_cleanup_blocks_new_registration(self):
        start = SOURCE.index("if (!stopProvider())")
        register = SOURCE.index("manager.addTestProvider(")
        self.assertLess(start, register)
        block = SOURCE[start:register]
        self.assertIn("stopSelf()", block)
        self.assertIn("return START_NOT_STICKY", block)
    def test_stop_action_reports_cleanup_result(self):
        block = SOURCE[SOURCE.index("if (intent?.action == ACTION_STOP)"):SOURCE.index("if (intent?.action != ACTION_START)")]
        self.assertIn("val removed = stopProvider()", block)
        self.assertIn("provider cleanup could not be confirmed", block)
        self.assertIn("stopSelf()", block)
    def test_destroy_preserves_warning_for_owned_provider(self):
        block = SOURCE[SOURCE.index("override fun onDestroy()"):]
        self.assertIn("val hadOwnedProvider = ownsTestProvider", block)
        self.assertIn("wasActive || hadOwnedProvider || !removed", block)
    def test_cleanup_retains_ownership_when_removal_throws(self):
        block = SOURCE[SOURCE.index("private fun stopProvider()"):SOURCE.index("override fun onDestroy()")]
        self.assertLess(block.index("manager.removeTestProvider("), block.index("ownsTestProvider = false"))
        self.assertIn("catch (_: Exception) { false }", block)

if __name__ == "__main__":
    unittest.main()
