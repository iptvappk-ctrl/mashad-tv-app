import re

path = r"c:\Users\hassa\Desktop\all project\iptv app\MadarTV\app\src\main\java\com\madar\tv\ui\ArabseedScreen.kt"

with open(path, "r", encoding="utf-8") as f:
    text = f.read()

# find provider badge block
pattern = r"// Provider badge top-left.*?\n\s*if\s*\(\s*item\.providerName\.isNotBlank\(\).*?\}\s*\}\s*\}\s*"
subbed, count = re.subn(pattern, "", text, flags=re.DOTALL)
print(f"Removed {count} matches")

with open(path, "w", encoding="utf-8") as f:
    f.write(subbed)
