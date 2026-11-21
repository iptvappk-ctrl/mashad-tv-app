import re

hud_path = r'C:\Users\hassa\.gemini\antigravity\brain\a8e22ead-f41c-4390-aaed-352345d72ecc\browser_sandbox\outputs\step_28_hud.txt'

with open(hud_path, 'r', encoding='utf-8', errors='ignore') as f:
    text = f.read()

# Look for articles or comments
print('--- COMMENTS IN HUD ---')
comments = re.findall(r'article\s+"Comment by.*?"(.*?)(?=article\s+"Comment by|\Z)', text, re.DOTALL)
print(f'Total comments found: {len(comments)}')

for idx, c in enumerate(comments):
    print(f'\n--- COMMENT {idx + 1} ---')
    lines = [l.strip() for l in c.split('\n') if l.strip() and not l.strip().startswith('- button') and not l.strip().startswith('/url:')]
    print('\n'.join(lines[:10]))
