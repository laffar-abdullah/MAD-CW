ckage com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class AdmLoginActivity extends AppCompatActivity {
    private static final String ADMIN_EMAIL = "admin@buyngo.com";
    private static final String ADMIN_PASSWORD = "admin123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adm_login);

        EditText adminEmailEditText = findViewById(R.id.adminEmailEditText);
        EditText adminPasswordEditText = findViewById(R.id.adminPasswordEditText);

        adminEmailEditText.setText(ADMIN_EMAIL);
        adminPasswordEditText.setText(ADMIN_PASSWORD);

        // Back to Welcome screen
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Login Admin Dashboard
        findViewById(R.id.adminLoginButton).setOnClickListener(v -> {
            String enteredEmail = adminEmailEditText.getText().toString().trim();
            String enteredPassword = adminPasswordEditText.getText().toString().trim();

            if (ADMIN_EMAIL.equalsIgnoreCase(enteredEmail) && ADMIN_PASSWORD.equals(enteredPassword)) {
                startActivity(new Intent(this, AdmDashboardActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

