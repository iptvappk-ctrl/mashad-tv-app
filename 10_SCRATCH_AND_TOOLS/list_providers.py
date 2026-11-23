with open(r'01_ZAQZOUKA_CLIENT\MadarFlutter\android\app\src\main\kotlin\com\madar\tv\data\MultiProviderRepository.kt', encoding='utf-8') as f:
    text = f.read()
import re
providers = re.findall(r'ProviderConfig\s*\(\s*id\s*=\s*"([^"]+)",\s*name\s*=\s*"([^"]+)",\s*baseUrl\s*=\s*"([^"]+)"', text)
print(f'Total providers: {len(providers)}')
for pid, name, url in providers:
    enabled = f'id = "{pid}"' in text and 'enabled = false' not in text[text.find(f'id = "{pid}"'):text.find(f'id = "{pid}"')+300]
    print(f'[{pid}] {name} -> {url} (enabled={enabled})')
