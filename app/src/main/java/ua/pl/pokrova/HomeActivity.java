package ua.pl.pokrova;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

import ua.pl.pokrova.db.DatabaseHelper;
import ua.pl.pokrova.db.KafuzmaDto;
import ua.pl.pokrova.ui.CustomExpandableListAdapter;
import ua.pl.pokrova.view.PsalomActivity;

public class HomeActivity extends AppCompatActivity {

    DatabaseHelper databaseHelper;
    ExpandableListView expandableListView;
    ExpandableListAdapter expandableListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        databaseHelper = DatabaseHelper.getDatabaseHelper(getApplicationContext());
        databaseHelper.create_db();

        List<KafuzmaDto> listKafuzm = databaseHelper.getAllKafuzm();
        List<String> name = new ArrayList<>();
        name.add("name");

        Button btn_1 = findViewById(R.id.btn_1);
        Button btn_3 = findViewById(R.id.btn_3);
        Button btn_4 = findViewById(R.id.btn_4);

        btn_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beginRead(0);
            }
        });
        btn_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beginRead(21);
            }
        });
        btn_4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beginRead(22);
            }
        });


        PreferenceManager.setDefaultValues(this,R.xml.preferences,false);

        expandableListView = (ExpandableListView) findViewById(R.id.expandableListView);
        expandableListAdapter = new CustomExpandableListAdapter(this, name,listKafuzm);
        expandableListView.setAdapter(expandableListAdapter);
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
                beginRead(i1+1);
                return false;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.Choice_Size:
                startActivity(new Intent(this, SettingsActivity.class));
                return (true);
            case R.id.About:
                showAlertDialog();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public void showAlertDialog(){
        android.app.AlertDialog.Builder alert = new android.app.AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.titlebar, null);
        alert.setCustomTitle(view);
        alert.setPositiveButton(
                "Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }
        );

        AlertDialog Dialog = alert.create();
        Dialog.show();

    }

    private void beginRead(int id){
        Intent intent = new Intent(HomeActivity.this, PsalomActivity.class);
        intent.putExtra("fname", id);
        Log.e("frame","f="+id);
        startActivity(intent);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        recreate();
    }

}
