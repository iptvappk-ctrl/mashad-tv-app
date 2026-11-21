import requests

url = "http://deft.yacinelive.com/api/categories"
r = requests.get(url, headers={"User-Agent": "okhttp/4.11.0", "Accept": "application/json", "x-android-package": "ver3.ycntivi.off"}, timeout=10)
print("HTTP Status:", r.status_code, "Headers:", r.headers, "Body:", r.text[:200])
