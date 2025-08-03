package ua.pl.pokrova.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Spanned;
import android.text.Html;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ua.pl.pokrova.R;
import ua.pl.pokrova.db.PsalomDto;

public class PsalmyAdapter extends RecyclerView.Adapter<PsalmyAdapter.ViewHolder> {

    private final Context context;
    private List<PsalomDto> arrayList = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private int mKf;
    private int selectedPosition = -1;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    // --- КЕШ ДЛЯ ЗБЕРІГАННЯ ЗАВАНТАЖЕНОГО HTML ---
    private final LruCache<String, Spanned> contentCache;

    public PsalmyAdapter(Context context) {
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        // Ініціалізуємо кеш (наприклад, на 20 елементів)
        this.contentCache = new LruCache<>(20);
    }

    public void setItems(List<PsalomDto> newItems, int kf) {
        final List<PsalomDto> oldItems = new ArrayList<>(this.arrayList);
        this.mKf = kf;
        this.contentCache.evictAll(); // Очищуємо кеш при оновленні даних

        // Find the selected position
        int np = sharedPreferences.getInt("NP", -1);
        selectedPosition = -1;
        if (np != -1) {
            for (int i = 0; i < newItems.size(); i++) {
                if (newItems.get(i).getId() == np) {
                    selectedPosition = i;
                    break;
                }
            }
        }

        executorService.execute(() -> {
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new PsalomDiffCallback(oldItems, newItems));
            mainThreadHandler.post(() -> {
                this.arrayList.clear();
                this.arrayList.addAll(newItems);
                diffResult.dispatchUpdatesTo(this);
            });
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_psalom, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        PsalomDto currentItem = arrayList.get(position);

        // --- Логіка для RadioButton (без змін) ---
        holder.radioButton.setOnCheckedChangeListener(null);
        holder.radioButton.setChecked(position == selectedPosition);

        if (currentItem.getName().contains("Псалом")) {
            holder.radioButton.setVisibility(View.VISIBLE);
        } else {
            holder.radioButton.setVisibility(View.INVISIBLE);
        }

        holder.radioButton.setOnClickListener(v -> {
            if (selectedPosition == position) {
                // Un-check
                selectedPosition = -1;
                sharedPreferences.edit().putInt("NP", -1).putInt("NKF", -1).apply();
            } else {
                int oldSelected = selectedPosition;
                selectedPosition = position;
                sharedPreferences.edit()
                        .putInt("NP", currentItem.getId())
                        .putInt("NKF", mKf)
                        .apply();
                if (oldSelected != -1) {
                    notifyItemChanged(oldSelected);
                }
            }
            notifyItemChanged(position);
        });

        // --- Налаштування тексту ---
        holder.title.setText(currentItem.getName());
        holder.short_desc.setText(currentItem.getShort_desc());
        holder.title.setVisibility(currentItem.getName() != null ? View.VISIBLE : View.GONE);
        holder.short_desc.setVisibility(currentItem.getShort_desc() != null ? View.VISIBLE : View.GONE);

        // --- АСИНХРОННЕ ЗАВАНТАЖЕННЯ З КЕШУВАННЯМ ---
        final String filePath = currentItem.getDesc();
        final Spanned cachedContent = contentCache.get(filePath);

        if (cachedContent != null) {
            // Якщо контент є в кеші, використовуємо його
            holder.desc.setText(cachedContent);
        } else {
            // Якщо ні, завантажуємо асинхронно
            holder.desc.setText("Завантаження...");
            executorService.execute(() -> {
                try {
                    InputStream inputStream = context.getAssets().open(filePath);
                    ByteArrayOutputStream result = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        result.write(buffer, 0, length);
                    }
                    inputStream.close();

                    final Spanned htmlContent = Html.fromHtml(result.toString("UTF-8"));
                    contentCache.put(filePath, htmlContent); // Зберігаємо в кеш

                    if (holder.getAdapterPosition() == position) {
                        mainThreadHandler.post(() -> holder.desc.setText(htmlContent));
                    }
                } catch (IOException e) {
                    if (holder.getAdapterPosition() == position) {
                        mainThreadHandler.post(() -> holder.desc.setText("Помилка завантаження."));
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    // Покращений ViewHolder
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, short_desc, desc;
        RadioButton radioButton;

        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.name_psalom);
            short_desc = itemView.findViewById(R.id.short_desc);
            desc = itemView.findViewById(R.id.desc);
            radioButton = itemView.findViewById(R.id.rb_psalom);

            // Налаштування шрифтів і розмірів можна залишити тут
            float txtSize = Float.parseFloat(sharedPreferences.getString("size", "16"));
            String appThemeFont = sharedPreferences.getString("font", "sans_serif");
            int resourceId = context.getResources().getIdentifier(appThemeFont, "font", context.getPackageName());
            if (resourceId != 0) {
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

    private static class PsalomDiffCallback extends DiffUtil.Callback {
        private final List<PsalomDto> oldList;
        private final List<PsalomDto> newList;

        public PsalomDiffCallback(List<PsalomDto> oldList, List<PsalomDto> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            final PsalomDto oldItem = oldList.get(oldItemPosition);
            final PsalomDto newItem = newList.get(newItemPosition);
            return Objects.equals(oldItem.getName(), newItem.getName())
                    && Objects.equals(oldItem.getShort_desc(), newItem.getShort_desc())
                    && Objects.equals(oldItem.getDesc(), newItem.getDesc());
        }
    }
}