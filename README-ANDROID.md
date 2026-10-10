# Fırsat Avcısı AI — Android WebView kabuğu

Bu Android Studio/Gradle projesi, `app/src/main/assets/index.html` içindeki prototipi Android uygulaması içinde açar.

## Bu proje ne yapar?
- Android'de kurulabilir APK oluşturmak için kaynak proje sağlar.
- İlk prototip arayüzünü WebView içinde gösterir.
- Sohbet yanıtları hâlâ örnektir; gerçek LLM veya internet ilan tarayıcısı içermez.

## APK oluşturmanın en kolay yolu (telefonla da mümkün)
1. Bu ZIP'i GitHub'a yeni bir repository olarak yükle.
2. Repository içinde **Actions** sekmesini aç.
3. `Build Android APK` iş akışını seçip **Run workflow** düğmesine bas.
4. İş tamamlanınca çalışmanın altındaki **Artifacts** bölümünden `firsat-avcisi-ai-debug-apk` dosyasını indir.
5. ZIP içinden `app-debug.apk` dosyasını çıkarıp Android telefonuna indir ve aç. Android, tarayıcıdan yükleme izni isteyebilir.

GitHub Actions kullanmak için GitHub hesabı gerekir. Bu paket şu anda imzalı mağaza sürümü değil; debug APK üretir. APK'yı yalnızca kendi cihazına kur. Bilinmeyen kaynaklardan APK yüklemek risklidir.

## Bilinen sınırlamalar
- Otomatik web araştırması, gerçek yapay zekâ ve arka plan bildirimleri henüz uygulanmadı.
- Android projesi ilk aşama WebView sarmalayıcısıdır.
- GitHub Actions iş akışı internet bağlantısı ve GitHub'ın uygun kullanım koşullarını gerektirir.
