import math, wave, json, hashlib, zipfile, datetime, os
from pathlib import Path
import numpy as np
OUT=Path(os.environ.get('PEO_AUDIO_OUT','./generated_audio')).resolve()
WAVS=OUT/'sounds';WAVS.mkdir(parents=True,exist_ok=True)
FS=22050
# Multiple sources of inspiration for the sound vocabulary; all audio generated here.
THEMES=[
('casino','Casino','Coins, slot bells and bright celebratory chimes','metal',988),
('arcade','Arcade','Bleeps, power-ups and springy joystick chirps','arcade',660),
('band','Concert Band','Brass punches, snares and festival flourishes','brass',329),
('sci-fi','Sci-Fi','Laser chirps, synthetic sweeps and teleport blips','laser',760),
('computer','Computer','Clean system bleeps, digital confirmations and keyboard chirps','computer',523),
('cathedral','Cathedral Bells','Resonant bell partials, tiny organ-like chords','bells',440),
('8-bit','Retro 8-Bit','Pixel blips, chip arpeggios and game-console jingles','chip',784),
('forest','Forest','Birdlike tweets, leaflike shimmers and gentle wood taps','forest',1200),
('ocean','Ocean','Watery bubbles, ripples and soft droplets','water',720),
('spacecraft','Spacecraft','Radar pings, cockpit chirps and launch-console signals','radar',580),
('workshop','Workshop','Metallic taps, small mechanisms and tool-like clicks','workshop',480),
('orchestra','Orchestra','Pizzicato strings, celesta and miniature orchestral phrases','strings',392),
('jazz','Jazz Club','Soft piano stabs, brushed taps and jazzy chromatic runs','jazz',349),
('carnival','Carnival','Calliope notes, cheerful chimes and carousel motifs','calliope',523),
('railway','Railway Station','Platform bells, train-like clicks and station chimes','rail',620),
('cyberpunk','Cyberpunk','Glitchy bass, neon synths and futuristic UI ticks','glitch',245),
('stadium','Sports Arena','Claps, taps and brief victory-signal tones','stadium',494),
('percussion','Percussion Studio','Woodblocks, hand drums and shakers','drums',440),
('fantasy','Fantasy Magic','Harp glints, stardust twinkles and spellbook tones','magic',880),
('vintage-radio','Vintage Radio','Radio ticks, dial chimes and soft static pips','radio',405)
]
CUES=['tap','select','confirm','back','swipe','toggle_on','toggle_off','open','close','success','notice','navigate']
PITCHES=np.array([0,2,4,5,7,9,11,12,14,16,19,-2]);

def smooth_noise(rng,n, width=25):
    raw=rng.normal(0,1,n)
    kernel=np.ones(width)/width
    return np.convolve(raw,kernel,mode='same')

