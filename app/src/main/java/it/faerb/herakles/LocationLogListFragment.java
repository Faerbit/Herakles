package it.faerb.herakles;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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
        getListView().post(new Runnable() {
            @Override
            public void run() {
                if (getListAdapter().getCount() > 0) {
                    noItemsFitScreen = getListView().getHeight() / getListView().getChildAt(0).getHeight();
                    // to be sure
                    noItemsFitScreen += 2;
                }
            }
        });
    }

    public static LocationLogListFragment newInstance() {
        return new LocationLogListFragment();
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        LocationLog.replaceCurrentLocationLog(getContext(), position);
        ((MainActivity) getActivity()).transitionToTrackFragment();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                         int totalItemCount) {
        //Log.d(TAG, String.format("onScroll: first: %d, visible: %d, total: %d", firstVisibleItem,
        //        visibleItemCount, totalItemCount));
        LocationLogAdapter adapter = (LocationLogAdapter) this.getListAdapter();
        if(totalItemCount < LocationLog.getFilesCount(getContext())) {
            int end = totalItemCount + (noItemsFitScreen - visibleItemCount);
            //Log.d(TAG, String.format("onScroll: loading %d - %d",totalItemCount, end));
            adapter.addAll(LocationLog.loadFiles(getContext(), totalItemCount, end));
            adapter.notifyDataSetChanged();
        }
    }
}
