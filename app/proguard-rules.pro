# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Kotlin Metadata and annotations needed by Room and Compose
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*,RuntimeVisibleAnnotations,Signature,InnerClasses,EnclosingMethod

# Keep Room Database, Entities, DAOs, and Migrations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.example.data.db.** { *; }
-keep class com.example.data.model.WorkoutSession { *; }
-keep class com.example.data.model.WeightLog { *; }
-keep class com.example.data.model.BodyMeasurement { *; }
-dontwarn androidx.room.paging.**

# Keep org.json usage (Android platform JSON classes)
-keep class org.json.** { *; }
-dontwarn org.json.**

# Keep Application, Activity, and BroadcastReceiver classes declared in AndroidManifest.xml
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.content.BroadcastReceiver

-keep class com.example.MainActivity { *; }
-keep class com.example.reminder.WorkoutAlarmReceiver { *; }
-keep class com.example.reminder.RestTimerAlarmReceiver { *; }
-keep class com.example.reminder.WeighInAlarmReceiver { *; }
-keep class com.example.reminder.BootReceiver { *; }

