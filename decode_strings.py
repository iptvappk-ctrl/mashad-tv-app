import sys
import re

def ror(val, bits):
    amt = bits & 7
    return (((val & 0xFF) >> amt) | ((val & 0xFF) << (8 - amt))) & 0xFF

def swap_nibbles(val):
    val &= 0xFF
    return ((val >> 4) & 0x0F) | ((val & 0x0F) << 4)

def decode_x(b_list):
    key1 = 0x2E << 1
    key2 = (0x51 << 1) | 1
    key3 = 0x3B ^ 0x04
    rotation = 9 - 6
    shift = 34 >> 1
    
    out = bytearray()
    for idx, b in enumerate(b_list):
        val = b & 0xFF
        val ^= ((key3 ^ ((idx * 7 + 13) & 0xFF)) & 0xFF)
        val = swap_nibbles(val)
        val = (val - shift) & 0xFF
        val ^= key2
        val = ror(val, rotation)
        val ^= key1
        out.append(val)
    return out.decode('utf-8', errors='replace')

with open('ArabseedRepo_old.kt', encoding='utf-16', errors='ignore') as f:
    text = f.read()

# Find all val _xx = byteArrayOf(...)
for m in re.finditer(r'val\s+(_[a-zA-Z0-9_]+)\s*=\s*byteArrayOf\(([^)]+)\)', text):
    var_name = m.group(1)
    raw_nums = m.group(2)
    nums = []
    for token in raw_nums.split(','):
        t = token.replace('.toByte()', '').strip()
        if t:
            try:
                nums.append(int(t))
            except:
                pass
    try:
        decoded = decode_x(nums)
        print(f"{var_name} = {repr(decoded)}")
    except Exception as e:
        print(f"{var_name} = error {e}")
