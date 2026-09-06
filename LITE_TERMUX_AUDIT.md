# LITE Termux Audit (Agent-2) — 2026-09-06

Branch: `dev` (branch `lite/ram2gb-lite` belum ada — kerja di `dev`, tanpa buat branch tandingan).
Tujuan: pangkas dependency/download Termux seminimal mungkin, tetap bisa build **Kotlin dan Java via Gradle**.
Batasan: `generativeai` (`com.google.ai.client.generativeai:generativeai:0.9.0`, `core/app/build.gradle.kts:267`) **WAJIB dipertahankan, tidak disentuh**.

## 1. File yang dibaca (16)

| # | File | Temuan kunci |
|---|------|--------------|
| 1 | `termux/application/build.gradle.kts` | lib, deps: androidx annotation/core/drawer/preference/viewpager, material, guava, markwon x4, `core.projects/common/resources`, `termux.view`, `termux.shared`, `utilities.preferences` |
| 2 | `termux/emulator/build.gradle.kts` | lib `com.termux.emulator`, NDK 27.1.12297006 (arm64+arm), deps HANYA `androidx.annotation` — paling ringan |
| 3 | `termux/shared/build.gradle.kts` | lib `com.termux.shared`, NDK 27.1.12297006, deps: appcompat/annotation/core, `window-v1alpha9` (komentar: JANGAN naik >1.0.0-alpha09, brk 58-59), markwon x4, material, guava, hiddenApiBypass, `commons-io` (60), **`common.termuxAmLib` v2.0.0 (61)**, core.common/resources, termux.view, buildInfo, preferences |
| 4 | `termux/view/build.gradle.kts` | lib `com.termux.view`, `api(projects.termux.emulator)` + annotation + core.resources — ringan |
| 5 | `gradle/libs.versions.toml:95` | `common-termuxAmLib = { module = "com.termux:termux-am-library", version = "v2.0.0" }` — TIDAK dihapus (sesuai instruksi), hanya kandidat |
| 6 | `core/app/build.gradle.kts:282-285` | 4 modul termux sebagai `implementation` (application, view, emulator, shared) + komentar `TODO LITE (Agent-2)` baru; generativeai di 267 utuh |
| 7 | `termux/.../TermuxInstaller.java` | Bootstrap = zip dari native lib `termux-bootstrap` (`loadZipBytes()` + `getZip()` native, 375-381), ekstrak ke `$PREFIX` staging lalu rename; BUKAN download apt saat install awal |
| 8 | `termux/.../cpp/termux-bootstrap.c` + `Android.mk` | JNI `Java_com_termux_app_TermuxInstaller_getZip`, blob dari `termux-bootstrap-zip.S` (prebuilt, sumber zip tidak di repo) |
| 9 | `termux/.../terminal/IdesetupSession.kt` | Menyalin asset `data/common/<arch>/idesetup` (arm/arm64) ke files/temp lalu eksekusi sebagai sesi terminal |
| 10 | `termux/.../activities/TerminalActivity.kt:121-143` | `addIdesetupSession(args)` — menjalankan biner idesetup + argumen onboarding |
| 11 | `core/common/.../managers/ToolsManager.java` | Ekstrak `android.jar`, `tooling-api-all.jar`, `androidide.init.gradle`, aapt2 dari `libaapt2.so` — independen dari apt Termux |
| 12 | `core/common/.../utils/Environment.java:96-103` | `JAVA_HOME` = `lib/jvm/java-17-openjdk` (fallback `java-21-openjdk`); `BASH_SHELL`/`LOGIN_SHELL` = `$PREFIX/bin/{bash,login}` — bukti JDK + bash dari prefix WAJIB |
| 13 | `.../fragments/onboarding/IdeSetupConfigurationFragment.kt:146-170` | Argumen idesetup: `--install-dir --sdk --jdk --ndk --assume-yes [--with-git] [--with-openssh]` |
| 14 | `.../fragments/onboarding/ideSetupConfig.kt` | `SdkVersion`: 33.0.1–35.0.1 (6 entri); `JdkVersion`: 17, 21; `NdkVersion`: `0`=Skip, `28.2.13676358` |
| 15 | `.../models/IdeSetupArguments.kt` | Enum flag idesetup (INSTALL_DIR, WITH_GIT, ASSUME_YES, WITH_OPENSSH, SDK_VERSION, JDK_VERSION, NDK_VERSION) |
| 16 | `.../activities/OnboardingActivity.kt:168-182` + `GradleBuildService.kt:30,545` + `JdkUtils.kt:22` | Syarat setup = JDK terinstal + `ANDROID_HOME` ada; build Gradle memakai `TermuxShellEnvironment` (dari `termux.shared`) — bukti shared WAJIB |

