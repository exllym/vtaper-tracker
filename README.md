# V-Taper Grind Tracker (Project Abbas V2)

Aplikasi Android native berbasis **Kotlin** dan **Jetpack Compose (Material 3)** untuk melacak latihan beban pola **Upper/Lower 4 hari per minggu**, memantau progres **lean bulking**, mengevaluasi **Progressive Overload**, serta menjaga konsistensi latihan dan penimbangan badan secara offline.

> [!NOTE]
> Aplikasi ini berjalan 100% secara lokal di perangkat (tanpa akun, tanpa server cloud, dan tanpa dependensi AI/jaringan eksternal) dengan dukungan ekspor/impor backup berformat **JSON**.

---

## Ringkasan Teknis Singkat

| Komponen | Spesifikasi |
| --- | --- |
| **Bahasa & UI** | Kotlin, Jetpack Compose, Material 3 (Dark Theme) |
| **Arsitektur** | MVVM (`MainViewModel` + `StateFlow`) & Repository Pattern |
| **Database Lokal** | Room Database (`vtaper_grind_database`, versi `2`) |
| **Preferensi & State Ringan** | `SharedPreferences` terpisah per domain |
| **Penjadwalan Alarm** | `AlarmManager` (`setExactAndAllowWhileIdle`) + `BroadcastReceiver` |
| **Minimum / Target SDK** | `minSdk 26` (Android 8.0) / `targetSdk 36` (`compileSdk 36.1`) |
| **Optimasi Rilis** | R8 Code Shrinking (`isMinifyEnabled = true`) & Resource Shrinking + Jetpack `profileinstaller` |

---

## Fitur Utama & Alur Layar

Aplikasi memiliki tiga tab navigasi utama (**Today**, **Schedule**, dan **Progress**) serta halaman detail **Volume Stats**.

### 1. Tab Today (Catatan Latihan Harian)
- **Identifikasi Hari Otomatis & Override Manual**:
  - Secara default, jadwal mengikuti hari kalender (Senin/Kamis = *Upper Body*, Selasa/Jumat = *Lower Body*, Rabu = *Rest Day*, Sabtu/Minggu = *Light Activity*).
  - Tersedia pemilih kompak **"Latihan hari ini"** (*Sesuai jadwal*, *Upper*, *Lower*) di bagian atas layar pada semua tipe hari—termasuk hari *Rest* dan *Light*—sehingga pengguna dapat menambal sesi latihan yang terlewat di hari lain.
  - Saat override aktif, badge header (`TopHeader`), warna aksen navigasi (`BottomNavBar`), daftar gerakan, saran *Progressive Overload*, dan label sesi yang disimpan otomatis mengikuti tipe latihan efektif, disertai indikator `"Menggantikan jadwal hari ini"`.
- **Pencatatan Set & Auto-Save Draf**:
  - Setiap gerakan menampilkan kartu yang dapat diperluas (*expandable*), target set × repetisi/durasi, tombol *Rest Timer* per gerakan, dan saran beban dari `ProgressiveOverloadCalculator`.
  - Input beban (kg), repetisi/detik, dan status centang (`isDone`) otomatis tersimpan sebagai draf per kombinasi `(tanggal, tipeLatihan)` sehingga berpindah antara *Upper* dan *Lower* di hari yang sama tidak saling menimpa, dan draf dari tanggal sebelumnya otomatis dibersihkan saat pergantian hari.
  - Tombol **"Finish Workout & Save"** memvalidasi minimal ada 1 set selesai/terisi, menyimpan sesi ke Room Database, menghapus draf khusus tipe yang disimpan, dan menghitung ulang saran overload.

### 2. Tab Schedule (Peta Program & Catatan Latihan)
- **Weekly Map**: Ringkasan visual 7 hari (*Mon–Sun*) dengan kode warna aksen (`UP` oranye, `LOW` biru, `OFF` / `LT` abu-abu).
- **Tabel Rincian Gerakan**: Daftar lengkap 6 gerakan *Upper Body* dan 6 gerakan *Lower Body* beserta kelompok otot dan target set × repetisi.
- **Notes**: Ringkasan aturan istirahat antar set, intensitas mendekati *failure*, dan prinsip *progressive overload*.

