with open('episode_main.html', 'r', encoding='utf-8', errors='ignore') as f:
    text = f.read()

print("First 1000 chars:")
print(repr(text[:1000]))
print("\nLast 500 chars:")
print(repr(text[-500:]))
