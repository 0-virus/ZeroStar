from PIL import Image, ImageDraw, ImageFont
from pathlib import Path
out=Path('docs/release/assets'); out.mkdir(parents=True, exist_ok=True)
rows=['....##....','....##....','...####...','##########','.########.','..######..','..######..','.###..###.','.##....##.']
def star(draw,x,y,u):
    for j,row in enumerate(rows):
        for i,c in enumerate(row):
            if c=='#': draw.rectangle((x+i*u,y+j*u,x+(i+1)*u-1,y+(j+1)*u-1),fill='#ffd780')
    for i in (3,6): draw.rectangle((x+i*u,y+4*u,x+i*u+u//2,y+5*u),fill='#2b1b3d')
icon=Image.new('RGB',(512,512),'#241b4d'); d=ImageDraw.Draw(icon)
d.rectangle((0,350,512,512),fill='#4b2a7b'); star(d,76,83,36)
for x,y in [(42,55),(427,80),(420,420),(75,430)]: d.rectangle((x,y,x+10,y+10),fill='#fff8ec')
icon.save(out/'icon-512.png')
feature=Image.new('RGB',(1024,500),'#f6ead8'); d=ImageDraw.Draw(feature)
d.rectangle((20,20,1003,479),outline='#2b1b3d',width=6)
d.rectangle((50,50,450,450),fill='#241b4d'); star(d,90,100,32)
font='app/src/main/res/font/ibm_plex_sans_kr.ttf'
for text,y,size in [('별 키우기',105,66),('여섯 빛깔의 작은 우주',215,30),('잠시 쉬는 동안, 별은 자라요',275,26),('ZeroStar',352,25)]:
    d.text((490,y),text,font=ImageFont.truetype(font,size),fill='#2b1b3d')
feature.save(out/'feature-1024x500.png')
