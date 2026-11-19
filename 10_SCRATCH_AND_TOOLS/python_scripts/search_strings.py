import os

target1 = "تعذر جلب السيرفرات"
target2 = "لا توجد سيرفرات"

for root, dirs, files in os.walk('MadarTV'):
    for f in files:
        if f.endswith(('.kt', '.xml')):
            p = os.path.join(root, f)
            try:
                with open(p, 'r', encoding='utf-8', errors='ignore') as fp:
                    content = fp.read()
                    if target1 in content:
                        print("Found target1 in", p)
                    if target2 in content:
                        print("Found target2 in", p)
            except Exception as e:
                pass
