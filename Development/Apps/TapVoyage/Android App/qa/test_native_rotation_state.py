"""Native rotation state checks; requires device test before production acceptance."""
from pathlib import Path
import unittest
SRC=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/peostectica/tapvoyage/MainActivity.kt").read_text()
class NativeRotationStateTests(unittest.TestCase):
    def test_page_restores_from_saved_state(self):
        self.assertIn('Page.values().firstOrNull { it.name==saved.getString("page_name") }',SRC)
        self.assertIn('outState.putString("page_name",page.name)',SRC)
    def test_picked_contact_survives_rotation_only(self):
        for name in ("local_contact_id","local_contact_label"):
            self.assertIn('saved.getString("'+name+'")',SRC)
            self.assertIn('outState.putString("'+name+'"',SRC)
    def test_show_hidden_is_restored(self):
        self.assertIn('saved.getBoolean("show_hidden",false)',SRC)
        self.assertIn('outState.putBoolean("show_hidden",showHidden)',SRC)
    def test_does_not_restore_owner_grants(self):
        block=SRC.split("override fun onCreate(savedInstanceState:Bundle?)",1)[1].split("private fun render()",1)[0]
        for forbidden in ("ownerGrant","remoteSession","entitlement","authorizationToken","premiumFlag"):
            self.assertNotIn(forbidden,block)
if __name__=="__main__":
    unittest.main()
