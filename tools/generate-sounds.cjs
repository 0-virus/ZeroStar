// Original, deterministic PCM sound effects; no recorded or licensed samples.
const fs = require('node:fs');
for (const [name, notes] of Object.entries({upgrade:[660,880], growth:[523.25,659.25,783.99], collection:[523.25,659.25,783.99,1046.5]})) {
  const rate=22050, count=Math.round(rate*0.11*notes.length), data=Buffer.alloc(count*2);
  for(let i=0;i<count;i++) {
    const note=Math.min(notes.length-1,Math.floor(i/(rate*0.11)));
    const phase=(i%(rate*0.11))/(rate*0.11);
    const envelope=Math.min(1,phase*20)*(1-phase);
    data.writeInt16LE(Math.round(Math.sin(2*Math.PI*notes[note]*i/rate)*envelope*6500),i*2);
  }
  const h=Buffer.alloc(44); h.write('RIFF'); h.writeUInt32LE(36+data.length,4); h.write('WAVEfmt ',8); h.writeUInt32LE(16,16); h.writeUInt16LE(1,20); h.writeUInt16LE(1,22); h.writeUInt32LE(rate,24); h.writeUInt32LE(rate*2,28); h.writeUInt16LE(2,32); h.writeUInt16LE(16,34); h.write('data',36); h.writeUInt32LE(data.length,40);
  fs.writeFileSync(`app/src/main/res/raw/${name}.wav`,Buffer.concat([h,data]));
}
