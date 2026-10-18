"""Regression tests for the package verifier using NON-INSTALLABLE ZIP fixtures."""
from pathlib import Path
import hashlib
import io
import json
import tempfile
import unittest
import zipfile
from verify_packaged_audio import verify
import verify_packaged_audio as gate

CUES=["tap","select","confirm","back","swipe","toggle_on","toggle_off",
      "open","close","success","notice","navigate"]

class AudioPackageVerifierTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.old=gate.EXPECTED_MANIFEST_SHA256
        self.addCleanup(lambda: setattr(gate,"EXPECTED_MANIFEST_SHA256",self.old))
        self.samples={}
        themes=[]
        for i in range(1,21):
            entries=[]
            for j,cue in enumerate(CUES,1):
                name=f"{i:02d}_original/{j:02d}_{cue}.wav"
                raw=b"RIFF"+bytes([i,j])*21+b"WAVE"
                self.samples[name]=raw
                entries.append({"filename":name,"sha256":hashlib.sha256(raw).hexdigest()})
            themes.append({"id":f"{i:02d}","sounds":entries})
        self.manifest=json.dumps({"themes":themes,"cue_order":CUES},sort_keys=True).encode()
        gate.EXPECTED_MANIFEST_SHA256=hashlib.sha256(self.manifest).hexdigest()

    def fixture(self,kind="good"):
        stream=io.BytesIO()
        with zipfile.ZipFile(stream,"w") as z:
            raw=self.manifest if kind!="bad_manifest" else self.manifest+b"bad"
            z.writestr(gate.MANIFEST,raw)
            for n,(name,sound) in enumerate(self.samples.items()):
                if kind=="missing" and n==0:continue
                if kind=="tampered" and n==0:sound+=b"changed"
                mode=zipfile.ZIP_DEFLATED if kind=="compressed" and n==0 else zipfile.ZIP_STORED
                z.writestr("assets/audio/sounds/"+name,sound,compress_type=mode)
        stream.seek(0)
        return stream

    def test_complete_240_audio_package_passes(self):
        self.assertEqual([],verify(self.fixture()))

    def test_missing_wav_rejected(self):
        self.assertTrue(any("Missing WAV" in issue for issue in verify(self.fixture("missing"))))

    def test_mutated_wav_rejected(self):
        self.assertTrue(any("Altered WAV" in issue for issue in verify(self.fixture("tampered"))))

    def test_compressed_wav_rejected(self):
        self.assertTrue(any("Compressed WAV" in issue for issue in verify(self.fixture("compressed"))))

    def test_modified_manifest_rejected(self):
        self.assertIn("Packaged audio manifest is not approved",verify(self.fixture("bad_manifest")))

if __name__=="__main__":
    unittest.main()
