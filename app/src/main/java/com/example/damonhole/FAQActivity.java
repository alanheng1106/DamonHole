package com.example.damonhole;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FAQActivity extends AppCompatActivity {

    private MaterialButton btnBack;
    private MaterialButton btnLanguage;
    private TextInputEditText etSearchHelp;
    private ChipGroup cgFaqCategories;
    private LinearLayout llFaqContainer;

    private List<FAQ> allFaqs = new ArrayList<>();
    private String currentCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        // Initialize category from string resource to match default chip selection
        currentCategory = getString(R.string.faq_cat_playback);

        initializeFaqData();
        initializeViews();
        setupListeners();
        displayFaqs(currentCategory, "");
    }

    private void initializeFaqData() {
        // Playback FAQs
        allFaqs.add(new FAQ(getString(R.string.faq_cat_playback), getString(R.string.faq_q_bg_play), getString(R.string.faq_a_bg_play)));
        allFaqs.add(new FAQ(getString(R.string.faq_cat_playback), getString(R.string.faq_q_screen_off), getString(R.string.faq_a_screen_off)));
        allFaqs.add(new FAQ(getString(R.string.faq_cat_playback), getString(R.string.faq_q_stop_unexpectedly), getString(R.string.faq_a_stop_unexpectedly)));

        // Content FAQs
        allFaqs.add(new FAQ(getString(R.string.faq_cat_content), getString(R.string.faq_q_is_free), getString(R.string.faq_a_is_free)));
        allFaqs.add(new FAQ(getString(R.string.faq_cat_content), getString(R.string.faq_q_source), getString(R.string.faq_a_source)));
        allFaqs.add(new FAQ(getString(R.string.faq_cat_content), getString(R.string.faq_q_download), getString(R.string.faq_a_download)));

        // Playlists FAQs
        allFaqs.add(new FAQ(getString(R.string.faq_cat_playlists), getString(R.string.faq_q_create_playlist), getString(R.string.faq_a_create_playlist)));
        allFaqs.add(new FAQ(getString(R.string.faq_cat_playlists), getString(R.string.faq_q_add_to_playlist), getString(R.string.faq_a_add_to_playlist)));

        // General FAQs
        allFaqs.add(new FAQ(getString(R.string.faq_cat_general), getString(R.string.faq_q_change_theme), getString(R.string.faq_a_change_theme)));
        allFaqs.add(new FAQ(getString(R.string.faq_cat_general), getString(R.string.faq_q_audio_quality), getString(R.string.faq_a_audio_quality)));
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnLanguage = findViewById(R.id.btnLanguage);
        etSearchHelp = findViewById(R.id.etSearchHelp);
        cgFaqCategories = findViewById(R.id.cgFaqCategories);
        llFaqContainer = findViewById(R.id.llFaqContainer);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        btnLanguage.setOnClickListener(v -> {
            startActivity(new Intent(this, LanguageActivity.class));
        });

        etSearchHelp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                displayFaqs(currentCategory, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        cgFaqCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipPlayback) currentCategory = getString(R.string.faq_cat_playback);
                else if (checkedId == R.id.chipContent) currentCategory = getString(R.string.faq_cat_content);
                else if (checkedId == R.id.chipPlaylists) currentCategory = getString(R.string.faq_cat_playlists);
                else if (checkedId == R.id.chipGeneral) currentCategory = getString(R.string.faq_cat_general);
                
                String query = (etSearchHelp.getText() != null) ? etSearchHelp.getText().toString() : "";
                displayFaqs(currentCategory, query);
            }
        });
    }

    private void displayFaqs(String category, String query) {
        llFaqContainer.removeAllViews();
        
        List<FAQ> filtered = allFaqs.stream()
                .filter(f -> f.category.equals(category))
                .filter(f -> f.question.toLowerCase().contains(query.toLowerCase()) || 
                            f.answer.toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());

        for (FAQ faq : filtered) {
            addFaqView(faq);
        }
    }

    private void addFaqView(FAQ faq) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_faq, llFaqContainer, false);
        
        TextView tvQuestion = view.findViewById(R.id.tvQuestion);
        TextView tvAnswer = view.findViewById(R.id.tvAnswer);
        ImageView ivToggle = view.findViewById(R.id.ivToggle);
        LinearLayout llHeader = view.findViewById(R.id.llHeader);

        tvQuestion.setText(faq.question);
        tvAnswer.setText(faq.answer);
        
        // Default to collapsed
        tvAnswer.setVisibility(View.GONE);
        ivToggle.setImageResource(R.drawable.ic_add);

        llHeader.setOnClickListener(v -> {
            if (tvAnswer.getVisibility() == View.VISIBLE) {
                tvAnswer.setVisibility(View.GONE);
                ivToggle.setImageResource(R.drawable.ic_add);
            } else {
                tvAnswer.setVisibility(View.VISIBLE);
                ivToggle.setImageResource(R.drawable.ic_remove);
            }
        });

        llFaqContainer.addView(view);
        
        // Add a divider
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
        llFaqContainer.addView(divider);
    }

    private static class FAQ {
        String category;
        String question;
        String answer;

        FAQ(String category, String question, String answer) {
            this.category = category;
            this.question = question;
            this.answer = answer;
        }
    }
}
