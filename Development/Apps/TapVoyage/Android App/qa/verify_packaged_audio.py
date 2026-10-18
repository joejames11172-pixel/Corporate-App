#!/usr/bin/env python3
"""Fail closed if a TapVoyage signed release is missing any CEO-approved WAV.
This verifies APK contents only, never release readiness or auditor approval.
"""
import hashlib, json, sys, zipfile
from pathlib import Path
EXPECTED_MANIFEST_SHA256="aa8d8e450c2af878b3f88eff434e1d1280ef49e7c1a3e42853486522c0c5c789"
MANIFEST="assets/audio/audio_themes_manifest.json"

def verify(path):
    issues=[]
    try:
        with zipfile.ZipFile(path) as z:
            info={x.filename:x for x in z.infolist()}
            if MANIFEST not in info: return ["Missing packaged approved audio manifest"]
            raw=z.read(MANIFEST)
            if hashlib.sha256(raw).hexdigest()!=EXPECTED_MANIFEST_SHA256:
                return ["Packaged audio manifest is not approved"]
            data=json.loads(raw)
            themes=data.get("themes",[])
            cues=data.get("cue_order",[])
            if len(themes)!=20 or len(cues)!=12:
                issues.append("Wrong number of themes/cues")
            required=[]
            for theme in themes:
                sounds=theme.get("sounds",[])
                if len(sounds)!=12:issues.append("Theme has wrong cue count: "+str(theme.get("id")))
                for sound in sounds:
                    name="assets/audio/sounds/"+sound["filename"]
                    required.append(name)
                    if name not in info:
                        issues.append("Missing WAV: "+name)
                        continue
                    if info[name].compress_type!=zipfile.ZIP_STORED:
                        issues.append("Compressed WAV cannot use AssetFileDescriptor: "+name)
                    if hashlib.sha256(z.read(name)).hexdigest()!=sound["sha256"]:
                        issues.append("Altered WAV: "+name)
            if len(required)!=240 or len(set(required))!=240:
                issues.append("Wrong number/duplicate names of approved WAVs")
            actual=[n for n in info if n.startswith("assets/audio/sounds/") and n.endswith(".wav")]
            if set(actual)!=set(required):
                issues.append("Unexpected or incomplete sound inventory")
    except (OSError,ValueError,KeyError,zipfile.BadZipFile) as err:
        issues.append("Unreadable audio artifact: "+str(err))
    return issues

if __name__=="__main__":
    if len(sys.argv)!=2: sys.exit("usage: verify_packaged_audio.py SIGNED_RELEASE_APK")
    issues=verify(Path(sys.argv[1]))
    print(json.dumps({"verdict":"PASS_AUDIO_PACKAGING_ONLY" if not issues else "REJECT",
                      "issues":issues},indent=2))
    sys.exit(bool(issues))
