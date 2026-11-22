# Panduan Build Signed Release APK (Android Studio)

Dokumen ini menjelaskan langkah-langkah untuk membangun file APK versi **release** yang ditandatangani (signed) di komputer Anda sendiri menggunakan **Android Studio**.

> **PERINGATAN PENTING — CADANGKAN DATA SEBELUM INSTALL!**
>
> Jika di HP Anda saat ini sudah terpasang aplikasi versi lama (misalnya versi debug dari emulator/Android Studio atau APK dengan signing key yang berbeda), Android **tidak mengizinkan** pembaruan langsung dengan key baru (*App not installed* / `INSTALL_FAILED_UPDATE_INCOMPATIBLE`).
>
> Anda **wajib meng-uninstall aplikasi lama terlebih dahulu**, dan proses uninstall akan **menghapus seluruh data lokal** (riwayat latihan, catatan berat badan, ukuran tubuh, serta pengaturan).
>
> **Sebelum meng-uninstall aplikasi lama:**
> 1. Buka aplikasi V-Taper Grind Tracker di HP Anda.
> 2. Masuk ke tab **Progress**.
> 3. Gulir ke bagian **Cadangkan & Pulihkan Data**.
> 4. Ketuk **Export Data** dan simpan file `.json` backup di penyimpanan HP atau Google Drive Anda.
> 5. Setelah APK release baru selesai di-install, buka tab **Progress** → **Import Data** untuk memulihkan semua data Anda.

---

## 1. Membuka Proyek di Android Studio

1. Pastikan Anda sudah menginstall **Android Studio** versi terbaru.
2. Ekstrak file ZIP proyek (atau *clone* repository) ke folder di komputer Anda.
3. Buka Android Studio, lalu pilih **Open** (atau **File > Open...**).
4. Arahkan ke folder root proyek (folder yang berisi `settings.gradle.kts`, `build.gradle.kts`, dan folder `app/`), lalu klik **OK**.
5. Tunggu hingga proses **Gradle Sync** selesai tanpa error.

---

## 2. Membuat Keystore Baru

Jika Anda belum memiliki file keystore (`.jks`), buat melalui Android Studio:

1. Di menu atas Android Studio, klik **Build > Generate Signed App Bundle / APK...**
2. Pilih **APK**, lalu klik **Next**.
3. Di bagian *Key store path*, klik **Create new...**
4. Isi formulir *New Key Store*:
   - **Key store path**: Pilih lokasi penyimpanan file keystore, misalnya langsung di folder root proyek dengan nama `release-key.jks` (sudah diabaikan oleh `.gitignore`).
   - **Password** & **Confirm**: Buat password untuk keystore dan catat dengan aman.
   - **Key > Alias**: Masukkan nama alias, misalnya `release`.
   - **Key > Password** & **Confirm**: Buat password untuk key alias tersebut dan catat.
   - **Validity (years)**: Biarkan default (`25` tahun atau lebih).
   - **Certificate**: Isi minimal satu kolom seperti *First and Last Name*.
5. Klik **OK**.

---

## 3. Mengkonfigurasi `keystore.properties` (Untuk Build Otomatis via Gradle)

Agar Android Studio / Gradle otomatis menandatangani build `release` setiap kali Anda build atau memilih build variant `release`:

1. Buat file baru bernama `keystore.properties` di **root proyek** (sejajar dengan `settings.gradle.kts` dan `gradle.properties`).
2. Isi file `keystore.properties` dengan format berikut (sesuaikan dengan data keystore Anda):

```properties
storeFile=release-key.jks
storePassword=PASSWORD_KEYSTORE_ANDA
keyAlias=release
keyPassword=PASSWORD_KEY_ANDA
```

> **Catatan:**
> - Jangan pernah membagikan atau meng-commit `keystore.properties` maupun file `*.jks` ke Git (keduanya sudah ditambahkan ke `.gitignore`).
> - Jika `keystore.properties` tidak ada, build `release` tetap bisa dikompilasi karena otomatis menggunakan key debug bawaan Android sebagai fallback agar build tidak gagal karena ketiadaan kunci rilis; namun APK tersebut hanya untuk pengujian dan **tidak boleh dipublikasikan**.

---

## 4. Membangun Release APK

Anda bisa memilih salah satu dari dua cara berikut:

### Cara A: Melalui Wizard Android Studio (Direkomendasikan)
1. Klik **Build > Generate Signed App Bundle / APK...**
2. Pilih **APK** → **Next**.
3. Pastikan *Key store path*, *Key store password*, *Key alias*, dan *Key password* sudah terisi → klik **Next**.
4. Pada *Build Variants*, pilih **release**.
5. Klik **Create** (atau **Finish**).
6. Tunggu hingga notifikasi *Generate Signed APK* selesai muncul.

### Cara B: Melalui Menu Build / Build Variants (Menggunakan `keystore.properties`)
1. Pastikan `keystore.properties` sudah dibuat di root proyek.
2. Buka panel **Build Variants** di sisi kiri Android Studio, ubah *Active Build Variant* modul `:app` menjadi **release**.
3. Klik menu **Build > Build Bundle(s) / APK(s) > Build APK(s)** (atau jalankan di terminal Android Studio: `./gradlew assembleRelease` / `gradle :app:assembleRelease`).

---

## 5. Lokasi File Output APK

Setelah build berhasil, file APK release tersedia di salah satu lokasi berikut tergantung metode yang digunakan:

- **Via *Generate Signed App Bundle / APK***:
  `app/release/app-release.apk`
- **Via *Build APK(s)* atau Gradle (`assembleRelease`)**:
  `app/build/outputs/apk/release/app-release.apk`

---

## 6. Cara Install APK ke HP dengan USB Debugging

1. **Export backup terlebih dahulu** di aplikasi lama (lihat peringatan di bagian atas panduan ini), lalu **uninstall** aplikasi versi lama dari HP jika sebelumnya menggunakan signing key berbeda.
2. Di HP Android Anda:
   - Buka **Setelan (Settings) > Tentang ponsel (About phone)**.
   - Ketuk **Nomor bentukan (Build number)** sebanyak 7 kali hingga muncul pesan *"Anda sekarang adalah pengembang"*.
   - Buka **Setelan > Sistem > Opsi pengembang (Developer options)** dan aktifkan **Debugging USB (USB debugging)**.
3. Sambungkan HP ke komputer menggunakan kabel USB. Jika muncul popup *"Izinkan debugging USB?"* di layar HP, centang *Selalu izinkan* lalu ketuk **Izinkan (Allow)**.
4. Jalankan perintah berikut di Terminal Android Studio (sesuaikan path APK dengan output pada langkah 5):

```bash
adb devices
adb install app/release/app-release.apk
```

*(Atau jika memakai Build APK(s) / Gradle: `adb install app/build/outputs/apk/release/app-release.apk`)*

> Sebagai alternatif, Anda juga bisa menyalin file `app-release.apk` ke penyimpanan HP lalu membukanya dari File Manager untuk meng-install secara langsung.
