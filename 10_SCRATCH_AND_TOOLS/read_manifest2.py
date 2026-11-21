import zipfile, struct

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk') as z:
    raw = z.read('AndroidManifest.xml')
    # Android binary xml:
    # magic 0x00080003
    # string pool chunk starts at offset 8
    # chunk type = 0x001C0001
    string_count = struct.unpack('<I', raw[16:20])[0]
    strings_offset = struct.unpack('<I', raw[28:32])[0]
    print(f"String count: {string_count}")
    # find strings using regex of utf-16 or utf-8
    import re
    # find all readable ASCII
    matches = re.findall(b'[a-zA-Z0-9._]{5,}', raw)
    for m in set(matches):
        m_str = m.decode('ascii')
        if any(k in m_str.lower() for k in ['ycn', 'yacine', 'ver', 'package', 'live', 'app']):
            print(m_str)
