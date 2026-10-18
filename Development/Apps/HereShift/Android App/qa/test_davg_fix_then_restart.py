"""Structural regression tests for the mandatory DAVG workflow."""
from pathlib import Path
import unittest

DOC = (Path(__file__).parent / "DAVG_FIX_THEN_RESTART_WORKFLOW.md").read_text()

class DAVGWorkflowTests(unittest.TestCase):
    def test_correction_precedes_restart(self):
        self.assertLess(DOC.index("3. **CORRECT**"), DOC.index("5. **RESTART ONLY AFTER CORRECTION**"))
    def test_verification_precedes_restart(self):
        self.assertLess(DOC.index("4. **VERIFY THE FIX**"), DOC.index("5. **RESTART ONLY AFTER CORRECTION**"))
    def test_rejected_cannot_be_terminal_action(self):
        self.assertIn("never a permission to stop", DOC)
        self.assertIn("NOT to STOP and NOT directly to RESTART", DOC)
    def test_repeated_failures_require_correction(self):
        self.assertIn("Never stop at a REJECTED report while a feasible corrective action remains", DOC)
    def test_external_dependency_must_be_reported(self):
        self.assertIn("request only the necessary authorized action", DOC)
        self.assertIn("continue all other independent corrections", DOC)
    def test_report_contains_fix_evidence(self):
        self.assertIn("root cause, exact corrective commits/actions, reproduction and verification results", DOC)

if __name__ == "__main__": unittest.main()
