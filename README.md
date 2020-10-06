# YTLiveStreaming for Android

Example of using [YouTube Live Streaming API v3](https://developers.google.com/youtube/v3/live/docs) on Android (Kotlin).

Here you can see upgraded in [Kotlin 1.4](https://github.com/JetBrains/kotlin/releases/tag/v1.4.10) deprecated version of the [YouTube WatchMe for Android](https://github.com/youtube/yt-watchme) project.

Was used
- [Google Java API Client Services](https://github.com/googleapis/google-api-java-client-services)
- [Google Sign-In](https://developers.google.com/identity/sign-in/android/sign-in). 
- MVVM
- Coroutines (Multithreading async operations)

To use this application,

1. [Enable YouTube Live Streaming for your channel](https://support.google.com/youtube/answer/2474026?hl=en).
1. In your [Google Developers Console](https://console.developers.google.com),
 1. Enable the YouTube Data API v3 (from Library)
 
![Screen Shot 2020-10-06 at 8 48 27 AM](https://user-images.githubusercontent.com/2775621/95163961-b6a7fb00-07b1-11eb-9b06-42fef871cb2f.png) 

 1. Create a client ID for Android, using your SHA1 and package name.
 
![Screen Shot 2020-10-06 at 8 45 37 AM](https://user-images.githubusercontent.com/2775621/95163944-abed6600-07b1-11eb-8e4e-c9cd1693e4a6.png)
![Screen Shot 2020-10-06 at 8 52 25 AM](https://user-images.githubusercontent.com/2775621/95163976-bc9ddc00-07b1-11eb-96ee-5540d0ab3d34.png)

Demo  video

![g1](https://user-images.githubusercontent.com/2775621/94280668-56fe5400-ff56-11ea-9928-31511f6f508d.gif)

There is an example of using the same API on iOS: [YTLiveStreaming for iOS](https://github.com/SKrotkih/YTLiveStreaming). 
