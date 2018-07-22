package crinysoft.yellowduck;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ListViewAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private ArrayList<ListViewItem> listData;

    public ListViewAdapter(Context context, ArrayList<ListViewItem> listData) {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.listData = listData;
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int i) {
        return listData.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ListViewItem listViewItem = listData.get(i);

        if (listViewItem.getItemType() == ListViewItem.ITEM_TYPE_SEND) {
            view = inflater.inflate(R.layout.talk_item_right, viewGroup, false);
            TextView text = (TextView) view.findViewById(R.id.tvTextRight);
            text.setText(listViewItem.getText());
        } else if (listViewItem.getItemType() == ListViewItem.ITEM_TYPE_RESV) {
            view = inflater.inflate(R.layout.talk_item_left, viewGroup, false);
            TextView text = (TextView) view.findViewById(R.id.tvTextLeft);
            text.setText(listViewItem.getText());
        }

        return view;
    }
}