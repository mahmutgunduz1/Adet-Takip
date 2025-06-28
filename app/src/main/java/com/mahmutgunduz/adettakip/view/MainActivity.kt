package com.mahmutgunduz.adettakip.view

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.FirebaseApp
import com.mahmutgunduz.adettakip.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Firebase'i başlat
        FirebaseApp.initializeApp(this)
        
        // Status bar ve navigation bar ayarları

        
        setContentView(R.layout.activity_main)
    }
    

}