def rotate_right(value, bits):
    amount = bits & 7
    return ((value >> amount) | (value << (8 - amount))) & 0xFF

def swap_nibbles(value):
    return ((value >> 4) & 0x0F) | ((value & 0x0F) << 4)

def decode(b_list):
    key1 = (0x2E << 1) & 0xFF
    key2 = ((0x51 << 1) | 1) & 0xFF
    key3 = (0x3B ^ 0x04) & 0xFF
    rotation = 9 - 6
    shift = (34 >> 1) & 0xFF
    res = bytearray()
    for index, b in enumerate(b_list):
        value = b & 0xFF
        value = value ^ ((key3 ^ ((index * 7 + 13) & 0xFF)) & 0xFF)
        value = swap_nibbles(value)
        value = (value - shift) & 0xFF
        value = value ^ key2
        value = rotate_right(value, rotation)
        value = value ^ key1
        res.append(value)
    return res.decode("utf-8", errors="replace")

bu = [3, 20, 27, 32, 168, 21, 156, 149, 77, 196, 84, 120, 67, 202, 68, 240, 244, 143, 1, 144, 23, 139, 46, 37, 57, 23, 73, 200, 95]
sk = [132, 190, 29, 87, 36, 157, 76, 152, 98, 187, 41, 162, 110, 18, 96, 249]
pn = [2, 156, 26, 131, 2, 182, 190, 53, 69, 194, 92, 212, 74, 227, 104, 113]
ua = [134, 153, 21, 34, 41, 50, 156, 30, 110, 238, 241, 113, 67]

print("BASE_URL:", decode(bu))
print("KEY:", decode(sk))
print("PKG:", decode(pn))
print("UA:", decode(ua))
