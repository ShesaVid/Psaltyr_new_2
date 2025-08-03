package ua.pl.pokrova.view;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ua.pl.pokrova.R;
import ua.pl.pokrova.db.DatabaseHelper;
import ua.pl.pokrova.db.PsalomDto;
import ua.pl.pokrova.service.AudioService;
import ua.pl.pokrova.ui.NavFlexboxAdapter;
import ua.pl.pokrova.ui.PsalmyAdapter;

public class PsalomActivity extends AppCompatActivity implements AudioService.ServiceCallbacks {

    // --- Поля для UI та даних ---
    private Button btnPrevK, btnNextK, btnPrevKb, btnNextKb;
    private RecyclerView rvListPsalom, rvPsalom;
    private int idK, tmpKp, tmpKn;
    private DatabaseHelper databaseHelper;
    private NavFlexboxAdapter adapterNumbrs;
    private PsalmyAdapter adapterPsalmy;
    private NestedScrollView scrollPsalmy;
    private SeekBar seekBar;
    private Button playPauseBtn;
    private TextView currentTimeText, durationText;
    private View playerSection;
    // Ми не будемо використовувати ProgressBar, але ви можете додати його в XML і розкоментувати код, якщо хочете
    // private ProgressBar progressBar;

    // --- Поля для роботи з AudioService та асинхронних операцій ---
    private AudioService audioService;
    private boolean isServiceBound = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Runnable updateSeekBarRunnable;

    // ... (ServiceConnection залишається без змін)
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioService.AudioBinder binder = (AudioService.AudioBinder) service;
            audioService = binder.getService();
            audioService.setCallbacks(PsalomActivity.this);
            isServiceBound = true;
            updateUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            audioService = null;
        }
    };


    // --- Методи життєвого циклу Activity ---
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean nightMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("mode_night", false);
        setTheme(nightMode ? R.style.NightThemeV1 : R.style.AppTheme);
        setContentView(R.layout.activity_psalom);

        Intent intent = getIntent();
        idK = intent.getIntExtra("fname", 0);
        databaseHelper = DatabaseHelper.getDatabaseHelper(getApplicationContext());

        initializeViews();
        initializeRecyclerViews();
        setupNavigationButtons();
        initializeUpdateSeekBarRunnable();

        showPsalmy(idK);
    }

    // ... (onStart, onStop, onDestroy, onRestart залишаються без змін)
    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, AudioService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceBound) {
            audioService.setCallbacks(null);
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown(); // Важливо закрити ExecutorService
        if (audioService != null && !audioService.isPlaying()) {
            stopService(new Intent(this, AudioService.class));
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }


    // --- Ініціалізація UI ---
    private void initializeViews() {
        // ... (код ініціалізації View залишається без змін)
        btnPrevK = findViewById(R.id.btn_prev_k);
        btnNextK = findViewById(R.id.btn_next_k);
        btnPrevKb = findViewById(R.id.btn_prev_k_b);
        btnNextKb = findViewById(R.id.btn_next_k_b);
        rvListPsalom = findViewById(R.id.rv_list_psalom);
        rvPsalom = findViewById(R.id.rv_psalom);
        scrollPsalmy = findViewById(R.id.scroll_psalmy);
        seekBar = findViewById(R.id.seekBar);
        playPauseBtn = findViewById(R.id.buttonPlayPause);
        currentTimeText = findViewById(R.id.textCurrentTime);
        durationText = findViewById(R.id.textDuration);
        playerSection = findViewById(R.id.playerSection);
//        progressBar = findViewById(R.id.your_progress_bar_id); // Якщо ви його додали

        playPauseBtn.setOnClickListener(v -> handlePlayPauseClick());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isServiceBound && audioService != null) {
                    audioService.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { handler.removeCallbacks(updateSeekBarRunnable); }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { if (isServiceBound && audioService != null && audioService.isPlaying()) { handler.post(updateSeekBarRunnable); } }
        });
    }

    private void initializeRecyclerViews() {
        // ... (код ініціалізації RecyclerView та оптимізації залишається без змін)
        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(this);
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW);
        rvListPsalom.setLayoutManager(flexboxLayoutManager);
        adapterNumbrs = new NavFlexboxAdapter(this);
        rvListPsalom.setAdapter(adapterNumbrs);
        adapterNumbrs.setOnItemClickListener((position, v) -> {
            View child = rvPsalom.getLayoutManager().findViewByPosition(position);
            if (child != null) {
                float y = rvPsalom.getY() + child.getY();
                scrollPsalmy.smoothScrollTo(0, (int) y);
            }
        });
        rvPsalom.setLayoutManager(new LinearLayoutManager(this));
        adapterPsalmy = new PsalmyAdapter(this);
        rvPsalom.setAdapter(adapterPsalmy);