### 3. Tab Progress & Halaman Volume Stats
- **Kartu Ringkasan Atas**: Menampilkan jumlah sesi 7 hari terakhir (`SESSIONS (7D)`), berat badan terbaru (`LATEST WEIGHT`), dan total sesi tersimpan (`TOTAL SESSIONS`).
- **Kartu Cepat Pengingat & Statistik**:
  - Akses cepat menuju dialog pengaturan pengingat latihan & pengingat timbang mingguan.
  - Akses menuju halaman **Volume Angkatan (30 Hari)** (`StatsScreen`) yang menampilkan grafik tren volume angkatan harian ($\text{beban} \times \text{repetisi}$), total volume, rata-rata per sesi, dan sesi tertinggi.
- **Pelacakan Berat Badan (`Body Weight`)**:
  - Input berat badan harian (kg), status pengingat kapan terakhir menimbang, serta edit target berat awal dan tujuan (default `38 kg` → `50 kg`).
  - Grafik garis tren berat badan (`WeightTrendChart`) dan blok **Analisis Mingguan** (`WeeklyTrendAnalysisBlock`).
- **Pengukuran Tubuh (`Body Measurement`)**:
  - Pencatatan lebar bahu (*shoulder*, cm) dan lingkar pinggang (*waist*, cm) beserta kalkulasi rasio V-taper ($\text{bahu} / \text{pinggang}$).
- **Riwayat Latihan (`Workout History`)**:
  - Menampilkan 15 sesi terbaru secara default dengan tombol **"Tampilkan lebih banyak"** (+15 sesi per ketukan).
  - Rincian set per gerakan diparsing secara *lazy* hanya ketika baris sesi diperluas, serta mendukung edit isi set sesi maupun hapus sesi.
- **Cadangkan & Pulihkan Data (`Backup & Restore`)**:
  - Ekspor dan impor seluruh data aplikasi melalui Storage Access Framework (`CreateDocument` / `OpenDocument`) dalam format `.json`.

---

## Program Latihan Mingguan (Upper / Lower Split)

### Peta Jadwal Mingguan

| Hari | Kode | Tipe Sesi | Keterangan |
| --- | --- | --- | --- |
| Senin | `UPPER` | Upper Body | Latihan tubuh bagian atas (6 gerakan) |
| Selasa | `LOWER` | Lower Body | Latihan tubuh bagian bawah & core (6 gerakan) |
| Rabu | `REST` | Rest Day | Istirahat penuh (bisa dioverride jika menambal sesi) |
| Kamis | `UPPER` | Upper Body | Latihan tubuh bagian atas (6 gerakan) |
| Jumat | `LOWER` | Lower Body | Latihan tubuh bagian bawah & core (6 gerakan) |
| Sabtu – Minggu | `LIGHT` | Light Activity | Jalan santai / peregangan ringan (bisa dioverride) |

### Daftar Gerakan Upper Body (Senin & Kamis)

| No | Gerakan | Kelompok Otot | Target | Istirahat | Tipe Input |
| --- | --- | --- | --- | --- | --- |
| 1 | Pull-up / Lat Pulldown | Lats, Upper Back | `4 x 8-10` | 150 dtk | `BODYWEIGHT_REPS` |
| 2 | Overhead Press | Shoulders | `4 x 8-10` | 150 dtk | `WEIGHT_REPS` |
| 3 | DB/Barbell Row | Back | `4 x 8-10` | 150 dtk | `WEIGHT_REPS` |
| 4 | Incline Bench Press | Upper Chest | `4 x 8-10` | 150 dtk | `WEIGHT_REPS` |
| 5 | Lateral Raise | Side Delts | `3 x 12-15` | 60 dtk | `WEIGHT_REPS` |
| 6 | Curl + Tricep Pushdown (superset) | Arms | `3 x 12` | 60 dtk | `WEIGHT_REPS` |

### Daftar Gerakan Lower Body (Selasa & Jumat)

| No | Gerakan | Kelompok Otot | Target | Istirahat | Tipe Input |
| --- | --- | --- | --- | --- | --- |
| 1 | Squat | Quads, Glutes | `4 x 6-8` | 150 dtk | `WEIGHT_REPS` |
| 2 | Romanian Deadlift | Hamstrings | `4 x 8-10` | 150 dtk | `WEIGHT_REPS` |
| 3 | Leg Press / Lunges | Quads | `3 x 10` | 90 dtk | `WEIGHT_REPS` |
| 4 | Leg Curl (machine) | Hamstrings | `3 x 12` | 60 dtk | `WEIGHT_REPS` |
| 5 | Calf Raise | Calves | `3 x 15` | 45 dtk | `WEIGHT_REPS` |
| 6 | Core (Plank/Leg Raise) | Core | `3 x 30-45 sec` | 45 dtk | `DURATION_ONLY` |

