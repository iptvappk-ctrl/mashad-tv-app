from bs4 import BeautifulSoup
import re

out = open('analysis_out.txt', 'w', encoding='utf-8')

for filename, label in [('episode_main.html', 'MAIN'), ('episode_watch.html', 'WATCH')]:
    out.write(f"\n==================== {label} PAGE ====================\n")
    with open(filename, 'rb') as f:
        data = f.read()
    if data.startswith(b'\xff\xfe'):
        text = data[2:].decode('utf-16le', errors='ignore')
    elif b'\x00' in data[:100]:
        text = data.decode('utf-16le', errors='ignore')
    else:
        text = data.decode('utf-8', errors='ignore')

    doc = BeautifulSoup(text, 'html.parser')
    out.write(f"Parsed text length: {len(text)}\n")
    
    # Story
    for sel in ['.post__story', '.post__content', '.story', '.entry-content', '.overview', 'p.story']:
        elems = doc.select(sel)
        for el in elems[:2]:
            t = el.get_text(strip=True)
            if len(t) > 20:
                out.write(f"Story [{sel}]: {t}\n")

    # Iframes
    for ifr in doc.select('iframe'):
        out.write(f"Iframe: {ifr.attrs}\n")

    # Server buttons or data-link/data-url
    for el in doc.select('[data-link], [data-url], [data-src], [data-embed]'):
        d_attrs = {k: v for k, v in el.attrs.items() if k.startswith('data-')}
        out.write(f"Data [{el.name}]: {d_attrs}\n")

    # Watch links
    for a in doc.select('a'):
        href = a.get('href', '')
        if any(x in href for x in ['watch', 'selary', 'series', 'season', 'govid', 'play', 'download']):
            out.write(f"A link: {href} | Text: {a.get_text(strip=True)}\n")

    # All buttons / server list
    for btn in doc.select('.servers__list li, .servers li, .watch-servers li, ul.servers-list li, .servers-tabs li, .server-item, .watch-btn, a.watch, a.download'):
        out.write(f"Server item: {btn.attrs} | Text: {btn.get_text(strip=True)}\n")

out.close()
print("Done analysis_out.txt")