//        rvPsalom.setHasFixedSize(true);
        rvPsalom.setItemViewCacheSize(10);
        rvPsalom.setDrawingCacheEnabled(true);
        rvPsalom.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
    }

    // ... (setupNavigationButtons та initializeUpdateSeekBarRunnable залишаються без змін)
    private void setupNavigationButtons() {
        View.OnClickListener prevClick = v -> showPsalmy(tmpKp);
        View.OnClickListener nextClick = v -> showPsalmy(tmpKn);
        btnPrevK.setOnClickListener(prevClick);
        btnPrevKb.setOnClickListener(prevClick);
        btnNextK.setOnClickListener(nextClick);
        btnNextKb.setOnClickListener(nextClick);
    }
    private void initializeUpdateSeekBarRunnable() {
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (isServiceBound && audioService != null && audioService.isPlaying()) {
                    updateUIForPlayback();
                    handler.postDelayed(this, 1000);
                }
            }
        };
    }


    // --- ОСНОВНА ЛОГІКА З АСИНХРОННИМ ЗАВАНТАЖЕННЯМ ---
    private void showPsalmy(int id) {
        this.idK = id;

        // Очищуємо старі дані та показуємо стан завантаження
        adapterPsalmy.setItems(new java.util.ArrayList<>(), idK);
        adapterNumbrs.setItems(new java.util.ArrayList<>());
        // if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // Запускаємо завантаження даних з БД у фоновому потоці
        executorService.execute(() -> {
            // Важкі операції, які виконуються у фоні
            List<PsalomDto> psaloms = databaseHelper.getPsaloms(idK);
            List<String> numbers = databaseHelper.getNumbers(idK);
            String audioUrl = databaseHelper.getAudioUrlById(idK);

            // Повертаємося в головний потік, щоб оновити UI
            handler.post(() -> {
                // if (progressBar != null) progressBar.setVisibility(View.GONE);

                // Оновлюємо навігацію та адаптери
                if (idK == 0 || idK == 21 || idK == 22) {
                    hideNavigation();
                } else {
                    updateNavigation();
                }

                playerSection.setVisibility(audioUrl != null && !audioUrl.isEmpty() ? View.VISIBLE : View.GONE);

                adapterPsalmy.setItems(psaloms, idK);
                adapterNumbrs.setItems(numbers);

                scrollPsalmy.scrollTo(0, 0);
                updateUI();
            });
        });
    }

    // ... (handlePlayPauseClick та решта методів залишаються без змін)
    private void handlePlayPauseClick() {
        if (!isServiceBound || audioService == null) return;
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Відсутнє підключення до Інтернету.", Toast.LENGTH_LONG).show();
            return;
        }

        executorService.execute(() -> {
            String audioUrl = databaseHelper.getAudioUrlById(idK);

            handler.post(() -> {
                if (audioUrl == null || audioUrl.isEmpty()) {
                    Toast.makeText(this, "Аудіо для цієї кафізми недоступне.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!audioService.isPlayerActive() || !audioService.getCurrentUrl().equals(audioUrl)) {
                    String title = getString(R.string.Kafyzma) + " " + idK;
                    Intent serviceIntent = new Intent(this, AudioService.class);
                    ContextCompat.startForegroundService(this, serviceIntent);
                    audioService.startPlaying(audioUrl, title);
                } else {
                    audioService.togglePlayPause();
                }
            });
        });
    }
    private void updateUI() {
        if (!isServiceBound || audioService == null) {
            playPauseBtn.setText("прослухати кафізму");
            seekBar.setProgress(0);
            currentTimeText.setText("00:00");
            durationText.setText("00:00");
            return;
        }
        updateUIForPlayback();
    }
    private void updateUIForPlayback() {
        if (audioService.isPlayerActive()) {
            if (audioService.isPlaying()) {
                playPauseBtn.setText("Пауза");
                handler.post(updateSeekBarRunnable);
            } else {
                playPauseBtn.setText("прослухати кафізму");
                handler.removeCallbacks(updateSeekBarRunnable);
            }
            seekBar.setMax(audioService.getDuration());
            seekBar.setProgress(audioService.getCurrentPosition());
            durationText.setText(formatTime(audioService.getDuration()));
            currentTimeText.setText(formatTime(audioService.getCurrentPosition()));
        } else {
            playPauseBtn.setText("прослухати кафізму");
            seekBar.setProgress(0);
            currentTimeText.setText("00:00");
            durationText.setText("00:00");
        }
    }
    private void hideNavigation() {
        btnPrevK.setVisibility(View.GONE);
        btnNextK.setVisibility(View.GONE);
        btnPrevKb.setVisibility(View.GONE);
        btnNextKb.setVisibility(View.GONE);
        rvListPsalom.setVisibility(View.GONE);
        findViewById(R.id.textView1).setVisibility(View.GONE);
    }
    private void updateNavigation() {
        tmpKp = idK - 1;
        tmpKn = idK + 1;
        boolean isFirst = idK == 1;
        boolean isLast = idK == 20;
        setNavVisibility(btnPrevK, btnPrevKb, !isFirst);
        setNavVisibility(btnNextK, btnNextKb, !isLast);
        btnPrevK.setText(getString(R.string.Kafyzma) + " " + tmpKp);
        btnPrevKb.setText(getString(R.string.Kafyzma) + " " + tmpKp);
        btnNextK.setText(getString(R.string.Kafyzma) + " " + tmpKn);
        btnNextKb.setText(getString(R.string.Kafyzma) + " " + tmpKn);
    }
    private void setNavVisibility(Button btn1, Button btn2, boolean visible) {
        btn1.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        btn2.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }
    @Override public void onPrepared() { runOnUiThread(this::updateUI); }
    @Override public void onCompletion() {
        runOnUiThread(() -> {
            updateUI();
            if (idK < 20) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    showPsalmy(idK + 1);
                    handlePlayPauseClick();
                }, 500);
            }
        });
    }
    @Override public void onError() { runOnUiThread(() -> { Toast.makeText(this, "Помилка відтворення аудіо", Toast.LENGTH_SHORT).show(); updateUI(); }); }
    @Override public void onPlaybackStateChanged() { runOnUiThread(this::updateUI); }
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
    @SuppressLint("DefaultLocale")
    private String formatTime(int millis) {
        if (millis < 0) millis = 0;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%02d:%02d", minutes, seconds);
    }
}