import requests

for url in ["http://ver3.yacinelive.com/api/categories", "https://ver3.yacinelive.com/api/categories", "http://ver3.yacinelive.com/categories"]:
    try:
        r = requests.get(url, headers={"User-Agent": "okhttp/4.11.0", "Accept": "application/json", "x-android-package": "ver3.ycntivi.off"}, timeout=5)
        print(url, "Status:", r.status_code, "Headers:", r.headers, "Text:", r.text[:200])
    except Exception as e:
        print(url, "Error:", e)
