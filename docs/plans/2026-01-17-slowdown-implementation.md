# SlowDown MVP Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a digital mindfulness Android app that intercepts distracting app launches and displays intervention screens with countdown timers.

**Architecture:** AccessibilityService monitors app launches, triggers OverlayActivity for intervention. Room stores intervention records, DataStore stores user preferences. Jetpack Compose for all UI.

**Tech Stack:** Kotlin, Jetpack Compose, Room, DataStore, AccessibilityService, Material3

---

## Task 1: Configure Gradle Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `build.gradle.kts`

**Step 1: Update version catalog**

Edit `gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.13.2"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.27"
coreKtx = "1.15.0"
junit = "4.13.2"
junitVersion = "1.2.1"
espressoCore = "3.6.1"
lifecycleRuntimeKtx = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.12.01"
room = "2.6.1"
datastore = "1.1.1"
navigationCompose = "2.8.5"
coroutines = "1.9.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

**Step 2: Update root build.gradle.kts**

Edit `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

**Step 3: Update app/build.gradle.kts**

Replace entire content of `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.slowdown"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.slowdown"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
```

**Step 4: Sync Gradle and verify build**

Run: `cd D:\devlopment\SlowDown && .\gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add -A
git commit -m "chore: configure Compose, Room, DataStore dependencies"
```

---

## Task 2: Create Data Layer - Entities

**Files:**
- Create: `app/src/main/java/com/example/slowdown/data/local/entity/InterventionRecord.kt`
- Create: `app/src/main/java/com/example/slowdown/data/local/entity/MonitoredApp.kt`

**Step 1: Create InterventionRecord entity**

Create `app/src/main/java/com/example/slowdown/data/local/entity/InterventionRecord.kt`:

```kotlin
package com.example.slowdown.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intervention_records")
data class InterventionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val interventionType: String,
    val userChoice: String,
    val countdownDuration: Int,
    val actualWaitTime: Int
)
```

**Step 2: Create MonitoredApp entity**

Create `app/src/main/java/com/example/slowdown/data/local/entity/MonitoredApp.kt`:

```kotlin
package com.example.slowdown.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val interventionType: String = "countdown",
    val countdownSeconds: Int = 10,
    val redirectPackage: String? = null,
    val isEnabled: Boolean = true
)
```

**Step 3: Verify compilation**

Run: `cd D:\devlopment\SlowDown && .\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add -A
git commit -m "feat(data): add Room entities for intervention records and monitored apps"
```

---

## Task 3: Create Data Layer - DAOs

**Files:**
- Create: `app/src/main/java/com/example/slowdown/data/local/dao/InterventionDao.kt`
- Create: `app/src/main/java/com/example/slowdown/data/local/dao/MonitoredAppDao.kt`

**Step 1: Create InterventionDao**

Create `app/src/main/java/com/example/slowdown/data/local/dao/InterventionDao.kt`:

```kotlin
package com.example.slowdown.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.slowdown.data.local.entity.InterventionRecord
import kotlinx.coroutines.flow.Flow

data class DailyStat(
    val day: String,
    val count: Int
)

data class AppStat(
    val appName: String,
    val count: Int
)

@Dao
interface InterventionDao {

    @Insert
    suspend fun insert(record: InterventionRecord)

    @Query("SELECT COUNT(*) FROM intervention_records WHERE timestamp >= :startTime")
    fun getCountSince(startTime: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) * 5 FROM intervention_records
        WHERE timestamp >= :startTime
        AND userChoice != 'continued'
    """)
    fun getSavedMinutesSince(startTime: Long): Flow<Int>

    @Query("""
        SELECT date(timestamp/1000, 'unixepoch', 'localtime') as day, COUNT(*) as count
        FROM intervention_records
        WHERE timestamp >= :startTime
        GROUP BY day
        ORDER BY day
    """)
    fun getDailyStats(startTime: Long): Flow<List<DailyStat>>

    @Query("""
        SELECT appName, COUNT(*) as count
        FROM intervention_records
        WHERE timestamp >= :startTime
        GROUP BY packageName
        ORDER BY count DESC
        LIMIT 5
    """)
    fun getTopApps(startTime: Long): Flow<List<AppStat>>

    @Query("SELECT * FROM intervention_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<InterventionRecord>>
}
```

**Step 2: Create MonitoredAppDao**

Create `app/src/main/java/com/example/slowdown/data/local/dao/MonitoredAppDao.kt`:

```kotlin
package com.example.slowdown.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.slowdown.data.local.entity.MonitoredApp
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {

    @Query("SELECT * FROM monitored_apps")
    fun getAll(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps WHERE isEnabled = 1")
    fun getEnabled(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): MonitoredApp?

    @Query("SELECT EXISTS(SELECT 1 FROM monitored_apps WHERE packageName = :packageName AND isEnabled = 1)")
    suspend fun isMonitored(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: MonitoredApp)

    @Update
    suspend fun update(app: MonitoredApp)

    @Delete
    suspend fun delete(app: MonitoredApp)

    @Query("DELETE FROM monitored_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
```

**Step 3: Verify compilation**

