"""Source-level UI wiring tests. Actual Android build/render still required."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
MAIN = (ROOT / "app/src/main/java/com/peostectica/tapvoyage/MainActivity.kt").read_text(encoding="utf-8")
POLICY = (ROOT / "app/src/main/java/com/peostectica/tapvoyage/core/TapVoyagePolicy.kt").read_text(encoding="utf-8")

class CircleVisibilityUiTests(unittest.TestCase):
    def test_local_identity_selected_not_phone_number(self):
        self.assertIn("ContactsContract.CommonDataKinds.Phone.CONTACT_ID", MAIN)
        self.assertIn("ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME", MAIN)
        self.assertNotIn("Phone.NUMBER", MAIN)
        self.assertIn("selectedContactId = id", MAIN)

    def test_actual_hide_show_unhide_controls(self):
        for text in ('button("Hide Me")','button("Unhide")', '"Show Hidden: ON"', '"Show Hidden: OFF"'):
            self.assertIn(text, MAIN)

    def test_hidden_status_uses_stable_contact_id(self):
        self.assertIn("hiddenContacts.visible(selectedId, showHidden)", MAIN)
        self.assertIn("hiddenContacts.hide(selectedId)", MAIN)
        self.assertIn("hiddenContacts.unhide(selectedId)", MAIN)

    def test_hidden_ids_persist_without_names_or_numbers(self):
        self.assertIn('getStringSet("hidden_contact_ids"', MAIN)
        self.assertIn('putStringSet("hidden_contact_ids", hiddenContacts.ids())', MAIN)
        self.assertNotIn('putString("selectedContact"', MAIN)
        self.assertIn("fun ids():Set<String> = hidden.toSet()", POLICY)

    def test_local_hide_handlers_do_not_manage_remote_authority(self):
        chunk = MAIN.split('button("Hide Me")',1)[1].split('button("Choose a phone contact")',1)[0]
        for prohibited in ("ownerRevokeRequester(", "ownerApproveApp(", "requestSession(", "ownerAccept(", "ownerApproveFullDevice("):
            self.assertNotIn(prohibited,chunk)

    def test_activity_does_not_create_offline_authorization_state(self):
        self.assertNotIn("TapVoyagePolicy()", MAIN)
        self.assertNotIn("private val policy =", MAIN)

if __name__ == "__main__":
    unittest.main()
