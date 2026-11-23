with open('10_SCRATCH_AND_TOOLS/yacine.apk', 'rb') as f:
    import zipfile
    z = zipfile.ZipFile(f)
    dex = z.read('classes.dex')

import re
uas = re.findall(rb'(?:User-Agent|user-agent|referer|Referer)[^\x00\r\n]{0,80}', dex)
for u in uas[:20]:
    print(u)
