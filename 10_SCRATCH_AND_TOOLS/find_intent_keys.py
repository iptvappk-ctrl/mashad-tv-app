import zipfile, re

with open('10_SCRATCH_AND_TOOLS/yacine.apk', 'rb') as f:
    apk = zipfile.ZipFile(f)
    dex = apk.read('classes.dex')

# Search for strings near SplashActivity
pos = dex.find(b'SplashActivity')
print("SplashActivity pos:", pos)
# Let's inspect 500 bytes around pos
print(dex[pos-100:pos+200])
