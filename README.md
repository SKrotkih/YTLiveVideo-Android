# YTLiveVideo-Android

1. Upgrade  [YouTube WatchMe for Android](https://github.com/youtube/yt-watchme) to the Kotlin 1.4.10.

Note. [YouTube WatchMe for Android](https://github.com/youtube/yt-watchme) has integrated with [Google Java API Client Services](https://github.com/googleapis/google-api-java-client-services) 

To use this application,

1. In your [Google Developers Console](https://console.developers.google.com),
 1. Enable the YouTube Data API v3 and Google+ API.
 1. Create a client ID for Android, using your SHA1 and package name.
1. [Enable YouTube Live Streaming for your channel](https://support.google.com/youtube/answer/2474026?hl=en).
1. Update the [JNI code](https://github.com/youtube/yt-watchme/blob/master/app/src/main/jni/ffmpeg-jni.c) with respect to [Live Streaming Guide](https://support.google.com/youtube/answer/2853702?hl=en).
1. Include cross-platform compiled streaming libraries.
 1. Either [libffmpeg.so](https://trac.ffmpeg.org/wiki/CompilationGuide/Android) under src/main/jniLibs/armeabi,
 1. or another streaming library with modifying VideoStreamingInterface
 