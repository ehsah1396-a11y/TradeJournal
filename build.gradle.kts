android {
    namespace = "com.example.tradejournal" // این رو دست نزن، همون قبلیه
    compileSdk = 34 // یا هر عددی که هست

    defaultConfig {
        // ...
    }

    // این بخش رو اضافه کن
    signingConfigs {
        create("release") {
            storeFile = file("release.keystore") // مسیر فایل کیستور توی ماژول app
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            // این خط رو اضافه کن تا از تنظیمات بالا استفاده کنه
            signingConfig = signingConfigs.getByName("release")
            
            isMinifyEnabled = false // برای اینکه کدت موقع بیلد خراب نشه، فعلاً false بذار
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    // ...
}
