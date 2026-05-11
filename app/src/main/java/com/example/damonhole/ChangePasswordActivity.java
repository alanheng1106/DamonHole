package com.example.damonhole;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputLayout tilCurrentPassword, tilNewPassword, tilConfirmPassword;
    private MaterialButton btnUpdatePassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();

        tilCurrentPassword = findViewById(R.id.tilCurrentPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);

        findViewById(R.id.btnBack).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        btnUpdatePassword.setOnClickListener(v -> updatePassword());
    }

    private void clearErrors() {
        tilCurrentPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private String text(TextInputLayout til) {
        return til.getEditText() != null ? til.getEditText().getText().toString().trim() : "";
    }

    private void updatePassword() {
        clearErrors();
        String currentPassword = text(tilCurrentPassword);
        String newPassword = text(tilNewPassword);
        String confirmPassword = text(tilConfirmPassword);

        boolean valid = true;
        if (TextUtils.isEmpty(currentPassword)) {
            tilCurrentPassword.setError(getString(R.string.password_empty));
            valid = false;
        }
        if (TextUtils.isEmpty(newPassword)) {
            tilNewPassword.setError(getString(R.string.password_empty));
            valid = false;
        } else if (newPassword.length() < 6) {
            tilNewPassword.setError(getString(R.string.password_length_error));
            valid = false;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.password_empty));
            valid = false;
        } else if (!newPassword.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.password_mismatch));
            valid = false;
        }
        if (!valid) return;

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, R.string.user_not_signed_in, Toast.LENGTH_SHORT).show();
            return;
        }

        btnUpdatePassword.setEnabled(false);

        // Re-authenticate, then update password
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(this, getString(R.string.change_password_success), Toast.LENGTH_LONG).show();
                        // Sign out so Firebase clears the old session token cleanly,
                        // then redirect to login with the new password.
                        mAuth.signOut();
                        android.content.Intent intent = new android.content.Intent(this, WelcomeActivity.class);
                        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String error = updateTask.getException() != null
                                ? updateTask.getException().getMessage() : "Unknown error";
                        tilNewPassword.setError(getString(R.string.change_password_failed, error));
                        btnUpdatePassword.setEnabled(true);
                    }
                });
            } else {
                // Wrong current password → show error on that specific field
                if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                    tilCurrentPassword.setError(getString(R.string.incorrect_password));
                } else {
                    String error = task.getException() != null
                            ? task.getException().getMessage() : getString(R.string.auth_failed, "Unknown");
                    tilCurrentPassword.setError(error);
                }
                btnUpdatePassword.setEnabled(true);
            }
        });
    }
}
