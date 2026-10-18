import unittest
from davg_transition_gate import validate

class TransitionTests(unittest.TestCase):
    def base(self):
        return {"defects":[{"id":"D-1","state":"OPEN"}]}
    def test_cannot_close_open_defect(self):
        x=self.base();x["attempt_close"]=True
        self.assertTrue(validate(x))
    def test_cannot_restart_without_correction(self):
        x=self.base();x["defects"][0]["state"]="RESTARTED"
        self.assertTrue(validate(x))
    def test_verified_requires_reproduction(self):
        x=self.base();x["defects"][0].update(state="VERIFIED",root_cause="x",correction_ref="commit",verification_ref="test")
        self.assertTrue(validate(x))
    def test_restart_with_evidence(self):
        x=self.base();x["defects"][0].update(state="RESTARTED",root_cause="x",correction_ref="commit",reproduction="original",verification_ref="test",restart_evidence="all gates")
        self.assertEqual([],validate(x))
    def test_external_dependency_owner_required(self):
        x=self.base();x["defects"][0]["state"]="EXTERNAL_BLOCKED"
        self.assertTrue(validate(x))
    def test_promotion_without_signing_rejected(self):
        x=self.base();x["attempt_promote"]=True
        self.assertTrue(validate(x))
    def test_promotion_requires_five_distinct_auditors(self):
        x=self.base();x["defects"][0].update(state="SUCCESS",root_cause="x",correction_ref="commit",reproduction="original",verification_ref="test",restart_evidence="full",independent_auditor_refs=["a"])
        x.update(attempt_promote=True,signed_release_evidence="sig",installed_device_evidence="device",five_auditor_refs=["a"]*5,ceo_approval_ref="approval")
        self.assertTrue(validate(x))
    def test_valid_open_iteration_without_close(self):
        self.assertEqual([],validate(self.base()))

if __name__=="__main__": unittest.main()
