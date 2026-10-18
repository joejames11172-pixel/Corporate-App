"""Native bottom-navigation focus alignment source checks (device rendering pending)."""
import unittest
from pathlib import Path
SRC=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/peostectica/tapvoyage/MainActivity.kt").read_text(encoding="utf-8")
class TabVisibilityTests(unittest.TestCase):
    def test_scrollable_bottom_navigation(self):
        self.assertIn('val navigationScroller = HorizontalScrollView(this).apply',SRC)
        self.assertIn('root.addView(navigationScroller',SRC)
    def test_current_tab_revealed_after_page_navigation(self):
        self.assertIn('navigationScroller.post {',SRC)
        self.assertIn('nav.getChildAt(page.ordinal)',SRC)
    def test_centers_tab_within_viewport(self):
        self.assertIn('selectedTab.left + selectedTab.width/2 - navigationScroller.width/2',SRC)
        self.assertIn('navigationScroller.scrollTo(centered.coerceAtLeast(0),0)',SRC)
    def test_all_sections_share_the_navigation(self):
        self.assertIn('for (target in Page.values())',SRC)
        self.assertIn('page=target;render()',SRC)
if __name__=="__main__":unittest.main()