Tambahan dicek: `termux/shared/.../termux/shell/am/TermuxAmSocketServer.java` (bungkus `AmSocketServer`, aktif via properti; dipanggil 1x dari `TermuxApplication.java:53`),
`shared/reflection/ReflectionUtils.java:10,32` (satu-satunya pemakai hiddenApiBypass), `shared/markdown/MarkdownUtils.java` (pemakai markwon),
`shared/res/raw/apt_info_script.sh` (hanya diagnostik `apt list`, bukan installer), aset `idesetup` = ELF aarch64 stripped 30 KB (arm64) / 38 KB (arm) — daftar paket apt TIDAK bisa diekstrak dari biner, disimpulkan dari flag + `Environment.java`.

## 2. Keputusan wajib vs opsional — modul termux

| Modul | Status | Alasan |
|-------|--------|--------|
| `termux:emulator` (`termux/emulator/build.gradle.kts`) | **WAJIB** | Inti emulasi terminal; deps hanya `androidx.annotation`; dipakai `termux:view` (api) dan sesi build. Hapus = tidak ada output build. |
| `termux:view` (`termux/view/build.gradle.kts`) | **WAJIB** | `TerminalView` untuk `TermuxActivity`/Gradle log/idesetup session; ringan (emulator+annotation+resources). |
| `termux:shared` (`termux/shared/build.gradle.kts`) | **WAJIB (inti), pangkas isi** | `TermuxShellEnvironment`, `ExecutionCommand`, `TermuxConstants/FileUtils` dipakai `GradleBuildService`, `JdkUtils`, `Environment`. Hapus modul = Gradle build mati. Yang dipangkas hanya deps opsional di dalamnya (lihat §3). |
| `termux:application` (`termux/application/build.gradle.kts`) | **WAJIB** | `TermuxService`, `TermuxInstaller` (bootstrap `$PREFIX`), `TerminalActivity`/`IdesetupSession`. Tanpa ini tidak ada prefix/JDK/SDK. |

> `core/app/build.gradle.kts:282-285` tetap 4x `implementation(...)`. JANGAN ubah ke `liteImplementation` sebelum ada stub/fallback tanpa-terminal (catatan sudah ditambah sebagai komentar `TODO LITE` di file).

## 3. Dependency dalam modul termux — wajib vs kandidat pangkas

