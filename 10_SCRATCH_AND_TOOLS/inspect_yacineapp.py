import requests

url = "https://yacineapp.tv/api/categories"
r = requests.get(url, headers={"User-Agent": "okhttp/4.11.0", "Accept": "application/json", "x-android-package": "ver3.ycntivi.off"}, timeout=10)
print("Status:", r.status_code)
print("Headers:", r.headers)
print("Text preview:", r.text[:500])
