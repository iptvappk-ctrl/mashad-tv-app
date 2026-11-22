import zipfile

with zipfile.ZipFile('10_SCRATCH_AND_TOOLS/yacine.apk', 'r') as z:
    data = z.read('classes.dex')
    # search where EventFragment or dev/google/yacinetv/ui/event is used
    # let's look for method names or field names
    for m in [b'EventFragment', b'onEventClick', b'EventAdapter', b'startActivity', b'channel_id', b'channel']:
        count = data.count(m)
        print(f'{m.decode()}: {count} occurrences')
