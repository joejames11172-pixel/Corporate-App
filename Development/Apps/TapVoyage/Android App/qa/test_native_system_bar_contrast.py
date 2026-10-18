"""System-bar icon contrast regressions for TapVoyage native Android screens.
Runtime legibility still requires real signed-device screenshot inspection.
"""
import unittest
from pathlib import Path
MAIN=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/peostectica/tapvoyage/MainActivity.kt").read_text(encoding="utf-8")

class SystemBarContrastTests(unittest.TestCase):
    def test_os_and_user_selected_modes_drive_icon_contrast(self):
        self.assertIn("val showDarkIcons = !night()",MAIN)
        self.assertIn("if (showDarkIcons) mask else 0",MAIN)
    def test_modern_android_changes_both_status_and_navigation_icons(self):
        self.assertIn("WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS",MAIN)
        self.assertIn("WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS",MAIN)
        self.assertIn("window.insetsController?.setSystemBarsAppearance",MAIN)
    def test_android_26_to_29_has_legacy_path(self):
        self.assertIn("View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR",MAIN)
        self.assertIn("View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR",MAIN)
        self.assertIn("window.decorView.systemUiVisibility",MAIN)
    def test_older_os_has_protected_bar_surfaces(self):
        self.assertIn("window.statusBarColor = surface",MAIN)
        self.assertIn("window.navigationBarColor = surface",MAIN)
    def test_every_render_recalculates_contrast(self):
        self.assertIn("private fun render() {\n        updateSystemBarContrast()",MAIN)
        self.assertIn('prefs.edit().putString("appearance",choice).apply();render()',MAIN)

if __name__ == "__main__": unittest.main()
