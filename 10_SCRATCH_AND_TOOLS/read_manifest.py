import zipfile, xml.etree.ElementTree as ET

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk') as z:
    manifest_data = z.read('AndroidManifest.xml')
    # Extract package name and strings
    import re
    strings = re.findall(b'[\x20-\x7e]{4,}', manifest_data)
    for s in strings:
        s_dec = s.decode('latin1')
        if any(k in s_dec.lower() for k in ['package', 'yacine', 'tivi', 'activity']):
            print(s_dec)
