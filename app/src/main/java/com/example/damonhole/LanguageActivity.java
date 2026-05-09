package com.example.damonhole;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.color.DynamicColors;

public class LanguageActivity extends AppCompatActivity {

    private ImageView checkEnUk, checkZhCn, checkMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        checkEnUk = findViewById(R.id.checkEnUk);
        checkZhCn = findViewById(R.id.checkZhCn);
        checkMs = findViewById(R.id.checkMs);

        updateCheckmarks();

        findViewById(R.id.langEnUk).setOnClickListener(v -> setLocale("en-GB"));
        findViewById(R.id.langZhCn).setOnClickListener(v -> setLocale("zh-CN"));
        findViewById(R.id.langMs).setOnClickListener(v -> setLocale("ms"));
    }

    private void updateCheckmarks() {
        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        String currentLang = "en-GB"; // Default
        if (!currentLocales.isEmpty()) {
            String tag = currentLocales.get(0).toLanguageTag();
            if (tag != null) {
                currentLang = tag;
            }
        }

        checkEnUk.setVisibility(View.GONE);
        checkZhCn.setVisibility(View.GONE);
        checkMs.setVisibility(View.GONE);

        if (currentLang.startsWith("en")) {
            checkEnUk.setVisibility(View.VISIBLE);
        } else if (currentLang.startsWith("zh")) {
            checkZhCn.setVisibility(View.VISIBLE);
        } else if (currentLang.startsWith("ms")) {
            checkMs.setVisibility(View.VISIBLE);
        }
    }

    private void setLocale(String languageTag) {
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageTag);
        AppCompatDelegate.setApplicationLocales(appLocale);
    }
}
