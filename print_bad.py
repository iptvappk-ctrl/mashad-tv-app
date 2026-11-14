with open('MadarTV/app/src/main/java/com/madar/tv/data/ArabseedRepository.kt', 'r', encoding='utf-8', errors='ignore') as f:
    lines = f.readlines()

for i in range(120, 160):
    print(f"{i+1}: {lines[i].rstrip()}")