---

## Cara Kerja Progressive Overload & Analisis Berat Badan

### 1. Progressive Overload (`ProgressiveOverloadCalculator`)
Kalkulator mengambil hingga 6 sesi terakhir yang memuat gerakan terkait, memvalidasi set yang terisi, dan membandingkan **2 sesi paling baru**:
- **Syarat Minimal**: Membutuhkan minimal **2 sesi** tercatat untuk memberi rekomendasi (`Confidence`: `LOW` jika `< 2` sesi, `MEDIUM` untuk `2–3` sesi, `HIGH` untuk `>= 4` sesi).
- **Aturan Kenaikan per Tipe Input**:
  - **`WEIGHT_REPS`** (atau `BODYWEIGHT_REPS` dengan beban tambahan `>= 0.25 kg`): Jika **2 sesi berturut-turut** seluruh set valid mencapai batas atas repetisi (*top rep*, mis. `10` pada `8-10`) dengan beban rata-rata serupa (selisih `<= 1.0 kg`), sistem menyarankan kenaikan beban dibulatkan ke kelipatan `0.5 kg`:
    - Beban `< 10 kg` → `+0.5 kg`
    - Beban `10 – < 30 kg` → `+1.0 kg`
    - Beban `30 – < 60 kg` → `+2.0 kg`
    - Beban `>= 60 kg` → `+2.5 kg`
  - **`BODYWEIGHT_REPS` (murni tanpa beban tambahan)**: Jika 2 sesi berturut-turut menyentuh *top rep*, disarankan menambah beban ekstra `1–2 kg` (atau variasi lebih sulit).
  - **`DURATION_ONLY` (mis. Plank)**: Jika 2 sesi berturut-turut mencapai durasi target atas (mis. `45 detik`), disarankan naik `+5 detik` pada sesi berikutnya.
  - Jika baru 1 sesi terakhir yang mencapai *top rep*, sistem menyarankan mengulang 1 sesi lagi pada beban/durasi yang sama sebelum menaikkan target.
- **Proyeksi ~1 Bulan**: Jika terdapat minimal 3 sesi dan kenaikan beban total `> 0.4 kg`, dihitung laju mingguan untuk menampilkan perkiraan beban sekitar 4 minggu ke depan.

### 2. Analisis Tren Berat Badan (`WeightTrendAnalyzer`)
- **Pengelompokan Mingguan**: Catatan berat badan dikelompokkan berdasarkan **minggu kalender (mulai Senin)** dan dihitung rata-rata tiap minggu.
- **Laju Perubahan per Minggu**: Mengambil hingga 4 minggu terakhir yang memiliki data, lalu menghitung laju antar minggu berurutan sebagai $\frac{\Delta\text{rata-rata kg}}{\text{jumlah minggu kalender}}$ menggunakan `ChronoUnit.WEEKS` (minimal `1` minggu). Dengan demikian, jeda beberapa minggu tanpa data penimbangan tidak dianggap sebagai lonjakan 1 minggu.
- **Kategori Status & Saran**:

| Status | Kriteria Laju Mingguan | Interpretasi & Saran |
| --- | --- | --- |
| `NOT_ENOUGH_DATA` | `< 2` minggu berisi data | Butuh minimal 2 minggu data penimbangan |
| `REACHED_TARGET` | Rata-rata minggu terakhir `>= targetWeightKg` | Target tercapai; pertahankan komposisi tubuh |
| `TOO_SLOW` | `< 0.1 kg/minggu` (termasuk tren turun) | Jika turun (`< 0`), saran tambah `±200–300 kkal/hari`; jika naik lambat, saran tambah `±150–200 kkal/hari` |
| `ON_TRACK` | `0.1` s.d. `0.35 kg/minggu` | Laju ideal *lean bulking*; pertahankan pola makan & latihan |
| `TOO_FAST` | `> 0.35 kg/minggu` | Kenaikan terlalu cepat (berisiko lemak berlebih); kurangi porsi sedikit |

