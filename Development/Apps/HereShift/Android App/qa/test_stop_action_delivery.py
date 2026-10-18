from pathlib import Path
import unittest

BASE=Path(__file__).resolve().parents[1]/"app/src/main/java/com/peostectica/hereshift"
MAIN=(BASE/"MainActivity.kt").read_text()
SERVICE=(BASE/"MockLocationService.kt").read_text()
class StopDeliveryTests(unittest.TestCase):
    def test_stop_sends_action_to_service(self):
        block=MAIN[MAIN.index("private fun stopMock()"):MAIN.index("private fun renderDisplay(")]
        self.assertIn("setAction(MockLocationService.ACTION_STOP)",block)
        self.assertIn("startService(",block)
        self.assertNotIn("stopService(",block.split("try {")[1])
    def test_stop_does_not_overwrite_service_status_after_delivery(self):
        block=MAIN[MAIN.index("private fun stopMock()"):MAIN.index("private fun renderDisplay(")]
        self.assertLess(block.index('"Stop requested"'),block.index("startService("))
    def test_service_handles_stop_and_reports_cleanup(self):
        block=SERVICE[SERVICE.index("if (intent?.action == ACTION_STOP)"):SERVICE.index("if (intent?.action != ACTION_START)")]
        self.assertIn("stopProvider()",block)
        self.assertIn("provider cleanup could not be confirmed",block)
    def test_failed_delivery_warns_user(self):
        block=MAIN[MAIN.index("private fun stopMock()"):MAIN.index("private fun renderDisplay(")]
        self.assertIn("Could not confirm simulation stopped",block)

if __name__=="__main__": unittest.main()
