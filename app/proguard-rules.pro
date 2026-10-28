# Readable crash stack traces in Play Console (upload the mapping file with each release).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# The ads SDK pulls in WorkManager 2.7 with Room 2.2, which finds its generated database by
# name and creates it reflectively. Without these, R8 strips it and the release crashes at launch
# ("Failed to create an instance of androidx.work.impl.WorkDatabase").
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
