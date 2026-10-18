import json,re,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def luminance(hex_color):
    rgb=[int(hex_color[i:i+2],16)/255 for i in (1,3,5)]
    channels=[v/12.92 if v<=0.04045 else ((v+0.055)/1.055)**2.4 for v in rgb]
    return 0.2126*channels[0]+0.7152*channels[1]+0.0722*channels[2]
def contrast(a,b):
    x,y=sorted([luminance(a),luminance(b)],reverse=True)
    return (x+0.05)/(y+0.05)
class ThemeTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.raw=(ROOT/'app/src/main/assets/themes.40.approved.json').read_bytes()
        cls.themes=json.loads(cls.raw)['presets']
        cls.activity=(ROOT/'app/src/main/java/com/peostectica/tapvoyage/MainActivity.kt').read_text()
    def test_40_distinct_approved_pairs(self):
        self.assertEqual(40,len(self.themes))
        self.assertEqual(40,len({v['id'] for v in self.themes}))
        self.assertTrue(all(v['approved'] for v in self.themes))
    def test_80_palettes_valid(self):
        for preset in self.themes:
            for mode in ('light','dark'):
                c=preset[mode]
                for key in ('background','surface','onBackground','onSurface','primary','onPrimary','outline'):
                    self.assertRegex(c[key],r'^#[0-9a-fA-F]{6}$')
    def test_text_contrast_4_5(self):
        for preset in self.themes:
            for mode in ('light','dark'):
                c=preset[mode]
                self.assertGreaterEqual(contrast(c['onBackground'],c['background']),4.5)
                self.assertGreaterEqual(contrast(c['onPrimary'],c['primary']),4.5)
    def test_native_theme_tokens_and_persistence(self):
        for text in ('assets.open("themes.40.approved.json")','prefs.getString("theme_id","01")','putString("theme_id",next)','getJSONObject(if(night()) "dark" else "light")','Select one of 40 display color themes'):
            self.assertIn(text,self.activity)
    def test_no_hardcoded_palette(self):
        self.assertNotIn('private val bg: Int get() = if (night()) Color.rgb(',self.activity)
if __name__=='__main__':unittest.main()
