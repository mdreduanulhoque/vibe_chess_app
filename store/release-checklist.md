# Vibe Chess — Google Play Release & Signing Checklist

This checklist covers generating your personal release upload keystore, building the signed release Android App Bundle (`.aab`) and debug APK (`.apk`), and completing all Google Play Console requirements.

---

## 1. Generate Your Own Upload Keystore & Configure Signing

> **Security Rule:** Never commit `keystore.properties` or any `.jks` / `.keystore` file to Git. Both are already listed in `.gitignore`.

### Step 1.1: Generate an Upload Keystore (`keytool`)
Run the following command from the project root (`d:\AAAAAAAAAAAAAAA\Vibe Chess App`) in PowerShell or terminal, replacing the passwords and Distinguished Name (`-dname`) with your own details:

```powershell
New-Item -ItemType Directory -Force -Path keystore
keytool -genkeypair -v `
  -keystore keystore/vibe-chess-upload.jks `
  -storetype JKS `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -alias vibe-chess-upload
```

*(When prompted, enter a strong keystore password, key password, and your name/organization.)*

### Step 1.2: Create `keystore.properties` (Git-Ignored)
Copy `keystore.properties.example` to `keystore.properties` in the project root and fill in your credentials:

```properties
storeFile=keystore/vibe-chess-upload.jks
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=vibe-chess-upload
keyPassword=YOUR_KEY_PASSWORD
```

### Step 1.3: Build Artifacts (Debug APK & Signed Release AAB)
Run from the project root:

```powershell
.\gradlew.bat assembleDebug bundleRelease
```

- **Debug APK (for sideloading/testing):**  
  `app\build\outputs\apk\debug\app-debug.apk`
- **Signed Release AAB (for Google Play upload):**  
  `app\build\outputs\bundle\release\app-release.aab`
- **R8 Mapping File (upload to Play Console for deobfuscated crash stack traces):**  
  `app\build\outputs\mapping\release\mapping.txt`

---

## 2. Manifest & Technical Compliance Verification

- [x] **Application ID:** `com.reduan.vibechess`
- [x] **Versioning:** `versionCode 1`, `versionName "1.0.0"`
- [x] **Target API Requirement:** `compileSdk 36`, `targetSdk 36` (exceeds Google Play's minimum target API level requirement for new apps), `minSdk 26` (Android 8.0+).
- [x] **Permissions Check:** Only `android.permission.VIBRATE` is declared in `AndroidManifest.xml`. **No `android.permission.INTERNET`** or any dangerous permissions are present.
- [x] **Code & Resource Shrinking:** `minifyEnabled true` and `shrinkResources true` enabled in `app/build.gradle` for the `release` build type.
- [x] **Adaptive Icon:** Foreground (`@drawable/ic_launcher_foreground`), Background (`@drawable/ic_launcher_background`), and Android 13+ Monochrome (`@drawable/ic_launcher_monochrome`) layers configured in `ic_launcher.xml` and `ic_launcher_round.xml`.
- [x] **Localization:** App name (`Vibe Chess` / `ভাইব চেস`) and all UI strings provided in `values/strings.xml` (English) and `values-bn/strings.xml` (Bangla), with `resConfigs "en", "bn"` and `@xml/locales_config`.

---

## 3. Google Play Console — App Content & Policy Forms

### 3.1 Privacy Policy
- [ ] Host the contents of `store/privacy-policy.md` on a publicly accessible HTTPS URL (e.g., GitHub Pages or your developer website) and enter the URL under **Policy > App content > Privacy policy**.

### 3.2 Data Safety Form Answers
Under **Policy > App content > Data safety**, answer as follows:
1. **Does your app collect or share any of the required user data types?**  
   -> **No** (Vibe Chess is 100% offline, has no `INTERNET` permission, and collects or transmits zero user data).
2. Review and submit. Google Play will display the **"No data collected"** and **"No data shared with third parties"** badges on the store listing.

### 3.3 Content Rating Questionnaire (IARC)
Under **Policy > App content > Content rating**:
1. **Category:** Select **Game**.
2. **Violence / Blood / Gore:** **No** (abstract minimalist chess pieces only).
3. **Fear / Horror:** **No**.
4. **Sexuality / Nudity:** **No**.
5. **Profanity / Crude Humor:** **No**.
6. **Controlled Substances:** **No**.
7. **Gambling / Simulated Gambling / Cash Prizes:** **No**.
8. **User-Generated Content / Online Chat / Location Sharing:** **No** (100% offline).
9. **In-App Purchases / Loot Boxes:** **No**.
- *Expected Rating:* **Everyone (ESRB) / PEGI 3 / USK 0 / IARC 3+**.

### 3.4 Ads & Target Audience
- **Ads:** Select **"No, my app does not contain ads"**.
- **Target Audience:** Select appropriate age groups (e.g., **13–15, 16–17, 18 and over**; if selecting under 13, complete the Families policy declaration—the app is fully compliant as it has no ads, no analytics, and no internet access).

### 3.5 Government & Financial Features Declarations
- **Government Apps:** **No**.
- **Financial Features:** **No**.

---

## 4. Play App Signing & Testing Tracks

### 4.1 Play App Signing (by Google Play)
- [ ] Under **Release > Setup > App integrity > Play App Signing**, enroll in **Play App Signing** (mandatory for new apps using `.aab`).
- [ ] Google Play will manage the production **App Signing Key** in Google's secure infrastructure, while your local `vibe-chess-upload.jks` key serves as your **Upload Key**.
- [ ] Back up your `vibe-chess-upload.jks` file and `keystore.properties` in a secure password manager / encrypted backup outside the repository.

### 4.2 Internal & Closed Testing Tracks
- [ ] **Internal Testing Track:**
  1. Go to **Release > Testing > Internal testing** and create a new release.
  2. Upload `app/build/outputs/bundle/release/app-release.aab` and `app/build/outputs/mapping/release/mapping.txt`.
  3. Add internal testers (up to 100 emails) and verify installation from the Play Store link on physical devices (light/dark themes, English/Bangla, phone/tablet).
- [ ] **Closed Testing Track (Required for Personal Developer Accounts):**
  1. Go to **Release > Testing > Closed testing** and promote the verified internal release (or upload `app-release.aab`).
  2. Add at least **12 testers** opted-in continuously for **14 days** (per Google Play Console policy for personal developer accounts created after Nov 2023).
- [ ] **Production Release:**
  1. Once testing requirements are complete, promote the release from Closed Testing to **Production**.
  2. Attach the English (`en-US`) and Bangla (`bn-BD`) store listings from `store/listing-en.md` and `store/listing-bn.md`, plus screenshots from `store/screenshot-shot-list.md`.
