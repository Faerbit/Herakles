package it.faerb.herakles;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class LocationLogListFragment extends ListFragment implements AbsListView.OnScrollListener {

    private static String TAG = "Herakles.LogListFragment";

    public LocationLogListFragment() {
        // required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListAdapter adapter = new LocationLogAdapter(getContext(), R.layout.locationlog_list_item,
                LocationLog.loadFiles(getContext(), 0, 1));
        this.setListAdapter(adapter);
    }

    private int noItemsFitScreen = 0;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setOnScrollListener(this);
        ListView listView = (ListView) getView().findViewById(R.id.list_view_logs);
        assert listView != null;
        if (getListAdapter().getCount() > 0) {
            noItemsFitScreen = listView.getHeight() / listView.getChildAt(0).getHeight();
            // to be sure
            noItemsFitScreen += 1;
        }
    }

    public static LocationLogListFragment newInstance() {
        return new LocationLogListFragment();
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {

    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                         int totalItemCount) {
        Log.d(TAG, String.format("onScroll: first: %d, visible: %d, total: %d", firstVisibleItem,
                visibleItemCount, totalItemCount));
        LocationLogAdapter adapter = (LocationLogAdapter) this.getListAdapter();
        if(totalItemCount <= LocationLog.getFilesCount(getContext())) {
            int start = firstVisibleItem + visibleItemCount;
            int end = start + (noItemsFitScreen - visibleItemCount);
            Log.d(TAG, String.format("onScroll: loading %d - %d", start, end));
            adapter.addAll(LocationLog.loadFiles(getContext(), start, end));
        }
    }
}
