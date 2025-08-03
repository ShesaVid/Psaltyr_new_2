package ua.pl.pokrova.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ua.pl.pokrova.R;

public class NavFlexboxAdapter extends RecyclerView.Adapter<NavFlexboxAdapter.ViewHolder>{

    Context context;
    List<String> arrayList = new ArrayList<>();
    private static ClickListener clickListener;

    public NavFlexboxAdapter(Context context) {
        this.context = context;
        this.arrayList = arrayList;
    }

    public void setItems(List<String> items){
        arrayList.clear();
        arrayList = items;
        notifyDataSetChanged();
    }

    @Override
    public NavFlexboxAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_nav, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NavFlexboxAdapter.ViewHolder holder, final int position) {

        holder.title.setText(arrayList.get(position));

    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.btn_numb);
            title.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            clickListener.onItemClick(getAdapterPosition(), view);
        }
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        NavFlexboxAdapter.clickListener = clickListener;
    }

    public interface ClickListener {
        void onItemClick(int position, View v);
    }
}
