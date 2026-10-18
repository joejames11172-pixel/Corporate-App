"""Source regression check for phone-width navigation; not a device-render test."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/java/com/peostectica/hereshift/MainActivity.kt"

class NavigationLayoutTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ui = UI.read_text(encoding="utf-8")

    def test_can_scroll_in_narrow_portrait(self):
        self.assertIn("val navigationScroller = HorizontalScrollView(this).apply", self.ui)
        self.assertIn("addView(navigationBar)", self.ui)

    def test_labels_are_not_squeezed_into_five_equal_columns(self):
        self.assertIn("LinearLayout.LayoutParams(dp(92), dp(52))", self.ui)
        self.assertNotIn("LinearLayout.LayoutParams(0, dp(52), 1f)", self.ui)

    def test_all_approved_sections_have_navigation(self):
        for label in ("Home", "Circle", "Display", "Audio", "Privacy"):
            self.assertIn('Triple("' + label + '"', self.ui)

    def test_talkback_label(self):
        self.assertIn('navButton.contentDescription = "Navigate to " + caption', self.ui)

if __name__ == "__main__":
    unittest.main()
