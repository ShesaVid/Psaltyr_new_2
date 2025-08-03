package ua.pl.pokrova.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import java.io.IOException;

import ua.pl.pokrova.R;
import ua.pl.pokrova.view.PsalomActivity;

public class AudioService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String TAG = "AudioService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AudioServiceChannel";
    private String currentUrl = "";

    private MediaPlayer mediaPlayer;
    private final IBinder binder = new AudioBinder();
    private String currentTrackTitle = "Псалтир"; // Default title

    // Інтерфейс для комунікації з активністю
    private ServiceCallbacks serviceCallbacks;

    public class AudioBinder extends Binder {
        public AudioService getService() {
            return AudioService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Тут можна обробляти дії з нотифікації, наприклад, play/pause
        String action = intent.getAction();
        if (action != null) {
            if (action.equals("ACTION_PLAY_PAUSE")) {
                if (mediaPlayer != null) {
                    togglePlayPause();
                }
            } else if (action.equals("ACTION_STOP")) {
                stopForeground(true);
                stopSelf();
            }
        }
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Audio Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, PsalomActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Кнопка Play/Pause
        Intent playPauseIntent = new Intent(this, AudioService.class);
        playPauseIntent.setAction("ACTION_PLAY_PAUSE");
        PendingIntent pPlayPauseIntent = PendingIntent.getService(this, 1, playPauseIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String playPauseText = (mediaPlayer != null && mediaPlayer.isPlaying()) ? "Пауза" : "Грати";

        // Кнопка Stop
        Intent stopIntent = new Intent(this, AudioService.class);
        stopIntent.setAction("ACTION_STOP");
        PendingIntent pStopIntent = PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);


        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Відтворення аудіо")
                .setContentText(currentTrackTitle)
                .setSmallIcon(R.mipmap.ic_launcher) // Ваша іконка
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_play_arrow, playPauseText, pPlayPauseIntent) // Потрібно додати іконки
                .addAction(R.drawable.ic_stop, "Зупинити", pStopIntent) // Потрібно додати іконки
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1)) // Показувати перші дві кнопки в компактному вигляді
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    public void startPlaying(String url, String title) {
        this.currentUrl = url; // Зберігаємо URL поточного треку
        this.currentTrackTitle = title;
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
        } else {
            mediaPlayer.reset();
        }

        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Error setting data source", e);
            if (serviceCallbacks != null) serviceCallbacks.onError();
        }
    }

    public void togglePlayPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
        // Оновити нотифікацію
        startForegroundService();
        if (serviceCallbacks != null) serviceCallbacks.onPlaybackStateChanged();
    }

    public boolean isPlayerActive() {
        // Плеєр активний, якщо він існує і не в стані помилки чи завершення
        return mediaPlayer != null;
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        startForegroundService();
        if (serviceCallbacks != null) serviceCallbacks.onPrepared();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (serviceCallbacks != null) serviceCallbacks.onCompletion();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer Error: " + what + ", " + extra);
        if (serviceCallbacks != null) serviceCallbacks.onError();
        return true; // true означає, що помилка оброблена
    }

    // Також, давайте очищати URL при знищенні плеєра
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        currentUrl = ""; // Очищаємо URL
    }

    // Інтерфейс для зворотного зв'язку з активністю
    public interface ServiceCallbacks {
        void onPrepared();
        void onCompletion();
        void onError();
        void onPlaybackStateChanged();
    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }
}