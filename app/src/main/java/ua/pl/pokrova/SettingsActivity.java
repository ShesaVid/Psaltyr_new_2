package ua.pl.pokrova;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import ua.pl.pokrova.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();

        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Повертаємось на попередній екран
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

}

