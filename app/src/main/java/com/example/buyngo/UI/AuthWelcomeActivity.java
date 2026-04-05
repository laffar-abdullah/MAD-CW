ackage com.example.buyngo.UI;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.buyngo.R;

public class AuthWelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_welcome);

        // Customer to Customer Login
        findViewById(R.id.btnCustomerLogin).setOnClickListener(v ->
                startActivity(new Intent(this, CusLoginActivity.class)));

        // Admin to Admin Login
        findViewById(R.id.btnAdminLogin).setOnClickListener(v ->
                startActivity(new Intent(this, AdmLoginActivity.class)));

        // Rider to Rider Login
        findViewById(R.id.btnRiderLogin).setOnClickListener(v ->
                startActivity(new Intent(this, RidLoginActivity.class)));
    }
}

