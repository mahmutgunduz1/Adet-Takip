# Adet Takip Uygulaması

Bu uygulama, kadınların adet döngülerini takip etmelerine yardımcı olan bir Android uygulamasıdır.

## Özellikler

- ✅ Kullanıcı kayıt ve giriş sistemi
- ✅ Adet tarihlerini kaydetme ve takip etme
- ✅ Döngü hesaplamaları (SCS saati, güvenli günler)
- ✅ Firebase Authentication ve Firestore entegrasyonu
- ✅ Offline veri desteği
- ✅ Türkçe dil desteği

## Teknik Özellikler

- **Dil**: Kotlin
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Mimari**: MVVM
- **Veritabanı**: Firebase Firestore
- **Kimlik Doğrulama**: Firebase Authentication

## Kurulum

### 1. Projeyi Klonlayın
```bash
git clone <repository-url>
cd Scs
```

### 2. Firebase Kurulumu

#### Firebase Console'da Proje Oluşturma:
1. [Firebase Console](https://console.firebase.google.com/)'a gidin
2. "Add project" butonuna tıklayın
3. Proje adını girin (örn: "adet-takip")
4. Google Analytics'i etkinleştirin (isteğe bağlı)

#### Android Uygulaması Ekleme:
1. Firebase projesinde "Add app" > "Android" seçin
2. Package name: `com.mahmutgunduz.adettakip`
3. App nickname: "Adet Takip"
4. SHA-1 sertifikasını ekleyin (debug için)

#### google-services.json Dosyası:
1. Firebase'den `google-services.json` dosyasını indirin
2. Dosyayı `app/` klasörüne kopyalayın

### 3. Firebase Authentication Kurulumu

1. Firebase Console'da "Authentication" > "Get started"
2. "Sign-in method" sekmesinde "Email/Password"ı etkinleştirin

### 4. Firestore Database Kurulumu

1. Firebase Console'da "Firestore Database" > "Create database"
2. "Start in test mode" seçin (geliştirme için)
3. Lokasyon seçin (Europe-west3 önerilir)

#### Firestore Security Rules:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Kullanıcılar sadece kendi verilerine erişebilir
    match /periodDates/{document} {
      allow read, write: if request.auth != null && 
                         request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && 
                    request.auth.uid == request.resource.data.userId;
    }
    
    // Kullanıcı profil bilgileri
    match /users/{userId} {
      allow read, write: if request.auth != null && 
                         request.auth.uid == userId;
    }
    
    // Diğer tüm belgeler için erişim engellendi
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

### 5. Projeyi Build Etme

```bash
# Windows için
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
./gradlew assembleDebug

# macOS/Linux için
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
```

## Veri Yapısı

### periodDates Collection
```json
{
  "userId": "string",
  "date": "timestamp",
  "timestamp": "timestamp",
  "hour": "number",
  "minute": "number"
}
```

### users Collection
```json
{
  "fullName": "string",
  "email": "string",
  "phoneNumber": "string"
}
```

## Sorun Giderme

### Yaygın Hatalar ve Çözümleri:

1. **"Veri yüklenirken hata: PERMISSION_DENIED"**
   - Firebase Security Rules'ı kontrol edin
   - Kullanıcının giriş yapmış olduğundan emin olun

2. **"İnternet bağlantınızı kontrol edin"**
   - İnternet bağlantısını kontrol edin
   - Firebase projesinin aktif olduğundan emin olun

3. **"R sınıfı bulunamıyor"**
   - `./gradlew clean` komutunu çalıştırın
   - Resource dosyalarında syntax hatası olup olmadığını kontrol edin

4. **"JAVA_HOME is not set"**
   - Android Studio'nun JDK yolunu JAVA_HOME olarak ayarlayın
   - Windows: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`

### Debug APK Konumu:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Katkıda Bulunma

1. Fork yapın
2. Feature branch oluşturun (`git checkout -b feature/AmazingFeature`)
3. Değişikliklerinizi commit edin (`git commit -m 'Add some AmazingFeature'`)
4. Branch'inizi push edin (`git push origin feature/AmazingFeature`)
5. Pull Request oluşturun

## Lisans

Bu proje MIT lisansı altında lisanslanmıştır.

## İletişim

Proje Sahibi: Mahmut Gündüz
Email: [email]

Proje Linki: [repository-url]