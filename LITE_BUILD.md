# LITE BUILD — Akselerasi Build HP RAM 2GB (Agent-4)

> Batasan: JANGAN edit `termux/*` atau flavor logic inti (milik Agent-2/3).
> JANGAN sentuh AI deps (`generativeai`). Hanya tambah config build.
> Dibaca: `gradle.properties`, `build.gradle.kts` (root), `core/app/build.gradle.kts`, `gradle/libs.versions.toml` (AGP 8.13.0, Kotlin 2.1.0).

## 1. File dibuat/diubah

1. **BARU: `gradle.properties.lite`** — tuning RAM kecil. Asli TIDAK dioverwrite.
2. **EDIT (komentar saja): `core/app/build.gradle.kts`** — tambah `// LITE-BUILD-SNIPPET` non-aktif agar tidak conflict Agent-3. Update: saat penulisan, flavor `lite`/`full` dimensi `tier` milik Agent-3 sudah muncul di working tree (uncommitted) — snippet diselaraskan ke `getByName("lite")` + `dimension = "tier"`, tetap dikomen.
3. **BARU (file ini): `LITE_BUILD.md`** — instruksi + laporan.

## 2. Cara pakai

```bash
# backup dulu (sekali saja)
cp gradle.properties gradle.properties.bak

# aktifkan mode lite
cp gradle.properties.lite gradle.properties

# build lite debug, batasi worker (RAM 2GB)
./gradlew :core:app:assembleLiteDebug --max-workers=2 --offline
# atau jika flavor lite belum ada (Agent-3 belum selesai), pakai debug biasa:
./gradlew :core:app:assembleDebug --max-workers=2 --offline

# varian lain (setelah flavor lite ada):
./gradlew :core:app:assembleLiteRelease --max-workers=2
./gradlew :core:app:assembleArm64V8aLiteDebug --max-workers=2

# verifikasi ringan tanpa full build:
./gradlew help --offline --max-workers=2
./gradlew :core:app:tasks --offline --dry-run --max-workers=2

# kembalikan ke normal:
cp gradle.properties.bak gradle.properties
```

> `assembleLiteDebug` butuh `productFlavors { create("lite") }` Agent-3 — saat ini sudah ada di working tree (dimensi `tier`, uncommitted). Jika checkout bersih tanpa itu, pakai `assembleDebug`.

## 3. Sebelum vs sesudah

| Item | Sebelum (`gradle.properties` asli) | Sesudah (`gradle.properties.lite`) | Efek |
|---|---|---|---|
| `org.gradle.jvmargs` Xmx | `-Xmx4096M` + `kotlin.daemon.jvm.options -Xmx4096M` | `-Xmx1024M` + `-Xmx1024M` | Heap -75%, muat di 2GB, cegah OOM-kill |
| `org.gradle.workers.max` | tidak diset (nCPU) | `2` | CPU/RAM stabil |
| `org.gradle.parallel` | `true` | `true` (tetap) | — |
| `org.gradle.caching` | `true` | `true` (tetap) | — |
| `org.gradle.configuration-cache` | dikomen (`#`) | `true` | Build ulang config lebih cepat |
| `android.nonTransitiveRClass` | `false` + TODO Migrate | `true` | Kompilasi R lebih ringan |
| `kotlin.incremental` / `kotlin.parallel.tasks.in.project` | tidak ada | `true` | Incremental lebih cepat |
| `android.useAndroidX` | `true` | `true` (tetap) | — |
| `lint abortOnError` (`core/app`) | `false` | `false` (tetap, lihat snippet) | Lite tidak berhenti karena lint |
| ABI / shrink (snippet) | `release { isShrinkResources=false }`, tanpa `abiFilters` | `lite-debug: minify=false, shrink=false`; `lite-release: minify+shrink=true`; `abiFilters arm64-v8a` | Debug cepat, release kecil |

## 4. Snippet yang ditambah

### 4a. `gradle.properties.lite` (penuh)

