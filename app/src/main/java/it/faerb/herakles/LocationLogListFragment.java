/*
 * Herakles - Sports Activity Tracking App for Android
 * Copyright (c) 2017 Fabian Klemp
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package it.faerb.herakles;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

public class LocationLogListFragment extends ListFragment implements AbsListView.OnScrollListener {

    private static String TAG = "Herakles.LogListFragment";

    public LocationLogListFragment() {
        // required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListAdapter adapter = new LocationLogAdapter(getContext(), R.layout.locationlog_list_item,
                LocationLog.loadMetaFiles(getContext(), 0, 1));
        this.setListAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_locationlog_list, container, false);
    }

    private int noItemsFitScreen = 0;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setOnScrollListener(this);
    }

    public static LocationLogListFragment newInstance() {
        return new LocationLogListFragment();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                         int totalItemCount) {
        //Log.d(TAG, String.format("onScroll: first: %d, visible: %d, total: %d", firstVisibleItem,
        //        visibleItemCount, totalItemCount));
        LocationLogAdapter adapter = (LocationLogAdapter) this.getListAdapter();
        ListView listView = getListView();
        View child = listView.getChildAt(0);
        if (noItemsFitScreen == 0 && adapter.getCount() > 0) {
            if (child != null) {
                noItemsFitScreen = listView.getHeight() / child.getHeight();
                // to be sure
                noItemsFitScreen += 2;
            }
        }
        if(totalItemCount < LocationLog.getFilesCount(getContext())) {
            int end = totalItemCount + (noItemsFitScreen - visibleItemCount);
            adapter.addAll(LocationLog.loadMetaFiles(getContext(), totalItemCount, end));
            adapter.notifyDataSetChanged();
        }
    }
}