Run: `cd D:\devlopment\SlowDown && .\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add -A
git commit -m "feat(data): add Room DAOs for interventions and monitored apps"
```

---

## Task 4: Create Data Layer - Database

**Files:**
- Create: `app/src/main/java/com/example/slowdown/data/local/AppDatabase.kt`

**Step 1: Create AppDatabase**

Create `app/src/main/java/com/example/slowdown/data/local/AppDatabase.kt`:

```kotlin
package com.example.slowdown.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.slowdown.data.local.dao.InterventionDao
import com.example.slowdown.data.local.dao.MonitoredAppDao
import com.example.slowdown.data.local.entity.InterventionRecord
import com.example.slowdown.data.local.entity.MonitoredApp

@Database(
    entities = [InterventionRecord::class, MonitoredApp::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun interventionDao(): InterventionDao
    abstract fun monitoredAppDao(): MonitoredAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "slowdown_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

**Step 2: Verify compilation**

Run: `cd D:\devlopment\SlowDown && .\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add -A
git commit -m "feat(data): add Room database configuration"
```

---

## Task 5: Create Data Layer - DataStore Preferences

**Files:**
- Create: `app/src/main/java/com/example/slowdown/data/preferences/UserPreferences.kt`

**Step 1: Create UserPreferences**

Create `app/src/main/java/com/example/slowdown/data/preferences/UserPreferences.kt`:

```kotlin
package com.example.slowdown.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {

    companion object {
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val DEFAULT_COUNTDOWN = intPreferencesKey("default_countdown")
        val COOLDOWN_MINUTES = intPreferencesKey("cooldown_minutes")
    }

    val serviceEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SERVICE_ENABLED] ?: true }

    val defaultCountdown: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[DEFAULT_COUNTDOWN] ?: 10 }

    val cooldownMinutes: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[COOLDOWN_MINUTES] ?: 5 }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SERVICE_ENABLED] = enabled
        }
    }

    suspend fun setDefaultCountdown(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_COUNTDOWN] = seconds
        }
    }

    suspend fun setCooldownMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[COOLDOWN_MINUTES] = minutes
        }
    }
}
```

**Step 2: Verify compilation**

Run: `cd D:\devlopment\SlowDown && .\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add -A
git commit -m "feat(data): add DataStore preferences for user settings"
```

---

## Task 6: Create Data Layer - Repository

**Files:**
- Create: `app/src/main/java/com/example/slowdown/data/repository/SlowDownRepository.kt`

**Step 1: Create SlowDownRepository**

Create `app/src/main/java/com/example/slowdown/data/repository/SlowDownRepository.kt`:

```kotlin
package com.example.slowdown.data.repository

