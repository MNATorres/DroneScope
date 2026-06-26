# El SDK de DJI no debe ofuscarse
-keep class dji.** { *; }
-keep class com.dji.** { *; }
-keep class com.cySdkyc.** { *; }
-keep class sdk.** { *; }
-dontwarn dji.**
-dontwarn com.dji.**
