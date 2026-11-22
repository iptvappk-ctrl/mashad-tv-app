with open('10_SCRATCH_AND_TOOLS/yacine.hprof', 'rb') as f:
    hprof = f.read()

pos = 17275461
print(hprof[pos-100:pos+300])