def render(theme_i,cue_i):
    slug,title,desc,style,base=THEMES[theme_i]
    rng=np.random.default_rng(10517+theme_i*881+cue_i*337)
    duration=.25 + ((cue_i*3+theme_i)%5)*.07
    if style in ('forest','water','radio','drums','workshop'): duration += .06
    n=int(FS*duration);t=np.arange(n)/FS
    freq=base*2**((PITCHES[cue_i]/12)/2)
    if cue_i in (3,6,8): freq=freq*.82
    if cue_i in (9,10): freq=freq*1.08
    tt=t/duration
    chirp=(1-0.24*tt)
    wave0=np.zeros(n,dtype=np.float64)
    def sin(f, sweep=0, phase=0):
        return np.sin(2*np.pi*(f*t + .5*sweep*t*t)+phase)
    def env(a=.015,d=.10):
        attack=np.minimum(1,t/max(a,1e-3)); return attack*np.exp(-t/max(d,0.01))
    noise=rng.normal(0,1,n)
    if style=='metal':
        # Small bright strike + enharmonic metallic partials.
        for ratio,ampl in [(1,.6),(2.01,.3),(2.68,.19),(4.12,.13),(5.4,.07)]:
            wave0 += ampl*sin(freq*ratio)*env(.003,.10+ratio*.025)
        if cue_i%3==0: wave0+=.35*sin(freq*1.498)*env(.01,.18)
    elif style=='arcade':
        f=np.where(tt<.42,freq*1.0,freq*1.5)
        cycle=np.cumsum(f)/FS
        wave0=np.sign(np.sin(2*np.pi*cycle))*.58+sin(freq*2,300)*.14
        wave0*=env(.002,.13)
    elif style=='brass':
        vib=1+.003*np.sin(2*np.pi*6*t)
        for harmonic,amp in [(1,.5),(2,.32),(3,.22),(4,.12),(5,.05)]: wave0+=amp*np.sin(2*np.pi*freq*harmonic*vib*t)
        wave0*=env(.019,.14)
        wave0+=.09*noise*env(.002,.035)
    elif style=='laser':
        phase=2*np.pi*(freq*1.7*t-1200*t*t)
        wave0=(np.sin(phase)+.22*np.sin(3*phase)) * env(.001,.14)
    elif style=='computer':
        phase=2*np.pi*(freq*t + .5*130*t*t)
        wave0=(np.sin(phase)+.19*np.sin(2*phase))*env(.002,.09)
        wave0+=.06*noise*env(.001,.015)
    elif style=='bells':
        for ratio,amp,decay in [(1,.56,.21),(2.08,.2,.16),(2.81,.15,.23),(4.17,.095,.12)]:wave0+=amp*sin(freq*ratio)*env(.008,decay)
    elif style=='chip':
        step=np.floor(t*65)/65
        wave0=.48*np.sign(np.sin(2*np.pi*freq*step))+.24*np.sign(np.sin(2*np.pi*freq*2*step));wave0*=env(.001,.12)
    elif style=='forest':
        phase=2*np.pi*(freq*1.2*t+580*t*t)
        wave0=(np.sin(phase)*.48+np.sin(phase*1.76)*.20)*env(.015,.10)+.12*smooth_noise(rng,n,9)*env(.01,.2)
    elif style=='water':
        phase=2*np.pi*(freq*1.7*t-420*t*t)
        wave0=(.65*np.sin(phase)+.27*np.sin(1.3*phase))*env(.005,.115)+.1*smooth_noise(rng,n,13)*env(.003,.18)
    elif style=='radar':
        wave0=(.54*sin(freq,60)+.18*sin(freq*2,100))*env(.01,.23)
        wave0+=.12*sin(freq*.5)*env(.018,.25)
    elif style=='workshop':
        wave0=.45*noise*env(.002,.028)+.42*sin(freq*.55)*env(.001,.09)+.12*sin(freq*2.2)*env(.001,.13)
    elif style=='strings':
        for k in range(1,7):wave0+= (1/k**1.4)*np.sin(2*np.pi*freq*k*t + (k%2)*.1)
        wave0*=env(.015,.15)
    elif style=='jazz':
        intervals=[1,1.25,1.5]
        wave0=sum((.45/(j+1))*sin(freq*ratio) for j,ratio in enumerate(intervals))*env(.008,.15)
        wave0+=.10*noise*env(.001,.025)
    elif style=='calliope':
        # Quint/octave organ glimmer.
        wave0=(.48*sin(freq)+.24*sin(freq*2)+.16*sin(freq*1.5))*env(.011,.2)
    elif style=='rail':
        wave0=(.30*sin(freq)+.18*sin(freq*1.5))*env(.005,.13)
        clicks=(np.sin(2*np.pi*17*t)>0.96).astype(float)
        wave0+=.22*noise*clicks*env(.002,.15)
    elif style=='glitch':
        quant_t=np.floor(t*550)/550
        wave0=(.5*np.sin(2*np.pi*freq*t)+.17*np.sign(np.sin(2*np.pi*freq*2*quant_t)))*env(.002,.12)
        wave0+=.10*noise*(np.sin(2*np.pi*36*t)>0.55)*env(.002,.12)
    elif style=='stadium':
        wave0=.38*noise*env(.002,.035)+.35*sin(freq)*env(.002,.15)
        wave0+=.2*noise*env(.10,.025)
    elif style=='drums':
        phase=2*np.pi*(freq*.45*t-75*t*t)
        wave0=.45*np.sin(phase)*env(.002,.08)+.3*noise*env(.001,.027)
        if cue_i%2==0: wave0+=.12*smooth_noise(rng,n,3)*env(.0,.08)
    elif style=='magic':
        wave0=sum(.45/(j+1)*sin(freq*r, sweep=-50) for j,r in enumerate((1,1.25,1.5,2,2.5)))*env(.016,.21)
        wave0+=.10*sin(freq*4)*env(.02,.18)
    elif style=='radio':
        carrier=sin(freq,sweep=40)
        wave0=.45*np.tanh(2.5*carrier)*env(.001,.11)+.21*smooth_noise(rng,n,8)*env(.003,.12)
        wave0*=.8+.2*np.sin(2*np.pi*16*t)
    # Some variety in cue sequence with brief double notes: still one sound per action.
    if cue_i in (1,9,11):
        shift=int(FS*(.09 if cue_i==1 else .13));out0=wave0.copy()
        out0[shift:]+=wave0[:-shift]*(.28 if cue_i==11 else .45)
        wave0=out0
    if cue_i in (3,6,8):wave0=-wave0
    # smooth fade + bounded peak and low RMS; avoid very shrill repeated sounds.
    fade=int(FS*.012);wave0[:fade]*=np.linspace(0,1,fade);wave0[-fade:]*=np.linspace(1,0,fade)
    rms=np.sqrt(np.mean(wave0**2)+1e-12)
    wave0=wave0*min(.12/(rms+1e-9),.65/(np.max(np.abs(wave0))+1e-9))
    return (np.round(np.clip(wave0,-.75,.75)*32767)).astype('<i2'),float(duration),float(np.max(np.abs(wave0))),float(np.sqrt(np.mean(wave0**2)))

