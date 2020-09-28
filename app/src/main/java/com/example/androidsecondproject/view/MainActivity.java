package com.example.androidsecondproject.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.example.androidsecondproject.R;
import com.example.androidsecondproject.model.LocationPoint;
import com.example.androidsecondproject.model.Profile;
import com.example.androidsecondproject.model.SwipeAdapter;
import com.example.androidsecondproject.model.eViewModels;
import com.example.androidsecondproject.viewmodel.LocationViewModel;
import com.example.androidsecondproject.viewmodel.MainViewModel;
import com.example.androidsecondproject.viewmodel.ViewModelFactory;
import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;


public class MainActivity extends AppCompatActivity implements LoginFragment.LoginFragmentInterface, RegisterFragment.RegisterFragmentInterface,
        AccountSetupFragment.AccountSetupFragmentInterface, PreferencesFragment.PreferencesFragmentInterface, ProfilePhotoFragment.PhotoFragmentInterface, ProfileFragment.UpdateDrawerFromProfileFragment,MatchesFragment.OnMoveToChat {
    private final int REQUEST_CHECK_SETTINGS =2 ;
    private Geocoder mGeoCoder;
    private String mCityName;
    private LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    Handler mHandler;

    int LOCATION_REQUEST = 1;

    private BroadcastReceiver tokenReceiver;
    private final String LOGIN_FRAGMENT = "login_fragment";
    private final String REGISTER_FRAGMENT = "register_fragment";
    private final String ACCOUNT_SETUP_FRAGMENT = "account_setup_fragment";
    private final String ACCOUNT_PREFERENCES_FRAGMENT = "account_preferences_fragment";
    private final String ACCOUNT_PHOTO_FRAGMENT = "account_photo_fragment";
    private final String ACCOUNT_PROFILE_FRAGMENT = "account_profile_fragment";
    private final String SWIPE_FRAGMENT = "swipe_fragment";
    private final String QUESTIONS_FRAGMENT = "questions_fragment";
    private final String MATCHES_FRAGMENT = "matches_fragment";
    private final String CHAT_FRAGMENT = "chat_fragment";

    private final String STACK ="secondary_stack";

    private LoginFragment loginFragment;
    private RegisterFragment registerFragment;
    private MainViewModel mViewModel;
    private CircleImageView mProfileIv;
    private TextView mNameTv;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private View headerView;
    Observer<Location> mLocationObserver;


    SwipeFlingAdapterView swipeProfile;
    SwipeAdapter swipeAdapter;

  //  private ImageView loaderIv;
    private SpinKitView loadingAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

     //   loaderIv=findViewById(R.id.main_loader);
        loadingAnimation=findViewById(R.id.spin_kit);
        Log.d("notifi2",savedInstanceState+"");

        initializeViewComponents();
        setObservers();
        onMessageTokenReceived();
        navigationView.getMenu().getItem(0).setChecked(true);



        if (mViewModel.checkIfAuth()) { // from splash activity
            fetchProfileData();
        } else {
            moveToLoginFragment();
        }
    }

    private void initializeViewComponents() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        headerView = navigationView.getHeaderView(0);
        Toolbar toolbar = findViewById(R.id.toolbar);
        mProfileIv = headerView.findViewById(R.id.profile_image);
        mNameTv = headerView.findViewById(R.id.username_tv);
        setSupportActionBar(toolbar);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                menuItem.setChecked(true);
                drawerLayout.closeDrawers();
                handleNavigationItemSelected(menuItem.getTitle().toString());
                return false;
            }
        });

        ActionBar actionBar = getSupportActionBar();
        //   actionBar.setDefaultDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_menu_24);
    }

    private void setObservers() {
        mViewModel = new ViewModelProvider(this, new ViewModelFactory(getApplication(), eViewModels.Main)).get(MainViewModel.class);


        Observer<Profile> profileObserverSuccess = new Observer<Profile>() {
            @Override
            public void onChanged(Profile profile) {
                Toast.makeText(MainActivity.this, "observer", Toast.LENGTH_SHORT).show();
                mViewModel.setToken();
                if (profile.getPreferences() == null) {
                    moveToPreferences();
                   // loaderIv.setVisibility(View.GONE);
                    loadingAnimation.setVisibility(View.GONE);
                } else if (mViewModel.isFirstTime()) {
                    mNameTv.setText(profile.getFirstName());
                    Glide.with(MainActivity.this).load(profile.getProfilePictureUri()).error(R.drawable.man_profile).into(mProfileIv);
                    getTokenWhenLogin();
                    mViewModel.setFirstTime(false);
                    handleReturnFromNotif();
                  // loaderIv.setVisibility(View.GONE);
                    loadingAnimation.setVisibility(View.GONE);

                } else {
                    mNameTv.setText(profile.getFirstName());
                    Glide.with(MainActivity.this).load(profile.getProfilePictureUri()).error(R.drawable.man_profile).into(mProfileIv);
                }
            }


        };

        Observer<String> profileObserverFail = new Observer<String>() {
            @Override
            public void onChanged(String s) {
                moveToAccountSetup();
            }
        };
        Observer<Profile> otherProfileObserverSuccess = new Observer<Profile>() {
            @Override
            public void onChanged(Profile profile) {
                String chatId = getIntent().getExtras().getString("chat_id");
                Log.d("buuug", "bug");
                moveToSwipeFragment();
                moveToChat(mViewModel.getProfile(), profile, chatId); // get  here only from notification
            }
        };

        mViewModel.getOtherProfileResultSuccess().observe(this, otherProfileObserverSuccess);
        mViewModel.getProfileResultSuccess().observe(this, profileObserverSuccess);
        mViewModel.getProfileResultFailed().observe(this, profileObserverFail);
    }


    private void fetchProfileData() {
        //loaderIv.setVisibility(View.VISIBLE);
        Log.d("login","login");
        loadingAnimation.setVisibility(View.VISIBLE);
        mViewModel.getNavigationHeaderProfile();
    }

    private void handleNavigationItemSelected(String title) {

        switch (title) {
            case "Home":
                clearStack(null);
                break;
            case "My Profile":
            ProfileFragment profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentByTag(ACCOUNT_PROFILE_FRAGMENT);
                if (profileFragment == null || !profileFragment.isVisible()) {
                    moveToProfileFragment();
                }
                break;
            case "Your Matches":
                MatchesFragment matchesFragment = (MatchesFragment) getSupportFragmentManager().findFragmentByTag(MATCHES_FRAGMENT);
                if (matchesFragment == null || !matchesFragment.isVisible()) {
                    moveToMatchesFragment();
                }
                break;
            case "Questions":
                QuestionsFragment questionsFragment = (QuestionsFragment) getSupportFragmentManager().findFragmentByTag(QUESTIONS_FRAGMENT);
                if (questionsFragment == null || !questionsFragment.isVisible()) {
                    moveToQuestionsFragment();
                }
                break;
            case "Messages":
                break;
            case "Settings":
                break;
            case "Logout":
                mViewModel.setToken("");
                mViewModel.setFirstTime(true);
                mViewModel.setFirstLocation(true);
                LocationViewModel.getInstance(getApplicationContext()).removeObserver(mLocationObserver);
                mViewModel.logout();
                clearStack(null);
                getSupportFragmentManager().beginTransaction().remove(getSupportFragmentManager().findFragmentByTag(SWIPE_FRAGMENT)).commit();
                moveToLoginFragment();
                break;
        }
    }


    private void moveToSwipeFragment() {
        Log.d("login","swipe");
        final Fragment swipeFragment = SwipeFragment.newInstance(mViewModel.getProfile());
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.flContent_second, swipeFragment, SWIPE_FRAGMENT);
        transaction.commit();




        /*swipeProfile.setFlingListener(new SwipeFlingAdapterView.onFlingListener() {
            @Override
            public void removeFirstObjectInAdapter() {

            }

            @Override
            public void onLeftCardExit(Object o) {

            }

            @Override
            public void onRightCardExit(Object o) {

            }

            @Override
            public void onAdapterAboutToEmpty(int i) {

            }

            @Override
            public void onScroll(float v) {

            }
        });

        swipeProfile.setOnItemClickListener(new SwipeFlingAdapterView.OnItemClickListener() {
            @Override
            public void onItemClicked(int i, Object o) {

            }
        });*/

    }

    private void moveToLoginFragment() {
         LoginFragment loginFragment = LoginFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.main_activity_id, loginFragment, LOGIN_FRAGMENT);
        transaction.commit();
    }

    @Override
    public void onClickMoveToRegister() {
        RegisterFragment registerFragment = RegisterFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.main_activity_id, registerFragment, REGISTER_FRAGMENT);
        transaction.commit();
    }

    @Override
    public void onClickMoveToLogin() {
        LoginFragment loginFragment = LoginFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.main_activity_id, loginFragment, LOGIN_FRAGMENT);
        transaction.commit();
    }

    private void moveToAccountSetup() {
        Fragment accountSetupFragment = AccountSetupFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
      //  transaction.add(R.id.main_activity_id, accountSetupFragment, ACCOUNT_SETUP_FRAGMENT);
        transaction.replace(R.id.main_activity_id,accountSetupFragment,ACCOUNT_SETUP_FRAGMENT);
        transaction.commit();
    }

    @Override
    public void onMoveToAccountSetup() {
        moveToAccountSetup();
    }

    private void moveToPreferences() {
        PreferencesFragment preferencesFragment = PreferencesFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        /*transaction.add(R.id.main_activity_id, preferencesFragment, ACCOUNT_PREFERENCES_FRAGMENT);
        Fragment photoFragment = fragmentManager.findFragmentByTag(ACCOUNT_PHOTO_FRAGMENT);
        if(photoFragment!=null)
            transaction.remove(photoFragment);*/
        transaction.replace(R.id.main_activity_id,preferencesFragment,ACCOUNT_PREFERENCES_FRAGMENT);
        transaction.commit();
    }

    @Override
    public void OnClickContinueToPreferences() {
        moveToPreferences();
    }

    @Override
    public void OnClickContinueToPhoto(Profile profile) {
        ProfilePhotoFragment profilePhotoFragment = ProfilePhotoFragment.newInstance(profile);
        FragmentManager fragmentManager = getSupportFragmentManager();
     //   fragmentManager.popBackStack();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
       // transaction.add(R.id.main_activity_id, profilePhotoFragment, ACCOUNT_PHOTO_FRAGMENT);
        transaction.replace(R.id.main_activity_id,profilePhotoFragment,ACCOUNT_PHOTO_FRAGMENT);
    //    transaction.addToBackStack(STACK);
        transaction.commit();
    }

    @Override
    public void OnClickContinueToApp() {
        //TODO
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment preferenceFragment = fragmentManager.findFragmentByTag(ACCOUNT_PREFERENCES_FRAGMENT);
     //   fragmentManager.popBackStack();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.remove(preferenceFragment);
        transaction.commit();
        fetchProfileData();
    }

    @Override
    public void onLoginToApp() {
        FragmentManager fragmentManager = getSupportFragmentManager();
       // fragmentManager.popBackStack();
        Fragment loginFragment = fragmentManager.findFragmentByTag(LOGIN_FRAGMENT);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.remove(loginFragment);
        transaction.commit();
        fetchProfileData();
    }


    @SuppressLint("WrongConstant")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(Gravity.START);
        }
        return super.onOptionsItemSelected(item);
    }

    public void moveToProfileFragment() {
//      ProfileFragment profileFragment = ProfileFragment.newInstance(mViewModel.getProfile(),mViewModel.getPictureUri().toString());
        ProfileFragment profileFragment = ProfileFragment.newInstance(mViewModel.getProfile());
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
       // transaction.add(R.id.flContent, profileFragment, ACCOUNT_PROFILE_FRAGMENT);
        transaction.replace(R.id.flContent,profileFragment,ACCOUNT_PROFILE_FRAGMENT);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void moveToQuestionsFragment() {
        QuestionsFragment questionsFragment = QuestionsFragment.newInstance(mViewModel.getProfile());
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
      //  transaction.add(R.id.flContent, questionsFragment, QUESTIONS_FRAGMENT);
        transaction.replace(R.id.flContent,questionsFragment,QUESTIONS_FRAGMENT);
        transaction.addToBackStack(null);
        transaction.commit();

    }

    private void moveToMatchesFragment() {
        MatchesFragment matchesFragment = MatchesFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
       // transaction.add(R.id.flContent, matchesFragment, MATCHES_FRAGMENT);
        transaction.replace(R.id.flContent,matchesFragment,MATCHES_FRAGMENT);
        transaction.addToBackStack(null);

        transaction.commit();
    }
    private void moveToMatchesFragment(String matcherUid) {
        MatchesFragment matchesFragment = MatchesFragment.newInstance(matcherUid);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
      //  transaction.add(R.id.flContent, matchesFragment, MATCHES_FRAGMENT);
        transaction.replace(R.id.flContent,matchesFragment,MATCHES_FRAGMENT);
        transaction.addToBackStack(null);
        transaction.commit();

    }

    @Override
    public void onUpdateProfile(Profile profile) {
        mViewModel.setProfile(profile);
    }

    private void getUserLocation() {
        //   startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
       // turnGPSOn();


             mLocationObserver= new Observer<Location>() {
            @Override
            public void onChanged(Location location) {
                try {
                    List<Address> addressList = mGeoCoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    mCityName = addressList.get(0).getLocality();
                    if (mCityName != null) {
                        mViewModel.getProfile().setCity(mCityName);
                        mViewModel.getProfile().setLocation(new LocationPoint(location.getLatitude(),location.getLongitude()));
                        //   moveToSwipeFragment();
                        mViewModel.writeProfile();

                        Log.d("loc",mCityName);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (location != null) {
                    Toast.makeText(MainActivity.this, "User Location: Lat = " + location.getLatitude() + ", Lon =" + location.getLongitude(), Toast.LENGTH_SHORT).show();
                }
                if (mViewModel.isFirstLocation()) {
                    moveToSwipeFragment();
                    mViewModel.setFirstLocation(false);
                }

            }
        };
        LocationViewModel.getInstance(getApplicationContext()).observe(this, mLocationObserver);


    }

    /*private void turnGPSOn() {
        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", true);
        sendBroadcast(intent);
    }
*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                //  getUserLocation();
                startLocation();

            }
            else{
                moveToSwipeFragment();
             //   startLocation();
            //    requestLocationPermissions();

            }
        }
    }

    private void onMessageTokenReceived() {
        tokenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mViewModel.setToken(intent.getStringExtra("token"));
            }
        };

        IntentFilter filter = new IntentFilter("token_changed");
        LocalBroadcastManager.getInstance(this).registerReceiver(tokenReceiver, filter);
    }

    private void getTokenWhenLogin() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(MainActivity.this, new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String newToken = instanceIdResult.getToken();
                Toast.makeText(MainActivity.this, newToken, Toast.LENGTH_SHORT).show();
                mViewModel.setToken(newToken);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(tokenReceiver);
    }

    public void moveToChat(Profile myProfile, Profile otherProfile, String chatid) {
        ChatFragment chatFragment = ChatFragment.newInstance(myProfile, otherProfile, chatid);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.flContent, chatFragment, CHAT_FRAGMENT);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void OnClickMoveToChat(Profile myProfile, Profile otherProfile, String chatid) {
        moveToChat(myProfile, otherProfile, chatid);
    }

    public void handleReturnFromNotif() {
        Bundle bundle = getIntent().getExtras();// add these lines of code to get data from notification
        Log.d("notifi",getIntent().getStringExtra("other_match_uid")+"");

        if (bundle != null) {
            final Menu menu = navigationView.getMenu();
            menu.getItem(2).setChecked(true);
            if (bundle.containsKey("chat_id")) {
                mViewModel.getOtherProfile(bundle.getString("chat_id"));
            }
            else if(bundle.containsKey("other_match_uid")){
                Log.d("notifi","notifi");
                moveToSwipeFragment();
                moveToMatchesFragment(bundle.getString("other_match_uid"));
            }
            else {
                requestLocationPermissions();
            }
        }
        else {
            requestLocationPermissions();
        }

    }



    public void requestLocationPermissions() {
        mGeoCoder = new Geocoder(this, Locale.getDefault());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int hasLocationPremission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasLocationPremission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
            } else {
                startLocation();
                //getUserLocation();
            }
        } else {
            startLocation();
            //getUserLocation();
        }
    }

    private void startLocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationCallback = new LocationCallback() ;
        }
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(final LocationResult locationResult) {
                super.onLocationResult(locationResult);
                /*mHandler = new Handler();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            List<Address> addressList = mGeoCoder.getFromLocation(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude(), 1);
                            mCityName = addressList.get(0).getLocality();
                            if (mCityName != null) {
                                    mHandler.removeCallbacks(this);
                                    mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
                                    mViewModel.getProfile().setCity(mCityName);
                                    mViewModel.getProfile().setLocation(new LocationPoint(locationResult.getLastLocation().getLatitude(),locationResult.getLastLocation().getLongitude()));
                                    moveToSwipeFragment();
                                    mViewModel.writeProfile();

                                    Log.d("locc",mCityName);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });*/
                mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
                getUserLocation();
            }
        };
        final LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(500);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        builder.setAlwaysShow(true);


        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());


        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mFusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
                }
            }
        });
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        //TODO:make a custom window.
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK)
                startLocation();
               // getUserLocation();
            else
                moveToSwipeFragment();
        }
    }
    private void clearStack(String stackName){
        FragmentManager fm =getSupportFragmentManager();

        for(int i = 0; i < fm.getBackStackEntryCount(); ++i) {
            fm.popBackStack(stackName,FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @Override
    public void onBackPressed() {
        final Menu menu = navigationView.getMenu();
        if (menu.getItem(0).isChecked()||menu.getItem(6).isChecked()) {
            Log.d("wtf","wtf");
            super.onBackPressed();
        }
        else{
            ChatFragment chatFragment = (ChatFragment) getSupportFragmentManager().findFragmentByTag(CHAT_FRAGMENT);
            if (chatFragment != null && chatFragment.isVisible()) {
                super.onBackPressed();
            }
            else {
                clearStack(null);
                navigationView.getMenu().getItem(0).setChecked(true);
            }
            }
        }
    @Override
    protected void onNewIntent(Intent intent) {
        this.setIntent(intent);

        String action = intent.getStringExtra(intent.getAction());

        Log.d("teest", getIntent().getAction()+"");
        if (action != null) {
            Log.d("teest", "shiva said right");

        }
        super.onNewIntent(intent);
    }

}




