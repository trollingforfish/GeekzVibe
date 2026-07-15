# Panduan Setup Fitur Premium & Firebase (GeekzVibe)

Aplikasi **GeekzVibe** saat ini telah dilengkapi dengan integrasi lengkap fitur Premium, Authentication, integrasi API Key Gemini berbasis SharedPreferences, dan sistem simulasi In-App Purchase yang terhubung langsung dengan Firebase Auth dan Cloud Firestore.

Untuk mengaktifkan integrasi riil dalam skala produksi, silakan ikuti panduan lengkap langkah-demi-langkah di bawah ini.

---

## 1. Konfigurasi Firebase Console

### A. Membuat Proyek Firebase
1. Buka [Firebase Console](https://console.firebase.google.com/).
2. Klik **Add project** (Tambah Proyek), masukkan nama proyek Anda (misal: `GeekzVibe-Music`), lalu klik **Continue**.
3. Aktifkan atau nonaktifkan Google Analytics sesuai kebutuhan, lalu klik **Create project**.

### B. Registrasi Aplikasi Android
1. Di halaman ikhtisar proyek Firebase, klik ikon **Android** untuk menambahkan aplikasi baru.
2. Masukkan **Android package name**: `com.example` (sesuaikan dengan nilai `namespace` di file `app/build.gradle.kts` Anda).
3. Masukkan nama panggilan aplikasi (misal: `GeekzVibe`).
4. Masukkan SHA-1 certificate fingerprint Anda (diperlukan untuk fitur Firebase Auth tingkat lanjut seperti Google Sign-In, bisa diperoleh via perintah gradle `:app:signingReport`).
5. Klik **Register app**.
6. Unduh file `google-services.json` yang diberikan, lalu letakkan file tersebut di dalam direktori proyek Anda pada folder **`/app/`**.

---

## 2. Mengaktifkan Layanan Firebase

### A. Firebase Authentication (Email/Password)
1. Di menu sidebar Firebase Console, navigasikan ke **Build > Authentication**.
2. Klik tombol **Get Started** jika baru pertama kali.
3. Buka tab **Sign-in method**, pilih **Email/Password**.
4. Aktifkan saklar **Email/Password**, lalu klik **Save** (Simpan).

### B. Cloud Firestore (Database Status Premium)
1. Di menu sidebar, navigasikan ke **Build > Firestore Database**.
2. Klik tombol **Create database**.
3. Pilih lokasi database terdekat, lalu klik **Next**.
4. Pilih **Start in test mode** untuk keperluan pengembangan awal, lalu klik **Create**.
5. Di tab **Rules**, pastikan aturannya mengizinkan akses tulis dan baca bagi pengguna yang terautentikasi:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

---

## 3. Implementasi Firebase Cloud Functions (Backend)

Cloud Functions digunakan untuk memvalidasi transaksi pembelian dari Google Play Billing secara secure (server-side) dan memperbarui status premium pengguna di Firestore.

### A. Setup Lingkungan Cloud Functions
1. Di terminal komputer lokal Anda, instal Firebase CLI secara global:
   ```bash
   npm install -g firebase-tools
   ```
2. Lakukan login ke akun Firebase Anda:
   ```bash
   firebase login
   ```
3. Inisialisasi Cloud Functions di folder proyek Anda:
   ```bash
   firebase init functions
   ```
   - Pilih proyek Firebase yang telah Anda buat di langkah sebelumnya.
   - Pilih bahasa pemrograman **JavaScript** atau **TypeScript** (panduan ini menggunakan JavaScript).
   - Setujui instalasi dependensi menggunakan `npm`.

### B. Kode Backend (`functions/index.js`)
Ganti isi file `functions/index.js` dengan kode lengkap di bawah ini untuk memproses validasi transaksi Google Play Store:

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { google } = require('googleapis');

admin.initializeApp();
const db = admin.firestore();

/**
 * Cloud Function untuk memvalidasi pembelian langganan / produk digital
 * dan memperbarui status 'isPremium' di dokumen Firestore pengguna.
 */
exports.verifyPurchase = functions.https.onCall(async (data, context) => {
  // Pastikan pengguna telah terautentikasi via Firebase Auth
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'Pengguna harus masuk terlebih dahulu.'
    );
  }

  const uid = context.auth.uid;
  const { purchaseToken, subscriptionId, packageName } = data;

  if (!purchaseToken || !subscriptionId || !packageName) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Parameter pembelian tidak lengkap.'
    );
  }

  try {
    // 1. Konfigurasi Google API Auth menggunakan Service Account Anda
    // (Unduh file kunci JSON Service Account dari Google Cloud Console)
    const auth = new google.auth.GoogleAuth({
      keyFilename: './service-account-key.json',
      scopes: ['https://www.googleapis.com/auth/androidpublisher'],
    });
    const authClient = await auth.getClient();
    const playDeveloperApi = google.androidpublisher({
      version: 'v3',
      auth: authClient,
    });

    // 2. Lakukan query status langganan ke Google Play Developer API
    const response = await playDeveloperApi.subscriptions.get({
      packageName: packageName,
      subscriptionId: subscriptionId,
      token: purchaseToken,
    });

    // 3. Periksa apakah status pembayaran berhasil (0 = Aktif, 1 = Ditangguhkan, dll)
    const paymentState = response.data.paymentState;
    if (paymentState === 0) {
      // Pembelian Valid! Perbarui status pengguna menjadi premium di Firestore
      await db.collection('users').document(uid).set({
        isPremium: true,
        premiumSince: admin.firestore.FieldValue.serverTimestamp(),
        subscriptionId: subscriptionId,
        purchaseToken: purchaseToken,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });

      return { success: true, message: 'Status premium berhasil diaktifkan!' };
    } else {
      return { success: false, message: 'Pembelian tidak aktif atau pembayaran ditangguhkan.' };
    }
  } catch (error) {
    console.error('Error saat memvalidasi pembelian:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Gagal memvalidasi pembelian secara server-side: ' + error.message
    );
  }
});
```

### C. Deploy Cloud Functions
Lakukan deployment kode backend Anda ke server Firebase:
```bash
firebase deploy --only functions
```

---

## 4. Konfigurasi Google Play Billing & In-App Purchases

Untuk menghubungkan transaksi riil di dalam aplikasi:
1. Buka **Google Play Console** dan pilih aplikasi Anda.
2. Masuk ke menu **Monetize > Products > Subscriptions** atau **In-App Products**.
3. Klik **Create Subscription**, buat ID produk (misal: `premium_monthly`), atur harga, dan klik **Save & Activate**.
4. Masuk ke **Google Cloud Console**, aktifkan **Google Play Developer API**.
5. Buat **Service Account** baru di Google Cloud Console, berikan akses penuh ke proyek Google Play Developer Anda, buat file kunci privat berformat **JSON**, lalu simpan file kunci tersebut dengan nama `service-account-key.json` di dalam folder `functions/` proyek Firebase Anda sebelum melakukan deploy ulang.

---

## 5. Mode Uji Coba & Simulasi Aman

Aplikasi **GeekzVibe** menyertakan mode simulasi instan yang kuat untuk membantu Anda melakukan testing fungsionalitas visual tanpa setup Firebase riil sekalipun:
- Tombol **Upgrade ke Premium** di tab Profile/Settings akan menjalankan simulasi IAP yang realistis dengan penundaan pemrosesan `1.5` detik.
- Jika Firebase Firestore terhubung, ia akan memperbarui status secara otomatis di server. Jika `google-services.json` belum di-setup, aplikasi akan beralih ke status **Premium Lokal (Simulasi)**, mengizinkan Anda melihat dan mencoba transisi antarmuka bebas iklan, lirik, dan equalizer secara real-time.
- Anda dapat mengonfigurasi API Key Gemini Anda secara instan di menu Settings untuk menguji kecerdasan lirik tanpa merusak konfigurasi codebase utama!
