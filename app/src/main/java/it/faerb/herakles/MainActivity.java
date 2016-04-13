package it.faerb.herakles;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity  {

    static final String TAG = "Herakles.MainActivity";

    // START PERMISSION CHECK
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        String message = getResources().getString(R.string.app_name) + " "
                + getResources().getString(R.string.permission_request_intro);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            message += "\n" + getResources().getString(R.string.permission_request_location);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            message += "\n" + getResources().getString(R.string.permission_request_storage);
        }
        if (!permissions.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            String[] params = permissions.toArray(new String[permissions.size()]);
            requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        } // else: We already have permissions, so handle as normal
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions();
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String[] titles = getResources().getStringArray(R.array.navigation_menu);
        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert  drawerLayout != null;
        ListView navDrawer = (ListView) findViewById(R.id.nav_drawer);
        assert navDrawer != null;
        navDrawer.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, titles));
        navDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, String.format("onItemClick: %d", position));

                Fragment fragment = null;
                Class fragmentClass;
                switch (position) {
                    case 0:
                        fragmentClass = TrackFragment.class;
                        break;
                    case 1:
                        fragmentClass = LocationLogListFragment.class;
                        break;
                    default:
                        fragmentClass = TrackFragment.class;
                }
                try {
                    fragment = (Fragment) fragmentClass.newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.frame_layout_content, fragment)
                        .commit();

                // close drawer after item is selected
                drawerLayout.closeDrawers();
            }
        });
        navDrawer.setItemChecked(0, true);

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this,                  // host Activity
                drawerLayout,         // DrawerLayout object
                toolbar,
                R.string.nav_open,  // "open drawer" description for accessibility
                R.string.nav_close  //"close drawer" description for accessibility
        );
        drawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.syncState();

        Fragment fragment = null;
        try {
            fragment = TrackFragment.class.newInstance();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.frame_layout_content, fragment).commit();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION,
                        PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION and WRITE_EXTERNAL_STORAGE
                Boolean location = perms.get(Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
                Boolean storage = perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;
                if (location && storage) {
                    // All Permissions Granted
                    Toast.makeText(MainActivity.this,
                            getResources().getString(R.string.permission_response_granted),
                            Toast.LENGTH_SHORT).show();
                } else if (location) {
                    Toast.makeText(this,
                            getResources().getString(R.string.permission_response_storage),
                            Toast.LENGTH_LONG).show();
                } else if (storage) {
                    Toast.makeText(this,
                            getResources().getString(R.string.permission_response_location),
                            Toast.LENGTH_LONG).show();
                } else { // !location && !storage case
                    // Permission Denied
                    Toast.makeText(this,
                            getResources().getString(R.string.permission_response_storage) +
                                    "\n" + getResources().getString(R.string.permission_response_location),
                            Toast.LENGTH_SHORT).show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
