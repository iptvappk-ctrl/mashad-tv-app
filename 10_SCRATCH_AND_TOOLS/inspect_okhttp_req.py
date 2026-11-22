with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    hprof = f.read()

pos = 17275545
# Print 1000 bytes before and after
chunk = hprof[max(0, pos-2000):pos+2000]

# Search for any header names or values
cleaned = ''.join(chr(b) if 32 <= b < 127 else ' ' for b in chunk)
print("Around request:")
for line in cleaned.split('   '):
    line = line.strip()
    if len(line) > 5:
        print("  ", line)
