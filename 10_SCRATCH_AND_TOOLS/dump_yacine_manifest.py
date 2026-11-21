import zipfile, subprocess

with open("10_SCRATCH_AND_TOOLS/find_intents.py") as f:
    pass

# We have aapt in Android SDK! Let us use aapt dump xmltree 10_SCRATCH_AND_TOOLS/yacine.apk AndroidManifest.xml
sdk_line = [l for l in open("01_ZAQZOUKA_CLIENT/MadarFlutter/android/local.properties").readlines() if l.startswith("sdk.dir=")][0]
sdk_dir = sdk_line.split("=")[1].strip().replace("\\\\", "\\").replace("\\:", ":")
import glob, os
aapts = glob.glob(os.path.join(sdk_dir, "build-tools", "*", "aapt.exe"))
if aapts:
    aapt = aapts[0]
    out = subprocess.check_output([aapt, "dump", "xmltree", "10_SCRATCH_AND_TOOLS/yacine.apk", "AndroidManifest.xml"]).decode("utf-8", errors="replace")
    print(out[:2000])
