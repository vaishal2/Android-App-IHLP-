package Pedometer.Stepitup;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.PermissionChecker;
import android.view.MenuItem;

import stepitup.BuildConfig;
import stepitup.R;

public class Addfragment extends FragmentActivity {

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStackImmediate();
        } else {
            finish();
        }
    }

    @Override
    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        startService(new Intent(this, UpdateSensor.class));
        if (bundle == null) {
            // Create new fragment and transaction
            Fragment newFragment = new MainScreen();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            // Replace whatever is in the fragment_container view with this
            // fragment,
            // and add the transaction to the back stack
            transaction.replace(android.R.id.content, newFragment);

            // Commit the transaction
            transaction.commit();
        }

        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 23 && PermissionChecker
                .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PermissionChecker.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }


    public boolean ItemSelectedoptions(final MenuItem itm) {//optionsItemSelected
        switch (itm.getItemId()) {
            case android.R.id.home:
                getFragmentManager().popBackStackImmediate();
                break;
            case R.id.action_settings:
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new Settings()).addToBackStack(null)
                        .commit();
                break;

        }
        return true;
    }
}
