# HouseMind Recognition Backend

This folder contains the small server that keeps your OpenAI key out of the Android app.

1. Create a Vercel account at https://vercel.com and install its GitHub integration.
2. Put this project in a private GitHub repository, then import it in Vercel. Set the Vercel project root directory to `backend`.
3. In Vercel, open **Settings**, then **Environment Variables**. Add `OPENAI_API_KEY` and paste your OpenAI key there. Do not paste that key into Android Studio or any Android project file.
4. Deploy the Vercel project. Copy its HTTPS address, such as `https://housemind-api.vercel.app`.
5. Open `app/src/main/java/com/housemind/app/recognition/HouseMindConfig.kt` in Android Studio. Paste the HTTPS address as `API_BASE_URL`, then rebuild the Android app.

The Android app sends one compressed JPEG to `POST /api/analyze`. The backend passes it to OpenAI, returns structured recognition fields, and does not save the photo.