import glob, json, re

for f in glob.glob('10_SCRATCH_AND_TOOLS/*'):
    if f.endswith('.json') or f.endswith('.txt') or f.endswith('.py'):
        try:
            with open(f, 'r', encoding='utf-8', errors='ignore') as fp:
                content = fp.read()
                matches = re.findall(r'https?://[^\s\"\'\<\>]+(?:\.m3u8|\.mpd|\.ts|\/live\/|\/stream\/)[^\s\"\'\<\>]*', content)
                if matches:
                    print(f'{f}: {len(matches)} streams found')
                    for m in set(matches[:10]):
                        print('  ', m)
        except Exception as e:
            pass
