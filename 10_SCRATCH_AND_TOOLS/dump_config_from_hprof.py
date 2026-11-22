with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    hprof = f.read()

pos = 9627291
start = max(0, pos - 1500)
chunk = hprof[start:pos+100]

# Find '{'
first_brace = chunk.find(b'{"')
print("first_brace:", first_brace)
if first_brace != -1:
    print(chunk[first_brace:first_brace+300])
