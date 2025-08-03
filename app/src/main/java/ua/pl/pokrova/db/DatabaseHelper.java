package ua.pl.pokrova.db;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final int DATABASE_USER_VERSION = 2;  //при зміні БД підняти на одиницю.
    private static String DB_PATH;
    private static String DB_NAME = "psaltyr.db";

    /*
        Так як зваємодія з базою зроблена через SQLiteDatabase.openDatabase, а не getWriteableDatabase,
        то SCHEMA ігнорується і методи onCreate та onUpgrade не викликаються

        Для змін БД використовуйте поле DATABASE_USER_VERSION.
        Перед додаванням audio_url реальна версія бази була 0.

        При зміні БД скопіюйте psaltyr.db з папки /assets в інше місце (бо в /assets не можна міняти user_version),
        зробіть зміни в базі через DB Browser для SQLite (наприклад, sqlitebrowser) та збільшіть user_verion на одиницю, збережіть дані. Перевірте, що зміни збереглись
        наступним SQL скріптом:

        PRAGMA user_version;

        Після чого скопіюйте оновлену базу в /assets, перезатерши старий файл.
        Збільшіть DATABASE_USER_VERSION на одиницю - він має бути таким самим, як значення, яке повернув PRAGMA user_version;

        Коли запустите додаток, create_db побачить, що версія бази змінилась, видалить на телефоні стару базу,
        скопіює нову з /assets і стане використовувати нову.
     */
    private static final int SCHEMA = 2;
    static final String TABLE_KAFUZMA = "kafuzma";
    static final String TABLE_PSALOM = "psalom";

    static final String ID_KAFUZMA = "id";
    static final String NAME_KAFUZMA = "name";
    static final String DESC_KAFUZMA = "desc";
    static final String ID_PSALOM = "id";
    static final String NAME_PSALOM = "name";
    static final String SHORT_DESC_PSALOM = "short_desc";
    static final String DESC_PSALOM = "desc";
    static final String ID_K_PSALOM = "id_kaf";
    static final String AUDIO_URL = "audio_url";
    private Context myContext;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, SCHEMA);
        this.myContext=context;
        DB_PATH =context.getFilesDir().getPath() + DB_NAME;
    }


    public static synchronized DatabaseHelper getDatabaseHelper(Context context) {

        DatabaseHelper dbHelper = null;
        if (dbHelper == null) {
            dbHelper = new DatabaseHelper(context.getApplicationContext());
        }
        return dbHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion,  int newVersion) {
    }

    public void create_db(){
        InputStream myInput = null;
        OutputStream myOutput = null;
        try {
            File file = new File(DB_PATH);

            boolean shouldCopy = !file.exists();

            if (!shouldCopy) {
                SQLiteDatabase tempDb = SQLiteDatabase.openDatabase(
                        DB_PATH, null, SQLiteDatabase.OPEN_READONLY);
                int currentVersion = tempDb.getVersion();
                tempDb.close();

                if (currentVersion < DATABASE_USER_VERSION) {
                    file.delete(); // Delete old DB
                    shouldCopy = true;
                }
            }
            if (shouldCopy) {
                this.getReadableDatabase();

                myInput = myContext.getAssets().open(DB_NAME);

                String outFileName = DB_PATH;

                myOutput = new FileOutputStream(outFileName);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = myInput.read(buffer)) > 0) {
                    myOutput.write(buffer, 0, length);
                }

                myOutput.flush();
                myOutput.close();
                myInput.close();
            }
        }
        catch(IOException ex){
            Log.d("DatabaseHelper", ex.getMessage());
        }
    }
    public SQLiteDatabase open()throws SQLException {

        return SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READWRITE);
    }

    public String getAudioUrlById(int id) {
        String audioUrl = null;
        String selectQuery = "SELECT " + AUDIO_URL + " FROM " + TABLE_KAFUZMA + " WHERE " + ID_KAFUZMA + " = ?";

        try (SQLiteDatabase db = open();
             Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(id)})) {

            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(AUDIO_URL);
                if (columnIndex != -1) {
                    audioUrl = cursor.getString(columnIndex);
                }
            }
        } catch (SQLException e) {
            Log.e("DatabaseHelper", "Error getting audio URL for id: " + id);
        }

        return audioUrl;
    }

    public List<KafuzmaDto> getAllKafuzm(){

        List<KafuzmaDto> list = new ArrayList<KafuzmaDto>();

        String selectQuery = "SELECT  * FROM " + TABLE_KAFUZMA;

        SQLiteDatabase db = open();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {

            while (!cursor.isAfterLast()) {
                KafuzmaDto kafuzma = new KafuzmaDto();
                kafuzma.setId(cursor.getInt(0));
                kafuzma.setName(cursor.getString(1));
                kafuzma.setDesc(cursor.getString(2));

                list.add(kafuzma);
                cursor.moveToNext();
            }

        }

        return list;
    }

    public List<PsalomDto> getPsaloms(int id){

        List<PsalomDto> list = new ArrayList<>();

        String selectQuery = "SELECT  * FROM " + TABLE_PSALOM + " WHERE " + ID_K_PSALOM + " = " + id;

        SQLiteDatabase db = open();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {

            while (!cursor.isAfterLast()) {
                PsalomDto psalom = new PsalomDto();
                psalom.setId(cursor.getInt(0));
                psalom.setName(cursor.getString(1));
                psalom.setShort_desc(cursor.getString(2));
                psalom.setDesc(cursor.getString(3));
                psalom.setId_kaf(cursor.getInt(4));

                list.add(psalom);

                cursor.moveToNext();
            }

        }

        return list;
    }

    public List<String> getNumbers(int id){
        List<String> numbersPsalom = new ArrayList<>();

        String selectQuery = "SELECT  * FROM " + TABLE_PSALOM + " WHERE " + ID_K_PSALOM + " = " + id;

        SQLiteDatabase db = open();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {

            while (!cursor.isAfterLast()) {
                if (cursor.getString(1).contains("Псалом")){
                    numbersPsalom.add(cursor.getString(1).split(" ")[1]);
                }
                cursor.moveToNext();
            }

        }

        return numbersPsalom;
    }
}
