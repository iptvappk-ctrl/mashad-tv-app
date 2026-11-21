import subprocess, glob, os
sdk_line = [l for l in open("01_ZAQZOUKA_CLIENT/MadarFlutter/android/local.properties").readlines() if l.startswith("sdk.dir=")][0]
sdk_dir = sdk_line.split("=")[1].strip().replace("\\\\", "\\").replace("\\:", ":")
aapt = glob.glob(os.path.join(sdk_dir, "build-tools", "*", "aapt.exe"))[0]
out = subprocess.check_output([aapt, "dump", "xmltree", "10_SCRATCH_AND_TOOLS/yacine.apk", "AndroidManifest.xml"]).decode("utf-8", errors="replace")

for line in out.splitlines():
    if "queries" in line or "package" in line or "activity" in line:
        print(line)
