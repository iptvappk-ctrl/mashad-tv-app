with open('ArabseedRepo_old.kt', encoding='utf-16', errors='ignore') as f:
    text = f.read()

import re
matches = re.findall(r'(private val [a-zA-Z0-9_]+.*)', text)
with open('constants.txt', 'w', encoding='utf-8') as out:
    for m in matches:
        out.write(m + '\n')
print("Wrote constants.txt, count:", len(matches))