import com.example.slowdown.data.local.dao.AppStat
import com.example.slowdown.data.local.dao.DailyStat
import com.example.slowdown.data.local.dao.InterventionDao
import com.example.slowdown.data.local.dao.MonitoredAppDao
import com.example.slowdown.data.local.entity.InterventionRecord
import com.example.slowdown.data.local.entity.MonitoredApp
import com.example.slowdown.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class SlowDownRepository(
    private val interventionDao: InterventionDao,
    private val monitoredAppDao: MonitoredAppDao,
    private val userPreferences: UserPreferences
) {
    // Preferences
    val serviceEnabled: Flow<Boolean> = userPreferences.serviceEnabled
    val defaultCountdown: Flow<Int> = userPreferences.defaultCountdown
    val cooldownMinutes: Flow<Int> = userPreferences.cooldownMinutes

    suspend fun setServiceEnabled(enabled: Boolean) = userPreferences.setServiceEnabled(enabled)
    suspend fun setDefaultCountdown(seconds: Int) = userPreferences.setDefaultCountdown(seconds)
    suspend fun setCooldownMinutes(minutes: Int) = userPreferences.setCooldownMinutes(minutes)

    // Monitored Apps
    val monitoredApps: Flow<List<MonitoredApp>> = monitoredAppDao.getAll()
    val enabledApps: Flow<List<MonitoredApp>> = monitoredAppDao.getEnabled()

    suspend fun isMonitored(packageName: String): Boolean = monitoredAppDao.isMonitored(packageName)
    suspend fun getMonitoredApp(packageName: String): MonitoredApp? = monitoredAppDao.getByPackage(packageName)
    suspend fun addMonitoredApp(app: MonitoredApp) = monitoredAppDao.insert(app)
    suspend fun updateMonitoredApp(app: MonitoredApp) = monitoredAppDao.update(app)
    suspend fun removeMonitoredApp(packageName: String) = monitoredAppDao.deleteByPackage(packageName)

    // Interventions
    suspend fun recordIntervention(record: InterventionRecord) = interventionDao.insert(record)

    fun getTodayCount(): Flow<Int> = interventionDao.getCountSince(getTodayStart())
    fun getTodaySavedMinutes(): Flow<Int> = interventionDao.getSavedMinutesSince(getTodayStart())
    fun getWeeklyStats(): Flow<List<DailyStat>> = interventionDao.getDailyStats(getWeekStart())
    fun getTopApps(): Flow<List<AppStat>> = interventionDao.getTopApps(getWeekStart())
    fun getRecentInterventions(limit: Int = 20): Flow<List<InterventionRecord>> = interventionDao.getRecent(limit)

    private fun getTodayStart(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getWeekStart(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
```

**Step 2: Verify compilation**

Run: `cd D:\devlopment\SlowDown && .\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add -A
git commit -m "feat(data): add repository to aggregate data access"
```

---

## Task 7: Create Application Class

**Files:**
- Create: `app/src/main/java/com/example/slowdown/SlowDownApp.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Step 1: Create SlowDownApp**

Create `app/src/main/java/com/example/slowdown/SlowDownApp.kt`:

```kotlin
package com.example.slowdown

import android.app.Application
import com.example.slowdown.data.local.AppDatabase
import com.example.slowdown.data.preferences.UserPreferences
import com.example.slowdown.data.repository.SlowDownRepository

class SlowDownApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }
    val repository: SlowDownRepository by lazy {
        SlowDownRepository(
            interventionDao = database.interventionDao(),
            monitoredAppDao = database.monitoredAppDao(),
            userPreferences = userPreferences
        )
    }
}
```

**Step 2: Register in AndroidManifest.xml**

Update `app/src/main/AndroidManifest.xml`, add `android:name=".SlowDownApp"` to application tag:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
        tools:ignore="QueryAllPackagesPermission" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application
        android:name=".SlowDownApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.SlowDown"
        tools:targetApi="31">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.SlowDown">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

**Step 3: Verify compilation**

Run: `cd D:\devlopment\SlowDown && .\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add -A
git commit -m "feat: add Application class with dependency initialization"
```

---

## Task 8: Create Utility Classes

**Files:**
- Create: `app/src/main/java/com/example/slowdown/util/PackageUtils.kt`
- Create: `app/src/main/java/com/example/slowdown/util/PermissionHelper.kt`

**Step 1: Create PackageUtils**

Create `app/src/main/java/com/example/slowdown/util/PackageUtils.kt`:

```kotlin
package com.example.slowdown.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)

object PackageUtils {

    private val SYSTEM_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.mms",
        "com.android.contacts",
        "com.android.dialer",
        "com.miui.home",
        "com.miui.securitycenter"
    )

    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .asSequence()
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName }
            .filter { !SYSTEM_PACKAGES.contains(it) }
            .mapNotNull { packageName ->
                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    AppInfo(
                        packageName = packageName,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(appInfo)
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }

    fun getAppName(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun isSystemCriticalApp(packageName: String): Boolean {
        return SYSTEM_PACKAGES.contains(packageName)
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun goHome(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
```

**Step 2: Create PermissionHelper**

Create `app/src/main/java/com/example/slowdown/util/PermissionHelper.kt`:

```kotlin
package com.example.slowdown.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

object PermissionHelper {

    fun isAccessibilityEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == context.packageName
        }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    // MIUI specific
    fun isMiui(): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
    }

    fun openMiuiAutoStartSettings(context: Context) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to app settings
            openAppSettings(context)
        }
    }

    fun openMiuiBackgroundPopupSettings(context: Context) {
        try {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                putExtra("extra_pkgname", context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppSettings(context)
        }
    }

    private fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
```

**Step 3: Verify compilation**

Run: `cd D:\devlopment\SlowDown && .\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add -A
git commit -m "feat(util): add PackageUtils and PermissionHelper"
```

---

## Task 9: Create Theme and Base UI

**Files:**
- Create: `app/src/main/java/com/example/slowdown/ui/theme/Color.kt`
- Create: `app/src/main/java/com/example/slowdown/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/example/slowdown/ui/theme/Type.kt`

**Step 1: Create Color.kt**

Create `app/src/main/java/com/example/slowdown/ui/theme/Color.kt`:

```kotlin
package com.example.slowdown.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val Green500 = Color(0xFF4CAF50)
val Green700 = Color(0xFF388E3C)
val Orange500 = Color(0xFFFF9800)
val Red500 = Color(0xFFF44336)
val Grey400 = Color(0xFFBDBDBD)
val Grey600 = Color(0xFF757575)
```

**Step 2: Create Type.kt**

Create `app/src/main/java/com/example/slowdown/ui/theme/Type.kt`:

```kotlin
package com.example.slowdown.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)
```

**Step 3: Create Theme.kt**

Create `app/src/main/java/com/example/slowdown/ui/theme/Theme.kt`:

```kotlin
package com.example.slowdown.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun SlowDownTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

**Step 4: Verify compilation**

Run: `cd D:\devlopment\SlowDown && .\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add -A
git commit -m "feat(ui): add Material3 theme configuration"
```

---

## Task 10: Create Dashboard ViewModel

**Files:**
- Create: `app/src/main/java/com/example/slowdown/viewmodel/DashboardViewModel.kt`

**Step 1: Create DashboardViewModel**

Create `app/src/main/java/com/example/slowdown/viewmodel/DashboardViewModel.kt`:

```kotlin
package com.example.slowdown.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.slowdown.data.local.dao.AppStat
import com.example.slowdown.data.local.dao.DailyStat
import com.example.slowdown.data.repository.SlowDownRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: SlowDownRepository
) : ViewModel() {

    val todayCount: StateFlow<Int> = repository.getTodayCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todaySavedMinutes: StateFlow<Int> = repository.getTodaySavedMinutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weeklyStats: StateFlow<List<DailyStat>> = repository.getWeeklyStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topApps: StateFlow<List<AppStat>> = repository.getTopApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serviceEnabled: StateFlow<Boolean> = repository.serviceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setServiceEnabled(enabled)
        }
    }

    class Factory(private val repository: SlowDownRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
```

**Step 2: Verify compilation**

Run: `cd D:\devlopment\SlowDown && .\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add -A
git commit -m "feat(viewmodel): add DashboardViewModel"
```

---

## Task 11: Create Dashboard Screen

**Files:**
- Create: `app/src/main/java/com/example/slowdown/ui/screen/DashboardScreen.kt`

**Step 1: Create DashboardScreen**

Create `app/src/main/java/com/example/slowdown/ui/screen/DashboardScreen.kt`:

```kotlin
package com.example.slowdown.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.slowdown.data.local.dao.AppStat
import com.example.slowdown.data.local.dao.DailyStat
import com.example.slowdown.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAppList: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val todayCount by viewModel.todayCount.collectAsState()
    val savedMinutes by viewModel.todaySavedMinutes.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val topApps by viewModel.topApps.collectAsState()
    val serviceEnabled by viewModel.serviceEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SlowDown") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Today's stats card
            item {
                TodayStatsCard(
                    count = todayCount,
                    savedMinutes = savedMinutes,
                    serviceEnabled = serviceEnabled,
                    onToggleService = { viewModel.setServiceEnabled(it) }
                )
            }

            // Weekly chart
            item {
                WeeklyChartCard(stats = weeklyStats)
            }

            // Top apps
            item {
                TopAppsCard(apps = topApps)
            }

            // Manage apps button
            item {
                Button(
                    onClick = onNavigateToAppList,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("管理监控应用")
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TodayStatsCard(
    count: Int,
    savedMinutes: Int,
    serviceEnabled: Boolean,
    onToggleService: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (serviceEnabled) "监控中" else "已暂停",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (serviceEnabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error
                )
                Switch(
                    checked = serviceEnabled,
                    onCheckedChange = onToggleService
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "今日已拦截",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$count 次",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "节省约 $savedMinutes 分钟",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyChartCard(stats: List<DailyStat>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "本周趋势",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (stats.isEmpty()) {
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                SimpleBarChart(
                    data = stats,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }
    }
}

@Composable
private fun SimpleBarChart(
    data: List<DailyStat>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.count } ?: 1
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 2)
        val maxHeight = size.height * 0.8f

        data.forEachIndexed { index, stat ->
            val barHeight = if (maxValue > 0) {
                (stat.count.toFloat() / maxValue) * maxHeight
            } else 0f

            val x = barWidth * (index * 2 + 0.5f)
            val y = size.height - barHeight

            drawRect(
                color = primaryColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

@Composable
private fun TopAppsCard(apps: List<AppStat>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "最常拦截",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (apps.isEmpty()) {
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                apps.forEach { app ->
                    AppStatRow(app = app, maxCount = apps.first().count)
                }
            }
        }
    }
}

@Composable
private fun AppStatRow(app: AppStat, maxCount: Int) {
    val progress = if (maxCount > 0) app.count.toFloat() / maxCount else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(80.dp)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .padding(horizontal = 8.dp),
        )
        Text(
            text = "${app.count}次",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(40.dp)
        )
    }
}
```

**Step 2: Verify compilation**

Run: `cd D:\devlopment\SlowDown && .\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add -A
git commit -m "feat(ui): add DashboardScreen with stats display"
```

---

## Task 12: Create AppList ViewModel and Screen

**Files:**
- Create: `app/src/main/java/com/example/slowdown/viewmodel/AppListViewModel.kt`
- Create: `app/src/main/java/com/example/slowdown/ui/screen/AppListScreen.kt`

**Step 1: Create AppListViewModel**

Create `app/src/main/java/com/example/slowdown/viewmodel/AppListViewModel.kt`:

```kotlin
package com.example.slowdown.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.slowdown.data.local.entity.MonitoredApp
import com.example.slowdown.data.repository.SlowDownRepository
import com.example.slowdown.util.AppInfo
import com.example.slowdown.util.PackageUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppListViewModel(
    private val repository: SlowDownRepository,
    private val context: Context
) : ViewModel() {

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    val monitoredApps: StateFlow<List<MonitoredApp>> = repository.monitoredApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _installedApps.value = PackageUtils.getInstalledApps(context)
            _isLoading.value = false
        }
    }

    fun toggleApp(appInfo: AppInfo, isMonitored: Boolean) {
        viewModelScope.launch {
            if (isMonitored) {
                repository.removeMonitoredApp(appInfo.packageName)
            } else {
                repository.addMonitoredApp(
                    MonitoredApp(
                        packageName = appInfo.packageName,
                        appName = appInfo.appName
                    )
                )
            }
        }
    }

    fun updateAppConfig(app: MonitoredApp) {
        viewModelScope.launch {
            repository.updateMonitoredApp(app)
        }
    }

    class Factory(
        private val repository: SlowDownRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppListViewModel(repository, context) as T
        }
    }
}
```

**Step 2: Create AppListScreen**

Create `app/src/main/java/com/example/slowdown/ui/screen/AppListScreen.kt`:

```kotlin
package com.example.slowdown.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.slowdown.util.AppInfo
import com.example.slowdown.viewmodel.AppListViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit
) {
    val installedApps by viewModel.installedApps.collectAsState()
    val monitoredApps by viewModel.monitoredApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val monitoredPackages = remember(monitoredApps) {
        monitoredApps.map { it.packageName }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择监控应用") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(installedApps, key = { it.packageName }) { app ->
                    AppListItem(
                        app = app,
                        isMonitored = monitoredPackages.contains(app.packageName),
                        onToggle = { viewModel.toggleApp(app, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppListItem(
    app: AppInfo,
    isMonitored: Boolean,
    onToggle: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(app.appName) },
        supportingContent = { Text(app.packageName, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            if (app.icon != null) {
                Image(
                    painter = rememberDrawablePainter(drawable = app.icon),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(app.appName.take(1))
                }
            }
        },
        trailingContent = {
            Checkbox(
                checked = isMonitored,
                onCheckedChange = { onToggle(isMonitored) }
            )
        }
    )
}
```

**Step 3: Add Accompanist dependency for drawable painter**

Update `gradle/libs.versions.toml` to add:

```toml
[versions]
# ... existing versions
accompanist = "0.34.0"

[libraries]
# ... existing libraries
accompanist-drawablepainter = { group = "com.google.accompanist", name = "accompanist-drawablepainter", version.ref = "accompanist" }
```

Update `app/build.gradle.kts` dependencies:

```kotlin
implementation(libs.accompanist.drawablepainter)
```

**Step 4: Verify compilation**

Run: `cd D:\devlopment\SlowDown && .\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add -A
git commit -m "feat(ui): add AppListScreen for selecting monitored apps"
```

---

## Task 13: Create Settings Screen

**Files:**
- Create: `app/src/main/java/com/example/slowdown/viewmodel/SettingsViewModel.kt`
- Create: `app/src/main/java/com/example/slowdown/ui/screen/SettingsScreen.kt`

**Step 1: Create SettingsViewModel**

Create `app/src/main/java/com/example/slowdown/viewmodel/SettingsViewModel.kt`:

```kotlin
package com.example.slowdown.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.slowdown.data.repository.SlowDownRepository
import com.example.slowdown.util.PermissionHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PermissionState(
    val accessibilityEnabled: Boolean = false,
    val overlayEnabled: Boolean = false,
    val batteryOptimizationDisabled: Boolean = false,
    val isMiui: Boolean = false
)

class SettingsViewModel(
    private val repository: SlowDownRepository,
    private val context: Context
) : ViewModel() {

    val defaultCountdown: StateFlow<Int> = repository.defaultCountdown
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val cooldownMinutes: StateFlow<Int> = repository.cooldownMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        _permissionState.value = PermissionState(
            accessibilityEnabled = PermissionHelper.isAccessibilityEnabled(context),
            overlayEnabled = PermissionHelper.canDrawOverlays(context),
            batteryOptimizationDisabled = PermissionHelper.isIgnoringBatteryOptimizations(context),
            isMiui = PermissionHelper.isMiui()
        )
    }

    fun setDefaultCountdown(seconds: Int) {
        viewModelScope.launch {
            repository.setDefaultCountdown(seconds)
        }
    }

    fun setCooldownMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setCooldownMinutes(minutes)
        }
    }

    fun openAccessibilitySettings() {
        PermissionHelper.openAccessibilitySettings(context)
    }

    fun openOverlaySettings() {
        PermissionHelper.openOverlaySettings(context)
    }

    fun openBatterySettings() {
        PermissionHelper.openBatteryOptimizationSettings(context)
    }

    fun openMiuiAutoStartSettings() {
        PermissionHelper.openMiuiAutoStartSettings(context)
    }

    fun openMiuiBackgroundPopupSettings() {
        PermissionHelper.openMiuiBackgroundPopupSettings(context)
    }

    class Factory(
        private val repository: SlowDownRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, context) as T
        }
    }
}
```

**Step 2: Create SettingsScreen**

Create `app/src/main/java/com/example/slowdown/ui/screen/SettingsScreen.kt`:

```kotlin
package com.example.slowdown.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.slowdown.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val defaultCountdown by viewModel.defaultCountdown.collectAsState()
    val cooldownMinutes by viewModel.cooldownMinutes.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Permissions section
            item {
                Text(
                    text = "权限设置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            item {
                PermissionItem(
                    title = "无障碍服务",
                    description = "用于检测应用启动",
                    isEnabled = permissionState.accessibilityEnabled,
                    onClick = { viewModel.openAccessibilitySettings() }
                )
            }

            item {
                PermissionItem(
                    title = "悬浮窗权限",
                    description = "用于显示干预界面",
                    isEnabled = permissionState.overlayEnabled,
                    onClick = { viewModel.openOverlaySettings() }
                )
            }

            item {
                PermissionItem(
                    title = "忽略电池优化",
                    description = "防止服务被系统杀死",
                    isEnabled = permissionState.batteryOptimizationDisabled,
                    onClick = { viewModel.openBatterySettings() }
                )
            }

            // MIUI specific permissions
            if (permissionState.isMiui) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "MIUI 专属设置",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text("自启动权限") },
                        supportingContent = { Text("允许应用开机自启") },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        trailingContent = {
                            TextButton(onClick = { viewModel.openMiuiAutoStartSettings() }) {
                                Text("去开启")
                            }
                        }
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text("后台弹出界面") },
                        supportingContent = { Text("允许后台弹出干预界面") },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        trailingContent = {
                            TextButton(onClick = { viewModel.openMiuiBackgroundPopupSettings() }) {
                                Text("去开启")
                            }
                        }
                    )
                }
            }

            // Intervention settings
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "干预设置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            item {
                SliderSettingItem(
                    title = "默认倒计时",
                    value = defaultCountdown,
                    valueRange = 3f..30f,
                    unit = "秒",
                    onValueChange = { viewModel.setDefaultCountdown(it) }
                )
            }

            item {
                SliderSettingItem(
                    title = "冷却时间",
                    value = cooldownMinutes,
                    valueRange = 1f..30f,
                    unit = "分钟",
                    onValueChange = { viewModel.setCooldownMinutes(it) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = {
            Icon(
                imageVector = if (isEnabled) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isEnabled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error
            )
        },
        trailingContent = {
            if (!isEnabled) {
                TextButton(onClick = onClick) {
                    Text("去开启")
                }
            }
        },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun SliderSettingItem(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = "$value $unit", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange,
            steps = (valueRange.endInclusive - valueRange.start).toInt() - 1
        )
    }
}
```

**Step 3: Verify compilation**

Run: `cd D:\devlopment\SlowDown && .\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add -A
git commit -m "feat(ui): add SettingsScreen with permissions and preferences"
```

---

## Task 14: Create AccessibilityService

**Files:**
- Create: `app/src/main/res/xml/accessibility_service_config.xml`
- Create: `app/src/main/java/com/example/slowdown/service/AppMonitorService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Step 1: Create accessibility service config**

Create `app/src/main/res/xml/accessibility_service_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagIncludeNotImportantViews"
    android:canRetrieveWindowContent="false"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:settingsActivity="com.example.slowdown.MainActivity" />
```

**Step 2: Add string resource**

Update `app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">SlowDown</string>
    <string name="accessibility_service_description">监控应用启动以提供数字正念干预</string>
</resources>
```

**Step 3: Create AppMonitorService**

Create `app/src/main/java/com/example/slowdown/service/AppMonitorService.kt`:

```kotlin
package com.example.slowdown.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.slowdown.SlowDownApp
import com.example.slowdown.ui.overlay.OverlayActivity
import com.example.slowdown.util.PackageUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppMonitorService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val cooldownMap = mutableMapOf<String, Long>()

    private val repository by lazy {
        (application as SlowDownApp).repository
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Skip our own package
        if (packageName == this.packageName) return

        // Skip system critical apps
        if (PackageUtils.isSystemCriticalApp(packageName)) return

        serviceScope.launch {
            handleAppLaunch(packageName)
        }
    }

    private suspend fun handleAppLaunch(packageName: String) {
        // Check if service is enabled
        val serviceEnabled = repository.serviceEnabled.first()
        if (!serviceEnabled) return

        // Check if app is monitored
        val monitoredApp = repository.getMonitoredApp(packageName) ?: return
        if (!monitoredApp.isEnabled) return

        // Check cooldown
        val cooldownMinutes = repository.cooldownMinutes.first()
        val lastIntervention = cooldownMap[packageName] ?: 0
        val cooldownMs = cooldownMinutes * 60 * 1000L
        if (System.currentTimeMillis() - lastIntervention < cooldownMs) return

        // Update cooldown
        cooldownMap[packageName] = System.currentTimeMillis()

        // Launch overlay
        val intent = Intent(this, OverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(OverlayActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(OverlayActivity.EXTRA_APP_NAME, monitoredApp.appName)
            putExtra(OverlayActivity.EXTRA_COUNTDOWN_SECONDS, monitoredApp.countdownSeconds)
            putExtra(OverlayActivity.EXTRA_REDIRECT_PACKAGE, monitoredApp.redirectPackage)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
```

**Step 4: Update AndroidManifest.xml**

Add the service declaration to `app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
        tools:ignore="QueryAllPackagesPermission" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application
        android:name=".SlowDownApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.SlowDown"
        tools:targetApi="31">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.SlowDown">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".ui.overlay.OverlayActivity"
            android:exported="false"
            android:excludeFromRecents="true"
            android:launchMode="singleTop"
            android:theme="@style/Theme.SlowDown.Overlay" />

        <service
            android:name=".service.AppMonitorService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="false">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

    </application>

</manifest>
```

**Step 5: Create overlay theme**

Update `app/src/main/res/values/themes.xml` to add overlay theme:

```xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.SlowDown" parent="Theme.Material3.DayNight.DynamicColors">
    </style>

    <style name="Theme.SlowDown.Overlay" parent="Theme.Material3.DayNight.DynamicColors">
        <item name="android:windowIsTranslucent">false</item>
        <item name="android:windowBackground">@android:color/black</item>
        <item name="android:windowFullscreen">true</item>
        <item name="android:windowNoTitle">true</item>
    </style>
</resources>
```

**Step 6: Verify compilation**

Run: `cd D:\devlopment\SlowDown && .\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add -A
git commit -m "feat(service): add AccessibilityService for app monitoring"
```

---

## Task 15: Create Overlay Activity and ViewModel

**Files:**
- Create: `app/src/main/java/com/example/slowdown/viewmodel/OverlayViewModel.kt`
- Create: `app/src/main/java/com/example/slowdown/ui/overlay/OverlayActivity.kt`

**Step 1: Create OverlayViewModel**

Create `app/src/main/java/com/example/slowdown/viewmodel/OverlayViewModel.kt`:

```kotlin
package com.example.slowdown.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.slowdown.data.local.entity.InterventionRecord
import com.example.slowdown.data.repository.SlowDownRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OverlayViewModel(
    private val repository: SlowDownRepository
) : ViewModel() {

    private val _countdown = MutableStateFlow(10)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private val _canContinue = MutableStateFlow(false)
    val canContinue: StateFlow<Boolean> = _canContinue.asStateFlow()

    private var countdownJob: Job? = null
    private var startTime: Long = 0
    private var packageName: String = ""
    private var appName: String = ""
    private var initialCountdown: Int = 10

    fun startCountdown(packageName: String, appName: String, seconds: Int) {
        this.packageName = packageName
        this.appName = appName
        this.initialCountdown = seconds
        this.startTime = System.currentTimeMillis()

        _countdown.value = seconds
        _canContinue.value = false

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in seconds downTo 1) {
                _countdown.value = i
                delay(1000)
            }
            _countdown.value = 0
            _canContinue.value = true
        }
    }

    fun recordAndFinish(userChoice: String) {
        viewModelScope.launch {
            val actualWaitTime = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            repository.recordIntervention(
                InterventionRecord(
                    packageName = packageName,
                    appName = appName,
                    timestamp = System.currentTimeMillis(),
                    interventionType = "countdown",
                    userChoice = userChoice,
                    countdownDuration = initialCountdown,
                    actualWaitTime = actualWaitTime
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }

    class Factory(private val repository: SlowDownRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OverlayViewModel(repository) as T
        }
    }
}
```

**Step 2: Create OverlayActivity**

Create `app/src/main/java/com/example/slowdown/ui/overlay/OverlayActivity.kt`:

```kotlin
package com.example.slowdown.ui.overlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slowdown.SlowDownApp
import com.example.slowdown.ui.theme.SlowDownTheme
import com.example.slowdown.util.PackageUtils
import com.example.slowdown.viewmodel.OverlayViewModel

class OverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_COUNTDOWN_SECONDS = "countdown_seconds"
        const val EXTRA_REDIRECT_PACKAGE = "redirect_package"
    }

    private val viewModel: OverlayViewModel by viewModels {
        OverlayViewModel.Factory((application as SlowDownApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run {
            finish()
            return
        }
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: packageName
        val countdownSeconds = intent.getIntExtra(EXTRA_COUNTDOWN_SECONDS, 10)
        val redirectPackage = intent.getStringExtra(EXTRA_REDIRECT_PACKAGE)

        viewModel.startCountdown(packageName, appName, countdownSeconds)

        setContent {
            SlowDownTheme {
                OverlayScreen(
                    viewModel = viewModel,
                    appName = appName,
                    redirectPackage = redirectPackage,
                    onContinue = {
                        viewModel.recordAndFinish("continued")
                        finish()
                    },
                    onRedirect = { redirectPkg ->
                        viewModel.recordAndFinish("redirected")
                        PackageUtils.launchApp(this, redirectPkg)
                        finish()
                    },
                    onCancel = {
                        viewModel.recordAndFinish("cancelled")
                        PackageUtils.goHome(this)
                        finish()
                    }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        viewModel.recordAndFinish("cancelled")
        PackageUtils.goHome(this)
        super.onBackPressed()
    }
}

@Composable
private fun OverlayScreen(
    viewModel: OverlayViewModel,
    appName: String,
    redirectPackage: String?,
    onContinue: () -> Unit,
    onRedirect: (String) -> Unit,
    onCancel: () -> Unit
) {
    val countdown by viewModel.countdown.collectAsState()
    val canContinue by viewModel.canContinue.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "你正在打开",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = appName,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = if (canContinue) "可以继续了" else countdown.toString(),
                color = if (canContinue) Color.Green else Color.White,
                fontSize = if (canContinue) 24.sp else 72.sp,
                textAlign = TextAlign.Center
            )

            if (!canContinue) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "请等待倒计时结束",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Redirect button (if configured)
            if (redirectPackage != null) {
                Button(
                    onClick = { onRedirect(redirectPackage) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("打开微信读书", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Continue button
            Button(
                onClick = onContinue,
                enabled = canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canContinue) MaterialTheme.colorScheme.primary
                                    else Color.Gray,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = if (canContinue) "继续访问" else "等待中...",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Cancel link
            TextButton(onClick = onCancel) {
                Text(
                    text = "返回桌面",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
```

**Step 3: Verify compilation**

Run: `cd D:\devlopment\SlowDown && .\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add -A
git commit -m "feat(ui): add OverlayActivity for intervention display"
```

---

## Task 16: Create Navigation and MainActivity

**Files:**
- Create: `app/src/main/java/com/example/slowdown/ui/navigation/NavGraph.kt`
- Modify: `app/src/main/java/com/example/slowdown/MainActivity.kt`
- Delete: `app/src/main/java/com/example/slowdown/FirstFragment.kt`
- Delete: `app/src/main/java/com/example/slowdown/SecondFragment.kt`
- Delete: `app/src/main/res/layout/*.xml`
- Delete: `app/src/main/res/navigation/nav_graph.xml`
- Delete: `app/src/main/res/menu/menu_main.xml`

**Step 1: Create NavGraph**

Create `app/src/main/java/com/example/slowdown/ui/navigation/NavGraph.kt`:

```kotlin
package com.example.slowdown.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.slowdown.SlowDownApp
import com.example.slowdown.ui.screen.AppListScreen
import com.example.slowdown.ui.screen.DashboardScreen
import com.example.slowdown.ui.screen.SettingsScreen
import com.example.slowdown.viewmodel.AppListViewModel
import com.example.slowdown.viewmodel.DashboardViewModel
import com.example.slowdown.viewmodel.SettingsViewModel

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object AppList : Screen("app_list")
    data object Settings : Screen("settings")
}

@Composable
fun NavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as SlowDownApp

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            val viewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(app.repository)
            )
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToAppList = { navController.navigate(Screen.AppList.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.AppList.route) {
            val viewModel: AppListViewModel = viewModel(
                factory = AppListViewModel.Factory(app.repository, context)
            )
            AppListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app.repository, context)
            )
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

**Step 2: Update MainActivity**

Replace `app/src/main/java/com/example/slowdown/MainActivity.kt`:

```kotlin
package com.example.slowdown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.slowdown.ui.navigation.NavGraph
import com.example.slowdown.ui.theme.SlowDownTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SlowDownTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
```

**Step 3: Delete old files**

```bash
rm app/src/main/java/com/example/slowdown/FirstFragment.kt
rm app/src/main/java/com/example/slowdown/SecondFragment.kt
rm -rf app/src/main/res/layout
rm app/src/main/res/navigation/nav_graph.xml
rm app/src/main/res/menu/menu_main.xml
```

**Step 4: Verify compilation**

Run: `cd D:\devlopment\SlowDown && .\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add -A
git commit -m "feat: setup Compose navigation and MainActivity"
```

---

## Task 17: Build and Test

**Step 1: Full build**

Run: `cd D:\devlopment\SlowDown && .\gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 2: Install on device**

Run: `cd D:\devlopment\SlowDown && .\gradlew installDebug`
Expected: Installation successful

**Step 3: Manual testing checklist**

1. Open app, verify Dashboard shows
2. Navigate to Settings, verify permissions display
3. Enable Accessibility Service
4. Grant Overlay permission
5. Navigate to App List, select an app to monitor
6. Launch monitored app, verify intervention appears
7. Wait for countdown, verify "继续访问" becomes active
8. Verify statistics update on Dashboard

**Step 4: Final commit**

```bash
git add -A
git commit -m "chore: complete MVP implementation"
```

---

## Summary

| Task | Description | Files |
|------|-------------|-------|
| 1 | Configure Gradle | build.gradle.kts, libs.versions.toml |
| 2 | Room Entities | InterventionRecord.kt, MonitoredApp.kt |
| 3 | Room DAOs | InterventionDao.kt, MonitoredAppDao.kt |
| 4 | Room Database | AppDatabase.kt |
| 5 | DataStore | UserPreferences.kt |
| 6 | Repository | SlowDownRepository.kt |
| 7 | Application | SlowDownApp.kt, AndroidManifest.xml |
| 8 | Utilities | PackageUtils.kt, PermissionHelper.kt |
| 9 | Theme | Color.kt, Type.kt, Theme.kt |
| 10 | Dashboard VM | DashboardViewModel.kt |
| 11 | Dashboard UI | DashboardScreen.kt |
| 12 | AppList | AppListViewModel.kt, AppListScreen.kt |
| 13 | Settings | SettingsViewModel.kt, SettingsScreen.kt |
| 14 | Service | AppMonitorService.kt, accessibility_config.xml |
| 15 | Overlay | OverlayViewModel.kt, OverlayActivity.kt |
| 16 | Navigation | NavGraph.kt, MainActivity.kt |
| 17 | Build & Test | Full integration test |