manifest={'schema':'peostectica.audio.theme.library.v1','generated_utc':'2026-10-17','purpose':'Original synthesized non-speech UI sound cues for mobile app testing; genre associations are creative, not measured popularity ranking','audio_files':'240 mono PCM WAV 22050Hz 16-bit','none_option':{'id':'none','name':'None · Sound effects off','default':True,'preview':False},'cue_order':CUES,'behavior':{'app_event':'Advance to next short cue (cycle index modulo 12)','preview':'User-triggered play/pause loop through 12 cues with ~160 ms gap','restore':'Back returns to prior screen; preference persists','mute':'None disables all effects; respect system silent/DND','volume':'App level adjustable','reduced_audio':'Use no sound by default and pause when backgrounded/interrupted'},'themes':[]}
for ix,theme in enumerate(THEMES):
    slug,title,description,style,base=theme
    folder=WAVS/f'{ix+1:02d}_{slug}';folder.mkdir(parents=True,exist_ok=True)
    sounds=[]
    for j,cue in enumerate(CUES):
        arr,duration,peak,rms=render(ix,j)
        filename=f'{j+1:02d}_{cue}.wav'
        with wave.open(str(folder/filename),'wb') as out:
            out.setnchannels(1);out.setsampwidth(2);out.setframerate(FS);out.writeframes(arr.tobytes())
        sounds.append({'cue_id':cue,'filename':f'{folder.name}/{filename}','duration_ms':round(duration*1000),'sha256':hashlib.sha256((folder/filename).read_bytes()).hexdigest(),'peak':round(peak,3),'rms':round(rms,3)})
    manifest['themes'].append({'id':f'{ix+1:02d}','slug':slug,'name':title,'description':description,'style':style,'sounds':sounds})
(OUT/'audio_themes_manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
# Optional distributable archive of files and manifest. Source generator included for reproducibility.
with zipfile.ZipFile(OUT/'Peostectica_20_Audio_Themes_240_Original_Cues.zip','w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    z.write(OUT/'audio_themes_manifest.json',arcname='audio_themes_manifest.json')
    z.write(Path(__file__),arcname='generate_audio.py')
    for f in sorted(WAVS.rglob('*.wav')):z.write(f,arcname='sounds/'+str(f.relative_to(WAVS)))
print('Generated',len(manifest['themes']), 'themes; files',sum(len(t['sounds']) for t in manifest['themes']))
print('Sample pack MB',round((OUT/'Peostectica_20_Audio_Themes_240_Original_Cues.zip').stat().st_size/1e6,2))