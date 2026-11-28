# Firebase, Google sign-in, analytics, and crash reporting

This repository is safe to build without Firebase. The Firebase SDKs are compiled into the
app, but every runtime call is disabled unless a real `app/google-services.json` is present.
That file is ignored by Git. Never put OAuth secrets, service-account JSON, or a keystore in
this repository.

## What is implemented

- **Google sign-in** uses Credential Manager plus Firebase Authentication. It creates only a
  Firebase Auth identity; routes, stops, photos, and tasks remain on the device. Cloud sync is
  deliberately not implemented yet.
- **Account deletion** is available in Account → Delete Google account. This deletes the
  Firebase Auth account for this app, signs out, and keeps device-local data intact.
- **Analytics and Crashlytics are opt-in.** Both start disabled in the manifest and Settings →
  Privacy and diagnostics. Advertising ID collection and ad-personalization signals are disabled.
  The telemetry seam rejects route names, addresses, coordinates, notes, e-mail, and account IDs.
- **Crashlytics mapping upload** is enabled only when `app/google-services.json` is present.

## Your one-time Firebase Console work

1. Create a Firebase project. Decide the project owner and billing owner first; use an
   organization-owned Google account for a business product.
2. Add Android app package **`com.danielcioban.routeplanner`**. Download its config and save it
   as `app/google-services.json`. It must never be committed.
3. In Firebase Authentication → Sign-in method, enable **Google**. In Project settings → Your
   apps, add SHA-1 fingerprints for both the debug keystore and the Play upload/app-signing key.
   Then download a *fresh* `google-services.json`; it contains the required web OAuth client ID.
4. Decide deliberately whether to enable Google Analytics when creating the project. If enabled,
   keep the app controls opt-in and complete the Play Data safety form and privacy policy with the
   actual data choices. Do not turn on ads integrations or personalized advertising for this app
   without legal review.
5. Build and test Google sign-in on a physical device. Test Account → Delete Google account too.
   Firebase can require recent sign-in before deletion; if that happens, sign out, sign in again,
   then retry.
6. Before a release, enable Crashlytics only if you will monitor it and update the privacy policy.

## Local checks

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:bundleRelease
```

With no config file, the Account screen remains usable but tells you that Google sign-in needs
configuration. With a valid config file, select Continue with Google. Never use a real customer
route or address while testing analytics.

## Not implemented yet

- Cloud Firestore / server route sync, team roles, sharing, and subscriptions.
- Server-side deletion of future synced route data. Add a published deletion policy and endpoint
  before storing any customer data remotely.
- Push notifications, Remote Config, Ads, or any advertising identifier use.

Read [ADR 0002](adr/0002-local-first-sync.md) before adding remote sync.