- Jika target belum tercapai dan laju mingguan positif (`> 0.01 kg/minggu`), sistem juga menghitung estimasi jumlah minggu menuju target berat badan.

---

## Sistem Pengingat & Rest Timer

Semua alarm terjadwal dikelola melalui `AlarmManager` dengan `setExactAndAllowWhileIdle` (jika izin alarm presisi tersedia pada Android 12+, atau `setAndAllowWhileIdle` sebagai fallback) dan dijadwalkan ulang otomatis saat perangkat selesai *booting* oleh `BootReceiver`.

| Fitur | Receiver & Channel | Jadwal / ID Default | Perilaku |
| --- | --- | --- | --- |
| **Pengingat Latihan** | `WorkoutAlarmReceiver`<br>Channel: `workout_reminder_channel` | Senin, Selasa, Kamis, Jumat pukul `16:00`<br>Request codes: `101, 102, 104, 105` | Mengikuti hari jadwal kalender; menjadwalkan ulang +1 minggu tiap kali berbunyi |
| **Pengingat Timbang Mingguan** | `WeighInAlarmReceiver`<br>Channel: `weigh_in_channel` | Minggu pukul `07:00` (hari & jam dapat diubah)<br>Request code: `7001`, Notif ID: `8001` | 1x seminggu sesuai hari & jam pilihan pengguna; otomatis menjadwalkan minggu berikutnya |
| **Rest Timer per Gerakan** | `RestTimerAlarmReceiver`<br>Channel: `rest_timer_channel` | Durasi per gerakan (`45–150 dtk`)<br>Request code: `1000 + index`, Notif ID: `5000 + index` | Hitung mundur berbasis `endTimestampMillis` (`RestTimerStore`) sehingga tetap akurat saat aplikasi di background; otomatis dibersihkan saat mengganti override tipe latihan |

Dialog pengaturan (`ReminderDialog`) juga memantau izin `SCHEDULE_EXACT_ALARM` serta status *Battery Optimization* dan menyediakan pintasan ke pengaturan sistem bila belum diaktifkan.

---

## Arsitektur Kode, Room Database, & Backup JSON

### Diagram Arsitektur

```text
┌──────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI (M3)                       │
│   TopHeader  │  TodayScreen  │  ScheduleScreen  │  Progress / Stats  │
│              │  (Selector +  │                  │  (Charts, History, │
│              │  Rest Timer)  │                  │   Backup, Dialogs) │
└───────────────────────────────┬──────────────────────────────────────┘
                                │ StateFlow / Event Callbacks
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         MainViewModel (MVVM)                         │
│  • Koordinasi tanggal aktif, override harian, & draf per (tgl, tipe) │
│  • Hitung mundur Rest Timer & sinkronisasi onResume / rollover       │
│  • Kalkulasi Progressive Overload & analisis tren berat mingguan     │
└───┬───────────────────────────┬──────────────────────────┬───────────┘
    │                           │                          │
    ▼                           ▼                          ▼
┌─────────────────┐   ┌────────────────────┐   ┌───────────────────────┐
│WorkoutRepository│   │ SharedPreferences  │   │   Alarm & Receiver    │
│  (Room DB v2)   │   │  • Draf latihan    │   │ • Pengingat latihan   │
│ • WorkoutSession│   │  • Override harian │   │ • Timbang mingguan    │
│ • WeightLog     │   │  • Rest timer ts   │   │ • Alarm rest timer    │
│ • BodyMeasure-  │   │  • Target berat    │   │ • Reschedule on boot  │
│   ment          │   │  • Jadwal alarm    │   └───────────────────────┘
└────────┬────────┘   └────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────────────┐
│                        BackupManager (JSON v2)                       │
│   Ekspor & Impor sesi latihan, berat badan, ukuran tubuh, & alarm    │
└──────────────────────────────────────────────────────────────────────┘
```

### Skema Room Database (`vtaper_grind_database`, Versi 2)

Database menggunakan Room (`AppDatabase`) dengan migrasi `MIGRATION_1_2` untuk menambahkan tabel `body_measurements` tanpa menghapus data pengguna versi 1.

