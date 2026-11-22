import zipfile

with open('10_SCRATCH_AND_TOOLS/yacine.apk', 'rb') as f:
    apk = zipfile.ZipFile(f)
    dex = apk.read('classes.dex')

# Search for SplashActivity string offset
pos_splash = dex.find(b'dev.google.ytvplayer.ui.splash.SplashActivity')
print("SplashActivity string offset:", pos_splash)

# Search for package string
pos_pkg = dex.find(b'com.ytv.pronew')
print("com.ytv.pronew offset:", pos_pkg)
