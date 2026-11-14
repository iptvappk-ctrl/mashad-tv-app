with open('MadarTV/app/src/main/java/com/madar/tv/ui/ArabseedScreen.kt', 'r', encoding='utf-8', errors='ignore') as f:
    lines = f.readlines()

for i in range(3160, 3255):
    print(f"{i+1}: {lines[i].rstrip().encode('ascii', 'backslashreplace').decode('ascii')}")
