"""Read-only APK native alignment inventory; runtime 16KB testing is still required."""
import json, struct, sys, zipfile
from pathlib import Path
path=Path(sys.argv[1])
rows=[]
with zipfile.ZipFile(path) as archive, path.open('rb') as raw:
    for entry in archive.infolist():
        if not entry.filename.endswith('.so'): continue
        data=archive.read(entry)
        if data[:4] != b'\x7fELF': raise ValueError('Unexpected native format')
        endian='<' if data[5]==1 else '>'
        bits=data[4]
        if bits==2:
            offset=struct.unpack_from(endian+'Q',data,32)[0]
            stride,count=struct.unpack_from(endian+'HH',data,54)
            align_offset,align_type=48,'Q'
        else:
            offset=struct.unpack_from(endian+'I',data,28)[0]
            stride,count=struct.unpack_from(endian+'HH',data,42)
            align_offset,align_type=28,'I'
        aligns=[struct.unpack_from(endian+align_type,data,offset+i*stride+align_offset)[0]
                for i in range(count) if struct.unpack_from(endian+'I',data,offset+i*stride)[0]==1]
        relro=[]
        for i in range(count):
            base=offset+i*stride
            if struct.unpack_from(endian+'I',data,base)[0] != 0x6474e552: continue
            virtual=struct.unpack_from(endian+('Q' if bits==2 else 'I'),data,base+(16 if bits==2 else 8))[0]
            memory=struct.unpack_from(endian+('Q' if bits==2 else 'I'),data,base+(40 if bits==2 else 20))[0]
            relro.append((virtual+memory)%16384==0)
        raw.seek(entry.header_offset+26)
        names,extra=struct.unpack('<HH',raw.read(4))
        payload=entry.header_offset+30+names+extra
        rows.append(dict(path=entry.filename,elf_bits=64 if bits==2 else 32,
                         load_alignments=aligns,elf_16kb=bool(aligns) and min(aligns)>=16384,
                         relro_16kb=all(relro),compressed=entry.compress_type!=0,zip_offset=payload,
                         zip_16kb=payload%16384==0))
print(json.dumps(dict(apk=str(path),native_libraries=rows),indent=2))

