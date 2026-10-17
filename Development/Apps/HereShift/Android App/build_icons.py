from pathlib import Path
import cairosvg
base=Path(__file__).resolve().parent
resources=base/'app/src/main/res/drawable-nodpi'
resources.mkdir(parents=True,exist_ok=True)
for stem,suffix in [('hereshift_icon_light','HereShift_Icon_15_light.svg'),('hereshift_icon_dark','HereShift_Icon_20_dark.svg')]:
    source=base/'branding'/suffix
    assert source.is_file()
    cairosvg.svg2png(url=str(source),write_to=str(resources/(stem+'.png')),output_width=432,output_height=432)