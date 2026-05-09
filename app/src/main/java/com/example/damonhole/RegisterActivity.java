package com.example.damonhole;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLoginPrompt);

        btnRegister.setOnClickListener(v -> registerUser());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        findViewById(R.id.btnLanguage).setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LanguageActivity.class));
        });
    }

    private void registerUser() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError(getString(R.string.first_name_required));
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.email_required));
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.password_required));
            return;
        }

        if (password.length() < 6) {
            etPassword.setError(getString(R.string.password_length_error));
            return;
        }

        btnRegister.setEnabled(false);
        Toast.makeText(this, R.string.creating_account, Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            saveUserToFirestore(firebaseUser.getUid(), firstName, lastName, email);
                        }
                    } else {
                        btnRegister.setEnabled(true);
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(RegisterActivity.this, getString(R.string.auth_failed, error), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String firstName, String lastName, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("firstName", firstName);
        userMap.put("lastName", lastName);
        userMap.put("name", firstName + " " + lastName);
        userMap.put("email", email);
        userMap.put("mobile", "");
        userMap.put("likedSongs", new ArrayList<>());
        userMap.put("playlists", new ArrayList<>());

        db.collection("users").document(uid).set(userMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mAuth.signOut();
                        Toast.makeText(getApplicationContext(), R.string.registration_success, Toast.LENGTH_LONG).show();
                        
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        btnRegister.setEnabled(true);
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(RegisterActivity.this, "Firestore error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
