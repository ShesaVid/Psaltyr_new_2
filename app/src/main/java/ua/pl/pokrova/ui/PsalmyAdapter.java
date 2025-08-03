package ua.pl.pokrova.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ua.pl.pokrova.R;
import ua.pl.pokrova.db.PsalomDto;

public class PsalmyAdapter extends RecyclerView.Adapter<PsalmyAdapter.ViewHolder> {

    Context context;
    List<PsalomDto> arrayList = new ArrayList<>();
    int selectedPosition = -1;
    SharedPreferences sharedPreferences;
    int mKf;

    public PsalmyAdapter(Context context) {
        this.context = context;
        this.arrayList = arrayList;
    }

    public void setItems(List<PsalomDto> items, int kf){
        arrayList.clear();
        arrayList = items;
        mKf = kf;
        notifyDataSetChanged();
    }

    @Override
    public PsalmyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_psalom, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final PsalmyAdapter.ViewHolder holder, int position) {

        if(sharedPreferences.getInt("NP",0)==arrayList.get(position).getId()){
            selectedPosition = position;
        } else selectedPosition =-1;

        if(selectedPosition==position){
            holder.radioButton.setChecked(true);
        } else{
            holder.radioButton.setChecked(false);
        }

        if (arrayList.get(position).getName().contains("Псалом")){
            holder.radioButton.setVisibility(View.VISIBLE);
        } else holder.radioButton.setVisibility(View.INVISIBLE);

        holder.radioButton.setTag(position);
        final int ii = position;
        holder.radioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sharedPreferences.getInt("NP",0)==arrayList.get(ii).getId()){
                    holder.radioButton.setChecked(false);
                    selectedPosition = -1;
                    sharedPreferences.edit().putInt("NP",-1).apply();
                    sharedPreferences.edit().putInt("NKF",-1).apply();

                } else {
                    selectedPosition = (Integer)holder.radioButton.getTag();
                    sharedPreferences.edit().putInt("NP",arrayList.get(ii).getId()).apply();
                    sharedPreferences.edit().putInt("NKF",mKf).apply();
                }

                notifyDataSetChanged();
            }
        });

        if(arrayList.get(position).getName()!=null){
            holder.title.setText(arrayList.get(position).getName());
        } else{
            holder.title.setVisibility(View.GONE);
        }
        if(arrayList.get(position).getShort_desc()!=null){
            holder.short_desc.setText(arrayList.get(position).getShort_desc());
        } else{
            holder.short_desc.setVisibility(View.GONE);
        }
        try
        {
            InputStream inputStream = context.getResources().getAssets().open(arrayList.get(position).getDesc());

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            holder.desc.setText(Html.fromHtml(result.toString("UTF-8")));
        }
        catch (IOException exception)
        {
            holder.desc.setText("Failed loading html."+exception);
        }

    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView short_desc;
        TextView desc;
        RadioButton radioButton;
        float txtSize;
        String appThemeFont;

        public ViewHolder(View itemView) {
            super(itemView);

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            txtSize = Float.parseFloat(sharedPreferences.getString("size",""));
            appThemeFont = sharedPreferences.getString("font", "sans_serif");
            Log.e("font","appThemeFont="+appThemeFont);

            title = itemView.findViewById(R.id.name_psalom);
            short_desc = itemView.findViewById(R.id.short_desc);
            desc = itemView.findViewById(R.id.desc);
            radioButton = itemView.findViewById(R.id.rb_psalom);

            int resourceId = context.getResources().
                    getIdentifier(appThemeFont, "font", context.getPackageName());
            Log.e("font","fID="+resourceId);

            if(resourceId!=0){
                Typeface typeface = ResourcesCompat.getFont(context, resourceId);
                title.setTypeface(typeface);
                short_desc.setTypeface(typeface);
                desc.setTypeface(typeface);
            }

            title.setTextSize(txtSize);
            short_desc.setTextSize(txtSize);
            desc.setTextSize(txtSize);

        }
    }
}
