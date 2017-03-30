/*
 * Herakles - Sports Activity Tracking App for Android
 * Copyright (c) 2017 Fabian Klemp
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package it.faerb.herakles;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
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
    public View getView(final int position, View convertView, ViewGroup parent) {
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

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocationLog.replaceCurrentLocationLog(getContext(), position);
                ((MainActivity) view.getContext()).transitionToTrackFragment();
            }
        });

        ImageButton delete_button = (ImageButton) row.findViewById(R.id.button_delete);
        assert delete_button != null;
        delete_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                dialog.setTitle(R.string.delete_dialog_title);
                dialog.setMessage(R.string.delete_dialog_text);
                dialog.setNegativeButton(R.string.no, null);
                dialog.setPositiveButton(R.string.yes, new AlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        data.remove(position);
                        LocationLogIO.deleteFile(context, position);
                        notifyDataSetChanged();

                    }});
                dialog.show();
            }
        });

        return row;
    }

    static class Holder {
        TextView begin;
        TextView distance;
        TextView duration;
    }
}
