package com.example.damonhole;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText etFirstName, etLastName, etEmail, etMobile;
    private ImageView ivProfileLarge;
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        ivProfileLarge = findViewById(R.id.ivProfileLarge);
        btnSave = findViewById(R.id.btnSave);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadUserProfile();

        btnSave.setOnClickListener(v -> saveUserProfile());
    }

    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        etEmail.setText(mAuth.getCurrentUser().getEmail());

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            etFirstName.setText(user.getFirstName());
                            etLastName.setText(user.getLastName());
                            etMobile.setText(user.getMobile());

                            // Optional: Still loads a profile picture if they uploaded one previously
                            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                ivProfileLarge.setPadding(0, 0, 0, 0);
                                Glide.with(this)
                                        .load(user.getProfileImageUrl())
                                        .placeholder(R.drawable.ic_music_note)
                                        .into(ivProfileLarge);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, R.string.failed_load_profile, Toast.LENGTH_SHORT).show());
    }

    private void saveUserProfile() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();

        if (firstName.isEmpty()) {
            etFirstName.setError(getString(R.string.first_name_required));
            return;
        }

        btnSave.setEnabled(false);
        Toast.makeText(this, R.string.updating_profile, Toast.LENGTH_SHORT).show();

        updateFirestoreProfile(uid, firstName, lastName, mobile);
    }

    private void updateFirestoreProfile(String uid, String firstName, String lastName, String mobile) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("mobile", mobile);
        updates.put("name", firstName + (lastName.isEmpty() ? "" : " " + lastName));

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.update_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
    }
}