```properties
org.gradle.jvmargs=-Xmx1024M \
  -Dkotlin.daemon.jvm.options="-Xmx1024M" \
  -XX:+HeapDumpOnOutOfMemoryError \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.util=ALL-UNNAMED \
  --add-opens java.base/java.io=ALL-UNNAMED
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
org.gradle.configuration-cache=true
org.gradle.workers.max=2
kotlin.incremental=true
kotlin.parallel.tasks.in.project=true
android.useAndroidX=true
android.enableJetifier=false
android.jetifier.ignorelist=common-30.2.2.jar
android.nonTransitiveRClass=true
systemProp.https.protocols=TLSv1,TLSv1.1,TLSv1.2
android.r8.version=8.6.17
org.gradle.configureondemand=false
```

### 4b. `core/app/build.gradle.kts` — komentar non-aktif (tidak hapus blok Agent-3)

```kotlin
// LITE-BUILD-SNIPPET (Agent-4, non-aktif agar tidak conflict Agent-2/3) —
// Agent-3 sudah membuat productFlavors lite/full dimensi "tier" (lihat atas).
// Jangan hapus blok Agent-3. Contoh aktif susulan untuk varian lite (hemat RAM/ukuran, arm64 saja):
// productFlavors {
//   getByName("lite") {
//     dimension = "tier" // sama dengan milik Agent-3
//     ndk { abiFilters += "arm64-v8a" }
//     // resourceConfigurations += listOf("in", "en") // opsional: pangkas bahasa
//   }
// }
// buildTypes {
//   getByName("debug") {
//     // default lite-debug: tanpa minify agar cepat di RAM 2GB
//     isMinifyEnabled = false
//     isShrinkResources = false
//   }
//   getByName("release") {
//     // jika build lite-release: minify+shrink aktif untuk APK kecil
//     // isMinifyEnabled = true
//     // isShrinkResources = true
//   }
// }
// lint { abortOnError = false } // tetap false untuk lite
```

Lokasi: di dalam blok `android { }`, setelah blok `lint { }`, sebelum `packaging { }`. Blok `buildTypes { debug/release }` dan `lint { abortOnError=false }` asli dibiarkan utuh.

## 5. Estimasi hemat (tanpa full build, teoritis)

- Heap Gradle+Kotlin: 8GB (4+4) → 2GB (1+1) = **hemat ~6GB virtual, -75%**, OOM di 2GB jauh berkurang.
- Worker dibatasi 2: cegah thrashing, build lebih lambat ~10-20% di HP 8-core tapi **tidak mati di tengah jalan**.
- `configuration-cache=true`: **hemat 20-40%** waktu pada build incremental/no-op setelah cache hangat.
- `nonTransitiveRClass=true`: modul dependen kompilasi ulang lebih sedikit, **hemat 5-15%** waktu + RAM kompilasi.
- `abiFilters arm64-v8a` saja (saat flavor lite aktif): paket native **~40-50% lebih kecil** vs 2 ABI, packaging/linking lebih cepat.
- `minify=false, shrink=false` untuk `lite-debug`: dex/R8 skip → debug **paling cepat**; aktifkan hanya di `lite-release` untuk APK kecil.
- Total: tidak bisa diukur pasti tanpa device, tapi target **build debug jalan di 2GB** yang sebelumnya OOM pada Xmx4096M.

## 6. Hasil verifikasi ringan

```bash
./gradlew help --offline --max-workers=2
# ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
# java -version: command not found, JAVA_HOME kosong, ANDROID_HOME/SDK tidak ada.
```

- Status: **GAGAL (expected)** — lingkungan Termux ini tanpa Java dan tanpa Android SDK, jadi Gradle tidak bisa jalan.
- Full build sengaja TIDAK dijalankan sesuai instruksi.
- Sintaks: file `.lite` = properties biasa (aman), edit `build.gradle.kts` hanya komentar `//` (tidak merusak kompilasi).
- Langkah lanjut di mesin builder (ada JDK 17 + SDK): jalankan `help --offline` lalu `assembleDebug --max-workers=2` untuk ukur waktu riil.

## 7. Catatan koordinasi

- Agent-2/3: snippet di atas sengaja dikomen. Saat `productFlavors` sudah ada, pindahkan `abiFilters` ke flavor `lite` dan sesuaikan `dimension`.
- Tidak menyentuh `termux/*`, AI deps (`generativeai:0.9.0`), `signingConfigs`, `applicationVariants` rename logic.
