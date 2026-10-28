path = r"c:\Users\hassa\Desktop\all project\iptv app\MadarTV\app\src\main\java\com\madar\tv\data\ArabseedRepository.kt"

with open(path, "r", encoding="utf-8") as f:
    text = f.read()

import re
old_block = r"if\s*\(!serverUrl\.contains\(GOVID_PLAY\)\)\s*\{\s*if\s*\(serverUrl\.contains\(\"\.m3u8\"\)\s*\|\|\s*serverUrl\.contains\(\"\.mp4\"\)\)\s*\{\s*streamCache\[serverUrl\]\s*=\s*serverUrl\s*return serverUrl\s*\}\s*return null\s*\}"
new_block = """if (!serverUrl.contains(GOVID_PLAY)) {
                streamCache[serverUrl] = serverUrl
                return serverUrl
            }"""

subbed, count = re.subn(old_block, new_block, text)
print(f"Replaced {count} occurrences")

if count > 0:
    with open(path, "w", encoding="utf-8", newline="\r\n") as f:
        f.write(subbed)
