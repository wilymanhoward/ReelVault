# Library consumer rules (Room, Hilt, kotlinx-serialization, Navigation) cover most of the app.
# Add app-specific keep rules here only when R8 breaks something.

# Credential Manager finds its Play Services provider by reflection; R8 would strip it, breaking
# Google sign-in in release builds (rule from the Credential Manager integration guide).
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}
