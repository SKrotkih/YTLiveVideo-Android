# YTLiveStreaming for Android

Example of using [YouTube Live Streaming API v3](https://developers.google.com/youtube/v3/live/docs) on Android (Kotlin).

Were also used:
- [Google Java API Client Services](https://github.com/googleapis/google-api-java-client-services)
- [Google Sign-In](https://developers.google.com/identity/sign-in/android/sign-in). 
- MVVM pattern
- Coroutines (Multithreading async operations)
- [YouTube Android Player API](https://developers.google.com/youtube/android/player)

To use this application,

1. [Enable YouTube Live Streaming for your channel](https://support.google.com/youtube/answer/2474026?hl=en).
1. In your [Google Developers Console](https://console.developers.google.com),
 1. Enable the YouTube Data API v3 (from the Library)
 
![Screen Shot 2020-10-06 at 8 48 27 AM](https://user-images.githubusercontent.com/2775621/95163961-b6a7fb00-07b1-11eb-9b06-42fef871cb2f.png) 

 1. Create a client ID for Android, using your SHA1 and package name.
 
![Screen Shot 2020-10-06 at 8 45 37 AM](https://user-images.githubusercontent.com/2775621/95163944-abed6600-07b1-11eb-8e4e-c9cd1693e4a6.png)
![Screen Shot 2020-10-06 at 8 52 25 AM](https://user-images.githubusercontent.com/2775621/95163976-bc9ddc00-07b1-11eb-96ee-5540d0ab3d34.png)

 For the YouTube Player API needed API key:   
   1. On the Credentials page (look for on the console) copy API Key    
   2. Open Credentials_.kt file (util folder). Replace the parameter "API Key" with Past API from the previous step. Close the file.
   3. Rename file Credentials_.kt to Credentials.kt 

Demo  video
1. Create a new stream on your YouTube account:

![v2](https://user-images.githubusercontent.com/2775621/95176102-0f34c380-07c5-11eb-99bf-84e38c6fe781.gif)

2. There are All, Upcoming, Active, Completed life cycle broadcasts. Watch current (active) stream.

![v5](https://user-images.githubusercontent.com/2775621/95176316-62a71180-07c5-11eb-8565-71baae59234f.gif)

There is an example of using the same API on iOS: [YTLiveStreaming for iOS](https://github.com/SKrotkih/YTLiveStreaming). 
