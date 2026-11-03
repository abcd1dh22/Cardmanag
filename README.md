# Card Wallet

## GitHub Actions দিয়ে APK বানানো

- `main`-এ push / PR → **debug APK** তৈরি হয় (Actions → run → Artifacts থেকে ডাউনলোড)
- `v1.0.0` ধরনের ট্যাগ push → **signed release APK** তৈরি হয়ে GitHub Releases-এ আপলোড হয়

### একবারের সেটআপ (release signing)
```bash
keytool -genkeypair -v -keystore release.jks -alias cardwallet \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w 0 release.jks      # macOS: base64 -i release.jks
```
Repo → Settings → Secrets and variables → Actions-এ যোগ করুন:
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

> `release.jks` ফাইল কখনো repo-তে commit করবেন না, আর একটা ব্যাকআপ নিজের কাছে রাখুন।

### রিলিজ দেওয়া
```bash
git tag v1.0.0
git push origin v1.0.0
```
