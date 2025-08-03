package ua.pl.pokrova.view;

import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.annotation.SuppressLint;
import androidx.core.content.ContextCompat;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import ua.pl.pokrova.R;
import ua.pl.pokrova.db.DatabaseHelper;
import ua.pl.pokrova.ui.NavFlexboxAdapter;
import ua.pl.pokrova.ui.PsalmyAdapter;


public class PsalomActivity extends AppCompatActivity {

    // ... (поля класу залишаються такими ж: кнопки, RecyclerViews, idK, databaseHelper, адаптери, scrollPsalmy) ...
    private Button btnPrevK, btnNextK, btnPrevKb, btnNextKb;
    private RecyclerView rvListPsalom, rvPsalom;
    private int idK, tmpKp, tmpKn;
    private DatabaseHelper databaseHelper;
    private NavFlexboxAdapter adapterNumbrs;
    private PsalmyAdapter adapterPsalmy;
    private NestedScrollView scrollPsalmy;

    // ... (поля плеєра: mediaPlayer, seekBar, кнопки, тексти) ...
    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private Button playPauseBtn;
    private TextView currentTimeText, durationText;

    // ... (Handler та Runnable) ...
    private Handler handler = new Handler(Looper.getMainLooper());
    private View playerSection;
    private Runnable updateSeekBarRunnable;
    private boolean audioLoadErrorShown = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean nightMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("mode_night", false);
        setTheme(nightMode ? R.style.NightThemeV1 : R.style.AppTheme);
        setContentView(R.layout.activity_psalom);


        Intent intent = getIntent();
        idK = intent.getIntExtra("fname", 0);
        databaseHelper = DatabaseHelper.getDatabaseHelper(getApplicationContext());

        initializeUpdateSeekBarRunnable(); // Ініціалізація Runnable
        initializeViews();
        initializeRecyclerViews();
        setupNavigationButtons();

