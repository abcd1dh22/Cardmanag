# Card Wallet

Kotlin + Jetpack Compose card manager. Cards are stored AES-256 encrypted on the device.

Features: stacked animated cards, live card-brand detection (Visa, Mastercard, Amex, Discover,
UnionPay, JCB, Diners) with Luhn check, fingerprint / screen-lock app lock, settings toggles,
screenshot blocking, auto-hide, clipboard auto-clear.

## GitHub Actions
- push / PR to `main` -> debug APK (Actions -> run -> Artifacts)
- tag `v1.0.0` -> signed release APK on GitHub Releases

### Release signing (one time)
```bash
keytool -genkeypair -v -keystore release.jks -alias cardwallet -keyalg RSA -keysize 2048 -validity 10000
base64 -w 0 release.jks
```
Add repo secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
Never commit `release.jks`.
