## v2.7.4-r1.02

### Build System
- Upgraded the **NDK** to **r28c**.  
- Added **NDK support** for the following ABIs: **arm64-v8a**, **armeabi-v7a**, and **x86_64**.  
  > Note: Since AndroidIDE-Rv2 primarily targets **aarch64** (for now), the default ABI remains **arm64-v8a**.

- Updated SDKs to the latest Android versions (**Android 15** and **Android 16**).  
- Updated **compileSdk** and added support for **Android platforms 35 and 36**.
- Upgraded Gradle distribution to **8.11.1** and Gradle plugin to **8.9.1**.  
- Updated Kotlin to **2.0.21**.  
- Upgraded all remaining dependencies to their latest versions.  
- Pre-installed NDK (c28.2.13676358)  
  (You can install it via **Main Screen → IDE Configurations**, or by opening a terminal and running:  
  `idesetup -c -y -wn`)
  
### Editor
- **UI Designer**: Added additional layouts, widgets, and partial support for **Material 3** designs.  
- Updated the editor color scheme to **Dracula** for a modern and consistent look.  

### Bug Fixes
- Fixed a crash that occurred when switching between **Dark** and **Light** modes.  
- Fixed I/O operation issues that causes the IDE to crash while coding.
- Fixed a crash in apps built with Rv2IDE when the **LogSender** plugin was enabled, caused by a missing data sync permission.

### Join my telegram channel
- https://t.me/rv2ide