        showPsalmy(idK, false);
    }

    private void initializeUpdateSeekBarRunnable() {
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                performSeekBarUpdate();
            }
        };
    }

    private void initializeViews() {
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

        playPauseBtn.setEnabled(false);
    }

    private void initializeRecyclerViews() {
        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(this);
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW);

        rvListPsalom.setLayoutManager(flexboxLayoutManager);
        adapterNumbrs = new NavFlexboxAdapter(this);
        rvListPsalom.setAdapter(adapterNumbrs);
        adapterNumbrs.setOnItemClickListener((position, v) -> {
            float y = rvPsalom.getY() + rvPsalom.getChildAt(position).getY();
            scrollPsalmy.post(() -> {
                scrollPsalmy.fling(0);
                scrollPsalmy.smoothScrollTo(0, (int) y);
            });
        });

        rvPsalom.setLayoutManager(new LinearLayoutManager(this));
        adapterPsalmy = new PsalmyAdapter(this);
        rvPsalom.setAdapter(adapterPsalmy);
    }

    // ЗМІНЕНО: Налаштування кнопок навігації викликають showPsalmy з startPlayback = false
    private void setupNavigationButtons() {
        View.OnClickListener prevClick = v -> showPsalmy(tmpKp, false); // Не запускати автоплей
        View.OnClickListener nextClick = v -> showPsalmy(tmpKn, false); // Не запускати автоплей

        btnPrevK.setOnClickListener(prevClick);
        btnPrevKb.setOnClickListener(prevClick);
        btnNextK.setOnClickListener(nextClick);
        btnNextKb.setOnClickListener(nextClick);
    }

    private void showPsalmy(int id, boolean startPlayback) {
        idK = id;
        handleMediaPlayer(startPlayback);
        if (idK == 0 || idK == 21 || idK == 22) {
            hideNavigation();
        } else {
            updateNavigation();
        }

        adapterNumbrs.setItems(databaseHelper.getNumbers(idK));
        adapterPsalmy.setItems(databaseHelper.getPsaloms(idK), idK);

        // Скидання прокрутки
        rvPsalom.scrollToPosition(0);
        rvListPsalom.scrollToPosition(0);
        scrollPsalmy.scrollTo(0, 0);
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

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void handleMediaPlayer(boolean startPlayback) {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            handler.removeCallbacks(updateSeekBarRunnable);
            resetProgressBar();
        }
        updatePlayButtonState(false);
        audioLoadErrorShown = false; // Скидаємо прапорець перед спробою завантажити нове аудіо

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Відсутнє підключення до Інтернету.", Toast.LENGTH_LONG).show();
            playerSection.setVisibility(View.GONE);
            updatePlayButtonState(false);
            return; // Виходимо з методу, якщо немає інтернету
        }

        String audioUrl = databaseHelper.getAudioUrlById(idK);
        if (audioUrl != null && !audioUrl.isEmpty()) {
            playerSection.setVisibility(View.VISIBLE);
            setupMediaPlayer(audioUrl, startPlayback);
        } else {
            playerSection.setVisibility(View.GONE);
            Log.d("PsalomActivity", "No audio URL found for idK: " + idK);
        }
    }

    private void setupMediaPlayer(String audioUrl, boolean startPlayback) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.prepareAsync();

            Log.d("PsalomActivity", "Setting up MediaPlayer for URL: " + audioUrl + " | startPlayback: " + startPlayback);

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d("PsalomActivity", "MediaPlayer prepared for idK: " + idK);
                seekBar.setMax(mediaPlayer.getDuration());
                durationText.setText(formatTime(mediaPlayer.getDuration()));
                playPauseBtn.setEnabled(true);
                updatePlayButtonState(true);

                if (startPlayback) {
                    mediaPlayer.start();
                    playPauseBtn.setText("Пауза");
                    handler.postDelayed(updateSeekBarRunnable, 0);
                    Log.d("PsalomActivity", "Autoplay started for idK: " + idK);
                } else {
                    // Не запускаємо автоматично (для першого запуску / ручного переходу)
                    playPauseBtn.setText("прослухати кафізму");
                    Log.d("PsalomActivity", "Autoplay disabled for idK: " + idK + ". Waiting for play button.");

                }
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d("PsalomActivity", "MediaPlayer completed for idK: " + idK);
                resetProgressBar();
                handler.removeCallbacks(updateSeekBarRunnable);

                // Переходимо до наступної кафізми лише якщо не було помилки завантаження
                if (!audioLoadErrorShown && idK < 20) {
                    Log.d("PsalomActivity", "Auto-advancing to next kafizma: " + (idK + 1));
                    showPsalmy(idK + 1, true);
                } else if (idK == 20) {
                    Log.d("PsalomActivity", "Last kafizma finished, stopping playback.");
                    updatePlayButtonState(true);
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("PsalomActivity", "MediaPlayer error: what=" + what + ", extra=" + extra + " for idK: " + idK);
                resetProgressBar();
                handler.removeCallbacks(updateSeekBarRunnable);
                updatePlayButtonState(false);
                playerSection.setVisibility(View.GONE);
                // Показуємо повідомлення про помилку лише якщо його ще не було показано для цієї спроби
                if (!audioLoadErrorShown) {
                    Toast.makeText(this, "Помилка відтворення аудіо. Перевірте з'єднання.", Toast.LENGTH_LONG).show();
                    audioLoadErrorShown = true; // Встановлюємо прапорець, щоб більше не показувати це повідомлення
                }
                return true;
            });

            playPauseBtn.setOnClickListener(v -> {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    playPauseBtn.setText("прослухати кафізму");
                    handler.removeCallbacks(updateSeekBarRunnable);
                } else if (mediaPlayer != null) {
                    try {
                        mediaPlayer.start();
                        playPauseBtn.setText("Пауза");
                        handler.postDelayed(updateSeekBarRunnable, 0);
                    } catch (IllegalStateException e) {
                        Log.e("PsalomActivity", "Play button error: MediaPlayer not ready?", e);
                    }
                }
            });

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && mediaPlayer != null) {
                        try {
                            mediaPlayer.seekTo(progress);
                            currentTimeText.setText(formatTime(progress));
                        } catch (IllegalStateException e) {
                            Log.e("PsalomActivity", "SeekBar Error: MediaPlayer not ready for seekTo.", e);
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {
                    handler.removeCallbacks(updateSeekBarRunnable);
                }
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        handler.postDelayed(updateSeekBarRunnable, 0);
                    }
                }
            });

        } catch (IOException | IllegalArgumentException | SecurityException e) {
            Toast.makeText(this, "Помилка налаштування плеєра.", Toast.LENGTH_SHORT).show();
            playerSection.setVisibility(View.GONE);
            updatePlayButtonState(false);
        }
    }

    private void performSeekBarUpdate() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                int currentPosition = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                currentTimeText.setText(formatTime(currentPosition));
                handler.postDelayed(updateSeekBarRunnable, 1000);
            } catch (IllegalStateException e) {
                handler.removeCallbacks(updateSeekBarRunnable);
            }
        } else {
            handler.removeCallbacks(updateSeekBarRunnable);
        }
    }

    private void resetProgressBar() {
        seekBar.setProgress(0);
        currentTimeText.setText("00:00");
        if (mediaPlayer != null) {
            playPauseBtn.setText("прослухати кафізму");
            try {
                durationText.setText(formatTime(mediaPlayer.getDuration()));
            } catch (IllegalStateException e){
                durationText.setText("00:00");
            }
        } else {
            playPauseBtn.setText("прослухати кафізму");
            durationText.setText("00:00");
        }
    }

    private void updatePlayButtonState(boolean enabled) {
        playPauseBtn.setEnabled(enabled);
        boolean nightMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("mode_night", false);

        if (enabled) {
            // Визначаємо колір для АКТИВНОЇ кнопки
            int enabledColor; // Змінна для зберігання кольору
            if (nightMode) {
                // Нічний режим: використовуємо колір з ресурсів R.color.night_text_v1
                enabledColor = ContextCompat.getColor(this, R.color.night_text_v1);
            } else {
                // Денний режим: залишаємо ваш колір #333333
                enabledColor = Color.parseColor("#000000");
            }
            playPauseBtn.setTextColor(enabledColor); // Встановлюємо визначений колір

        } else {
            // НЕАКТИВНА кнопка: залишаємо сірий колір
            playPauseBtn.setTextColor(Color.GRAY);
        }
    }

    @SuppressLint("DefaultLocale")
    private String formatTime(int millis) {
        if (millis < 0) millis = 0;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playPauseBtn.setText("прослухати кафізму");
            handler.removeCallbacks(updateSeekBarRunnable);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        recreate();
    }

}