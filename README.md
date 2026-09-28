# YTLiveStreaming for Android

An example of using the [YouTube Live Streaming API v3](https://developers.google.com/youtube/v3/live/docs) on Android (Kotlin).

## Technologies & Libraries Used

- [Google Java API Client Services](https://github.com/googleapis/google-api-java-client-services)
- [Google Sign-In](https://developers.google.com/identity/sign-in/android/sign-in)
- MVVM Architecture
- Kotlin Coroutines (asynchronous operations)
- [YouTube Android Player API](https://developers.google.com/youtube/android/player)

## Getting Started

To use this application:

1. [Enable YouTube Live Streaming for your channel](https://support.google.com/youtube/answer/2474026?hl=en).
2. In your [Google Developers Console](https://console.developers.google.com):
   1. Enable the **YouTube Data API v3** (from the Library).

      ![Screen Shot 2020-10-06 at 8 48 27 AM](https://user-images.githubusercontent.com/2775621/95163961-b6a7fb00-07b1-11eb-9b06-42fef871cb2f.png)

   2. Create a Client ID for Android using your SHA-1 fingerprint and package name.

      ![Screen Shot 2020-10-06 at 8 45 37 AM](https://user-images.githubusercontent.com/2775621/95163944-abed6600-07b1-11eb-8e4e-c9cd1693e4a6.png)
      ![Screen Shot 2020-10-06 at 8 52 25 AM](https://user-images.githubusercontent.com/2775621/95163976-bc9ddc00-07b1-11eb-96ee-5540d0ab3d34.png)

   3. Obtain an API Key for the YouTube Player API:
      1. On the Credentials page in the console, copy your API key.
      2. Open `Credentilals_.kt` in the `util` folder. Replace `"API Key"` with your actual API key from the previous step.
      3. Rename `Credentilals_.kt` to `Credentials.kt`.

## Demo Video

1. Create a new stream on your YouTube account:

   ![v2](https://user-images.githubusercontent.com/2775621/95176102-0f34c380-07c5-11eb-99bf-84e38c6fe781.gif)

2. View broadcasts across different lifecycle states (All, Upcoming, Active, Completed) and watch active streams:

   ![v5](https://user-images.githubusercontent.com/2775621/95176316-62a71180-07c5-11eb-8565-71baae59234f.gif)

## iOS Implementation

An iOS implementation using the same API is available at [YTLiveStreaming for iOS](https://github.com/SKrotkih/YTLiveStreaming).
