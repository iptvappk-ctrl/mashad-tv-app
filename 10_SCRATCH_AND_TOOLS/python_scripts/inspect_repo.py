import re

with open('ArabseedRepo_old.kt', encoding='utf-16', errors='ignore') as f:
    old = f.read()

for line in old.splitlines()[:50]:
    print(line)
