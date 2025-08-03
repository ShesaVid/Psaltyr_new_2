package ua.pl.pokrova.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import java.util.List;

import ua.pl.pokrova.R;
import ua.pl.pokrova.db.KafuzmaDto;

public class CustomExpandableListAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<String> expandableListTitle;
    private List<KafuzmaDto> expandableListDetail;

    public CustomExpandableListAdapter(Context context, List<String> expandableListTitle, List<KafuzmaDto> expandableListDetail) {
        this.context = context;
        this.expandableListTitle = expandableListTitle;
        this.expandableListDetail = expandableListDetail;
    }

    @Override
    public Object getChild(int listPosition, int expandedListPosition) {
        return this.expandableListDetail.get(expandedListPosition);
    }

    @Override
    public long getChildId(int listPosition, int expandedListPosition) {
        return expandedListPosition;
    }

    @Override
    public View getChildView(int listPosition, final int expandedListPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        final KafuzmaDto kafuzma = (KafuzmaDto)getChild(listPosition, expandedListPosition);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.expandable_psalms, null);
        }
        TextView expandedListTextView = convertView.findViewById(R.id.textView);
        TextView txtNumb = convertView.findViewById(R.id.txt_number);
        ImageView imgBM = convertView.findViewById(R.id.iv_main);
        expandedListTextView.setText(kafuzma.getName());
        txtNumb.setText(kafuzma.getDesc());

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getInt("NKF",0)==kafuzma.getId()){
            imgBM.setVisibility(View.VISIBLE);
        } else imgBM.setVisibility(View.INVISIBLE);

        return convertView;
    }

    @Override
    public int getChildrenCount(int listPosition) {
        return this.expandableListDetail.size();
    }

    @Override
    public Object getGroup(int listPosition) {
        return this.expandableListTitle.get(listPosition);
    }

    @Override
    public int getGroupCount() {
        return this.expandableListTitle.size();
    }

    @Override
    public long getGroupId(int listPosition) {
        return listPosition;
    }

    @Override
    public View getGroupView(int listPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String listTitle = (String) getGroup(listPosition);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.expandable_kathisma, null);
        }
        TextView listTitleTextView = (TextView) convertView
                .findViewById(R.id.textView);
        //listTitleTextView.setTypeface(null, Typeface.BOLD);
        //listTitleTextView.setText(listTitle);
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int listPosition, int expandedListPosition) {
        return true;
    }
}
