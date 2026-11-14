with open('MadarTV/app/src/main/java/com/madar/tv/ui/ArabseedScreen.kt', 'r', encoding='utf-8', errors='ignore') as f:
    text = f.read()

import re
matches = re.findall(r'item\.providerName\s*==\s*"([^"]*)"', text)
print("providerName comparisons in ArabseedScreen.kt:")
for m in set(matches):
    print(" -", repr(m))
