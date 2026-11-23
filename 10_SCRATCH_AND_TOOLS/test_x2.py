key1 = (0x2E << 1) & 0xFF
key2 = ((0x51 << 1) | 1) & 0xFF
key3 = (0x3B ^ 0x04) & 0xFF
rotation = 3
shift = 17

def rotate_right(val, bits):
    amt = bits & 7
    return ((val >> amt) | (val << (8 - amt))) & 0xFF

def swap_nibbles(val):
    return (((val >> 4) & 0x0F) | ((val & 0x0F) << 4)) & 0xFF

def decode_x(b_list):
    res = bytearray()
    for idx, b in enumerate(b_list):
        val = b & 0xFF
        val ^= (key3 ^ ((idx * 7 + 13) & 0xFF)) & 0xFF
        val = swap_nibbles(val)
        val = (val - shift) & 0xFF
        val ^= key2
        val = rotate_right(val, rotation)
        val ^= key1
        res.append(val)
    return res.decode('utf-8', errors='ignore')

bu = [3, 20, 27, 32, 168, 21, 156, 149, 77, 196, 84, 120, 67, 202, 68, 240, 244, 143, 1, 144, 23, 139, 46, 37, 57, 23, 73, 200, 95]
sk = [132, 190, 29, 87, 36, 157, 76, 152, 98, 187, 41, 162, 110, 18, 96, 249]
pn = [2, 156, 26, 131, 2, 182, 190, 53, 69, 194, 92, 212, 74, 227, 104, 113]
ua = [134, 153, 21, 34, 41, 50, 156, 30, 110, 238, 241, 113, 67]

print("BU:", decode_x(bu))
print("SK:", decode_x(sk))
print("PN:", decode_x(pn))
print("UA:", decode_x(ua))
