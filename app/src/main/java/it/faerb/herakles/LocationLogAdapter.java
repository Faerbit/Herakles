package it.faerb.herakles;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;

public class LocationLogAdapter  extends ArrayAdapter<LocationLog> {

    private static Context context;
    private static int layoutResourceId;
    private ArrayList<LocationLog> data = null;

    public LocationLogAdapter(Context context, int layoutResourceId, ArrayList<LocationLog> data) {
        super(context, layoutResourceId, data);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        Holder holder;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new Holder();
            holder.begin = (TextView) row.findViewById(R.id.text_begin);
            holder.distance = (TextView) row.findViewById(R.id.text_distance);
            holder.duration = (TextView) row.findViewById(R.id.text_duration);

            row.setTag(holder);
        }
        else {
            holder = (Holder) row.getTag();
        }

        LocationLog log = data.get(position);
        holder.begin.setText(DateFormat.getDateTimeInstance().format(log.getBegin()));
        holder.distance.setText(Util.formatDistance(log.getDistance()));
        holder.duration.setText(Util.formatDuration(log.getDuration()));

        return row;
    }

    static class Holder {
        TextView begin;
        TextView distance;
        TextView duration;
    }
}
