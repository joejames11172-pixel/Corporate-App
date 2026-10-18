"""TapVoyage 20-theme audio source regression tests; device audio QA is separate."""
from pathlib import Path
import unittest
import json
import re
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'app/src/main/java/com/peostectica/tapvoyage/MainActivity.kt').read_text()
GRADLE=(ROOT/'app/build.gradle.kts').read_text()
GENERATE=(ROOT/'qa/generate_audio_assets.py').read_text()
class AudioNativeTests(unittest.TestCase):
    def test_all_20_themes_read_from_packaged_manifest(self):
        self.assertIn('assets.open("audio/audio_themes_manifest.json")',MAIN)
        self.assertIn('for (i in 0 until audioThemes.length())',MAIN)
        self.assertIn('ids.add(theme.getString("id"))',MAIN)
    def test_none_is_default_and_persisted(self):
        self.assertIn('prefs.getString("audio_theme", "none")',MAIN)
        self.assertIn('putString("audio_theme",ids[position])',MAIN)
    def test_all_12_original_cues_preview_with_gap(self):
        self.assertIn('val sounds=item.getJSONArray("sounds")',MAIN)
        self.assertIn('playNext(index+1)',MAIN)
        self.assertIn('handler.postDelayed({playNext(index+1)},160)',MAIN)
    def test_silent_mode_and_background_stop(self):
        self.assertIn('audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL',MAIN)
        self.assertIn('override fun onPause()',MAIN)
        self.assertIn('stopAudioPreview()',MAIN)
    def test_wav_files_are_stored_uncompressed(self):
        self.assertIn('androidResources { noCompress += "wav" }',GRADLE)
        self.assertIn('dependsOn(generateApprovedAudio)',GRADLE)
    def test_generator_anchors_to_approved_manifest_hash(self):
        self.assertIn('aa8d8e450c2af878b3f88eff434e1d1280ef49e7c1a3e42853486522c0c5c789',GENERATE)
        self.assertIn('TAPVOYAGE_AUDIO_OUT',GENERATE)
    def test_event_sounds_cycle_through_approved_cues(self):
        self.assertIn('private fun playUiFeedback()',MAIN)
        self.assertIn('feedbackIndex=(feedbackIndex+1)%sounds.length()',MAIN)
        self.assertIn('setOnClickListener { if (page != Page.AUDIO) playUiFeedback(); action() }',MAIN)
    def test_volume_control_is_saved_and_applied(self):
        self.assertIn('prefs.getInt("audio_volume", 60)',MAIN)
        self.assertIn('prefs.edit().putInt("audio_volume",value).apply()',MAIN)
        self.assertIn('player.setVolume(soundLevel,soundLevel)',MAIN)
        self.assertIn('contentDescription="Audio effect volume"',MAIN)
    def test_event_sound_resource_cleanup_on_background(self):
        self.assertIn('feedbackPlayer?.release()',MAIN)
        self.assertIn('override fun onPause()',MAIN)
if __name__=="__main__": unittest.main()
