"""Regression checks for SDK35+ edge-to-edge and earlier Android window insets.
These static checks do not replace an installed-device portrait/landscape render.
"""
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=(ROOT/"app/src/main/java/com/peostectica/tapvoyage/MainActivity.kt").read_text(encoding="utf-8")
BUILD=(ROOT/"app/build.gradle.kts").read_text(encoding="utf-8")

class SystemInsetTests(unittest.TestCase):
    def test_target_requires_edge_to_edge_aware_layout(self):
        self.assertIn("targetSdk = 36",BUILD)
        self.assertIn("root.setOnApplyWindowInsetsListener",SRC)

    def test_system_bars_and_display_cutout_both_respected(self):
        self.assertIn("WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()",SRC)

    def test_left_top_right_bottom_safe_padding(self):
        self.assertIn("view.setPadding(safe.left, safe.top, safe.right, safe.bottom)",SRC)

    def test_legacy_devices_get_compatible_insets(self):
        self.assertIn("if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)",SRC)
        for side in ("Left","Top","Right","Bottom"):
            self.assertIn("windowInsets.systemWindowInset"+side,SRC)

    def test_layout_recreation_rerequests_insets(self):
        self.assertIn("setContentView(root)\n        root.requestApplyInsets()",SRC)
        self.assertLess(SRC.index("root.setOnApplyWindowInsetsListener"),SRC.index("setContentView(root)"))

    def test_bottom_navigation_remains_in_safe_root(self):
        self.assertIn("val navigationScroller = HorizontalScrollView(this).apply",SRC)
        self.assertIn("root.addView(navigationScroller",SRC)

if __name__=="__main__":unittest.main()