| Tabel | Entitas | Kolom Utama |
| --- | --- | --- |
| `workout_sessions` | `WorkoutSession` | `id`, `date` (`yyyy-MM-dd`), `dayType` (`UPPER`/`LOWER`), `dayLabel`, `timestamp`, `exercisesJson` |
| `weight_logs` | `WeightLog` | `id`, `date` (`yyyy-MM-dd`), `kg`, `timestamp` |
| `body_measurements` | `BodyMeasurement` | `id`, `date` (`yyyy-MM-dd`), `shoulderCm`, `waistCm`, `timestamp` |

### Penyimpanan `SharedPreferences`

| Store / Manager | Nama Preferences | Fungsi |
| --- | --- | --- |
| `DraftWorkoutStore` | `draft_workout_prefs` | Menyimpan draf set latihan per `(date, dayType)` (`UPPER` / `LOWER`) dan membuang draf tanggal lama |
| `WorkoutOverrideStore` | `workout_override_prefs` | Menyimpan `override_date` & `override_type` (`UPPER` / `LOWER`); otomatis batal saat tanggal berganti |
| `RestTimerStore` | `rest_timer_prefs` | Menyimpan `rest_end_ts_<idx>` dan `rest_total_sec_<idx>` untuk sinkronisasi timer setelah *background* |
| `WeightGoalManager` | `weight_goals_prefs` | Menyimpan `start_weight_kg` (default `38f`) dan `target_weight_kg` (default `50f`) |
| `ReminderManager` | `workout_reminder_prefs` | Menyimpan status aktif, hari, jam, dan menit pengingat latihan serta pengingat timbang mingguan |

### Format Backup JSON (`BackupManager`, Versi 2)

`BackupManager` mendukung ekspor versi `2` dan impor yang kompatibel mundur dengan backup versi `1`. Struktur JSON yang dihasilkan:

```json
{
  "backupVersion": 2,
  "exportedAt": 1728550000000,
  "sessions": [
    {
      "id": 1,
      "date": "2026-10-10",
      "dayType": "UPPER",
      "dayLabel": "Upper Body",
      "timestamp": 1728550000000,
      "exercisesJson": "[...]"
    }
  ],
  "weightLogs": [
    { "id": 1, "date": "2026-10-10", "kg": 41.5, "timestamp": 1728550000000 }
  ],
  "bodyMeasurements": [
    { "id": 1, "date": "2026-10-10", "shoulderCm": 112.5, "waistCm": 74.0, "timestamp": 1728550000000 }
  ],
  "weightGoals": { "start": 38.0, "target": 50.0 },
  "reminderSettings": { "hour": 16, "minute": 0, "enabled": true },
  "weighInReminderSettings": { "enabled": true, "day": 7, "hour": 7, "minute": 0 }
}
```

Saat proses *restore* dikonfirmasi, database Room diperbarui secara atomik (`replaceAllSessions`, `replaceAllWeights`, `replaceAllMeasurements`), seluruh target berat dan jadwal alarm diperbarui, serta draf/override/timer hari berjalan dibersihkan lalu dimuat ulang.

---

## Build, Signing, & Pengujian

### Prasyarat Lingkungan
- **JDK**: Java 11+
- **Android Studio / SDK**: Compile SDK 36 (`minorApiLevel = 1`), Gradle Kotlin DSL

### Menjalankan Pengujian (Unit & Robolectric)
Proyek ini menggunakan JUnit 4, Robolectric, dan Roborazzi untuk pengujian logika dan komponen Android di JVM lokal tanpa emulator:

```bash
./gradlew :app:testDebugUnitTest
```

### Build APK Debug & Release
- **Debug APK**:
  ```bash
  ./gradlew :app:assembleDebug
  ```
- **Release APK** (dengan R8 minification & resource shrinking aktif):
  ```bash
  ./gradlew :app:assembleRelease
  ```

#### Catatan Penandatanganan (*Signing*)
1. Jika terdapat `keystore.properties` di root proyek, build `release` otomatis memakai keystore rilis tersebut.
2. Jika `keystore.properties` tidak tersedia, build `release` tetap dapat dikompilasi karena jatuh kembali (*fallback*) ke `debugConfig` (bila `debug.keystore` ada di root proyek) atau ke key debug bawaan Android (`signingConfigs.getByName("debug")`) khusus untuk pengujian lokal (**jangan dipublikasikan**).
3. Panduan lengkap membuat keystore baru, menyiapkan `keystore.properties`, membangun APK via Android Studio, dan instalasi via USB Debugging tersedia di [`BUILD.md`](BUILD.md).
