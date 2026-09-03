# AI Reader

Android starter project for the AI Reader app.

Current starter features:
- PDF picker
- Library UI
- Persistent read permission for selected documents
- Basic PDF page counting
- Reader shell
- Notes and highlight UI
- Text-to-speech hook
- AI tools menu placeholder

Next implementation stages:
1. Replace the reader shell with real PdfRenderer page rendering and gestures.
2. Persist the library, last page, notes, highlights and bookmarks using Room/DataStore.
3. Extract text from PDFs, including OCR for scanned PDFs.
4. Add Gemini through a secure backend/proxy (never ship a private API key in the APK).
5. Add selection-aware translation, explanation and summarization.
6. Add cloud backup/storage with quotas and user authentication.
7. Add download/offline management and compression.
8. Add male/female voice selection and multilingual TTS.
9. Polish the UI after functionality is stable.

Build requirements:
- Android Studio with a recent Android SDK.
- JDK 17.
