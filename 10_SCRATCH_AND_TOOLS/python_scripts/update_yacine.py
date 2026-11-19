with open("MadarTV/app/src/main/java/com/madar/tv/data/YacineRepository.kt", "r", encoding="utf-8") as f:
    text = f.read()

old_str = """.filter { it.url.isNotEmpty() }
         .sortedBy { if (it.url.contains("rex.new-redirect.online")) 1 else 0 }"""
new_str = """.filter { it.url.isNotEmpty() && !it.url.contains("rex.new-redirect.online") }"""

if old_str in text:
    text = text.replace(old_str, new_str)
elif old_str.replace("\n", "\r\n") in text:
    text = text.replace(old_str.replace("\n", "\r\n"), new_str)
else:
    print("Could not find old_str in YacineRepository.kt")

with open("MadarTV/app/src/main/java/com/madar/tv/data/YacineRepository.kt", "w", encoding="utf-8") as f:
    f.write(text)
print("Updated YacineRepository.kt")