| Dependency | Lokasi | Status | Alasan / dampak hapus |
|------------|--------|--------|------------------------|
| `androidx.annotation/core/appcompat/drawer/preference/viewpager`, `window-v1alpha9` | shared, application | **WAJIB** | dipakai luas; window JANGAN naik versi (komentar brk 58-59 `shared/build.gradle.kts`) |
| `google.guava`, `commons-io` | shared, application | **WAJIB** | dipakai `core.common`, javac-services, LSP; hapus = compile error luas |
| `core.common/resources/projects`, `utilities.preferences/buildInfo`, `termux.view/emulator` | shared, application | **WAJIB** | rantai kompilasi terminal+build |
| `com.termux:termux-am-library v2.0.0` (`common-termuxAmLib`, `libs.versions.toml:95` → `shared/build.gradle.kts:61`) | shared | **OPSIONAL (kandidat #1)** | Hanya untuk `AmSocketServer`/`TermuxAmSocketServer` (instal via `am`, dipanggil 1x di `TermuxApplication:53`). Build Kotlin/Java via Gradle TIDAK butuh. Hapus butuh stub/guard 2 call-site — serahkan Agent-3/4. **TOML tidak dihapus.** |
| markwon x4 (`core, extStrikethrough, linkify, recycler`) | shared (50-53), application (50-53) | **OPSIONAL (kandidat #2)** | Hanya `MarkdownUtils` (dialog error/laporan). Bisa diganti `TextView` polos. Hapus butuh edit `MarkdownUtils.java` + pemanggil — serahkan Agent-3/4. |
| `hiddenApiBypass` (`org.lsposed...:6.1`) | shared (56) | **OPSIONAL (kandidat #3)** | Hanya `ReflectionUtils.java:10,32`. Non-fatal di API baru; hapus butuh try/catch fallback — serahkan Agent-3/4. |
| `google.material` di modul termux | shared (54), application (48) | **Tahan dulu** | Dipakai UI terminal; hemat kecil, risiko UI rusak. Bukan prioritas lite. |

## 4. Paket bootstrap/runtime yang bisa dipangkas (ringkas: hanya Kotlin+Java)

Biner `idesetup` tertutup, tapi flag onboarding + `Environment.java` memberi daftar pasti:

| Paket / komponen | Status lite | Dasar |
|------------------|-------------|-------|
| Base prefix dari `termux-bootstrap` zip (bash, coreutils, apt/dpkg, linker) | **WAJIB** | `shellExists()`, `BASH_SHELL`/`LOGIN_SHELL` (`Environment.java:102-103`); tanpa ini tidak ada shell Gradle |
| JDK **17 saja** (`lib/jvm/java-17-openjdk`) | **WAJIB (satu)** | `Environment.java:96-99`; `JdkVersion` sediakan 17+21 — lite kunci **JDK 17**, hapus opsi JDK 21 dari default onboarding |
| Android SDK **satu versi (35.0.1)** | **WAJIB (satu)** | `SdkVersion` ada 6 entri (33.0.1→35.0.1); lite kunci **35.0.1** (`ALL` arch), sembunyikan sisanya |
| Gradle distribution (wrapper, `GradleBuildService:441`) | **WAJIB (cache)** | Diunduh sekali lalu cache di `GRADLE_USER_HOME`; lite: pertahankan cache, jangan wipe |
| `android.jar` + `tooling-api-all.jar` + `init.gradle` + aapt2 (`ToolsManager`) | **WAJIB (aset lokal)** | Sudah dari APK assets, bukan apt — tidak dipangkas |
| NDK `28.2.13676358` (`--ndk`, default bisa `0`=Skip) | **PANGKAS (default Skip)** | Hanya untuk template NativeCpp; pesan `idesetup -y -c -wn` hanya muncul saat NDK hilang (3 call-site §1) — Kotlin/Java murni tidak butuh; hemat ratusan MB |
| `clang`/`cmake` (ikut NDK/native) | **PANGKAS** | Tidak dipakai kompilasi Kotlin/Java/Gradle JVM |
| `--with-git` | **PANGKAS (default off)** | Version control saja; build tidak butuh |
| `--with-openssh` | **PANGKAS (default off)** | SSH saja; build tidak butuh |
| `apt update` diagnostik (`apt_info_script.sh`) | **Biarkan** | Hanya untuk laporan bug, tidak mengunduh saat build |

**Versi minimal usul lite:** base prefix + JDK 17 + SDK 35.0.1 + Gradle cache + aset lokal (`android.jar`, `tooling-api`, `init.gradle`, aapt2). NDK=`0` (Skip), git=off, openssh=off.

## 5. Rekomendasi konkret untuk Agent-3/4 (file+baris, tanpa merusak Kotlin/Java)

1. `gradle/libs.versions.toml:95` — JANGAN hapus baris; saat siap: hapus `implementation(libs.common.termuxAmLib)` di `termux/shared/build.gradle.kts:61` + guard `TermuxApplication.java:53` (`setupTermuxAmSocketServer`) + stub `TermuxAmSocketServer`. (Hanya installer `am`; build aman.)
2. `termux/shared/build.gradle.kts:50-53` + `termux/application/build.gradle.kts:50-53` — hapus markwon x4 + sederhanakan `MarkdownUtils.java` ke TextView polos. (Hanya dialog; build aman.)
3. `termux/shared/build.gradle.kts:56` — hapus `hiddenApiBypass` + fallback di `ReflectionUtils.java:25-32`. (Hanya refleksi; build aman.)
4. `core/app/.../fragments/onboarding/ideSetupConfig.kt:30-84` — lite: default `SdkVersion.SDK_35_0_1`, `JdkVersion.JDK_17`, `NdkVersion.NDK_DISABLE`; sembunyikan entri lain di UI (jangan hapus enum agar `full` tetap bisa).
5. `IdeSetupConfigurationFragment.kt:146-170` — lite: default `installGit`/`installOpenssh` unchecked.
6. `core/app/build.gradle.kts:282-285` — TETAP `implementation` 4 modul termux (komentar TODO LITE sudah dipasang). Konversi ke `liteImplementation` DILARANG sebelum ada fallback tanpa-terminal.
7. JANGAN sentuh: `core/app/build.gradle.kts:267` generativeai, `window-v1alpha9` (komentar brk 58-59), blok flavor Agent-3, `gradle.properties`, uidesigner.
8. NDK/clang/cmake TIDAK ada di `build.gradle.kts` — pemangkasannya cukup via default onboarding (butir 4-5), tanpa edit native.

## 6. Edit yang dilakukan Agent-2

- `core/app/build.gradle.kts` (di atas brk 282): **komentar `TODO LITE` saja** (6 baris), menandai 4 modul termux WAJIB + menunjuk laporan ini + melarang konversi ke `liteImplementation` tanpa stub. Tidak ada dependency ditambah/dihapus/diubah; generativeai utuh (diff tidak menyentuh baris generativeai — verifikasi `grep -n` masih 267).
- `gradle/libs.versions.toml`: **tidak diubah** (kandidat hanya dicatat §3 butir 1).
- File Agent-3/4 (flavor di `core/app/build.gradle.kts:72-90`, `proguard-rules.pro`, `gradle.properties.lite`, `LiteMode.kt`, uidesigner): **tidak disentuh**.
