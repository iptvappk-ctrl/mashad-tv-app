import urllib.request, xml.etree.ElementTree as ET

ids = [
    'UCJUCcJUeh0Cz2xyKwkw5Q1w', 'UCEPBbIhMIssHjm6KamAMVP8', 'UCHH2NBAM584fJX90v4y3M1i', 
    'UCEJSsCRgBIhMIssHjm6KamA', 'UC0yRktXU1pPSTRmWlVOekN5', 'UCRvbT2DSo9Ce4nJ6KfeiUH5', 
    'UC1aUkFfVjdhLVFLbUJrajls', 'UCwEThskJTu5edvGVy0vByU5', 'UC7TETKJDRTzm0KSP764aAfA', 
    'UCrcIxUlgdxyaRrqxnKHxO7N', 'UCENTEDBgLIhMIssHjm6KamA', 'UC_dx9tDEYPStvr_EkeO2A3G', 
    'UCHKmKeaneUDxWe9X8PpUSb8', 'UCRTMcoHMXFm3plrG8SfWNyj', 'UC0zQXU4cHh6YVprcG9RLXhh', 
    'UCKIEAIJiDbofPspOun8xreP', 'UC1mUmU3MzlrZXphWW5GdGdM'
]

for cid in ids:
    feed = f'https://www.youtube.com/feeds/videos.xml?channel_id={cid}'
    try:
        req = urllib.request.Request(feed, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=5) as resp:
            xml_data = resp.read()
            root = ET.fromstring(xml_data)
            title = root.find('{http://www.w3.org/2005/Atom}title')
            channel_name = title.text if title is not None else ''
            print(f'MATCH: {cid} -> {channel_name}')
            entries = root.findall('{http://www.w3.org/2005/Atom}entry')
            print(f'  Found {len(entries)} recent videos!')
            for e in entries[:3]:
                etitle = e.find('{http://www.w3.org/2005/Atom}title').text
                epub = e.find('{http://www.w3.org/2005/Atom}published').text
                print(f'    - [{epub}] {etitle}')
    except Exception as e:
        pass
