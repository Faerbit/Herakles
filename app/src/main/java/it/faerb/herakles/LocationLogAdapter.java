package it.faerb.herakles;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class LocationLogAdapter  extends ArrayAdapter<LocationLog>{

    static Context context;
    static int layoutResourceId;
    ArrayList<LocationLog> data = null;

    public LocationLogAdapter(Context context, int layoutResourceId, ArrayList<LocationLog> data) {
        super(context, layoutResourceId, data.toArray(new LocationLog[data.size()]));
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        Holder holder = null;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new Holder();
            holder.title = (TextView) row.findViewById(R.id.text_title);
            holder.subtitle = (TextView) row.findViewById(R.id.text_subtitle);

            row.setTag(holder);
        }
        else {
            holder = (Holder) row.getTag();
        }

        LocationLog log = data.get(position);
        holder.title.setText(log.getTitle());
        holder.subtitle.setText(log.getSubtitle());

        return row;
    }

    static class Holder {
        TextView title;
        TextView subtitle;
    }
}
