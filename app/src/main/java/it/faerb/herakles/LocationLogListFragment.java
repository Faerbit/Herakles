package it.faerb.herakles;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

public class LocationLogListFragment extends ListFragment {

    private static String TAG = "Herakles.LogListFragment";

    public LocationLogListFragment() {
        // required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*ListView listView = (ListView) getView().findViewById(R.id.list_view_logs);
        assert listView != null;*/
        ListAdapter adapter = new LocationLogAdapter(getContext(), R.layout.locationlog_list_item,
                LocationLog.getLocationLogs());
        setListAdapter(adapter);
        //listView.setAdapter(adapter);
    }

    public static LocationLogListFragment newInstance() {
        return new LocationLogListFragment();
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {

    }
}
