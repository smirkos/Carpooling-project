package com.carpooling;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.*;
import com.facebook.model.GraphUser;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.LoginButton;
import com.facebook.widget.ProfilePictureView;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class MainActivity extends Activity {

	private static final String TAG_LOG = "MainActivity";
    private static final String TAG_KEY = "Key hash";
	private static final String URL_PREFIX_FRIENDS = "https://graph.facebook.com/me/friends?access_token=";
    public static final int RESULT_ACTIVITY_DIPOSREQUEST = 1;
    private static final int STOP_SERVICE_MENU_OPTION = R.id.StopService_option_menu;
    private static final int START_SERVICE_MENU_OPTION = R.id.StartService_option_menu;

    public UiLifecycleHelper uiHelper;

    private final Session.StatusCallback statusCallback = new Session.StatusCallback(){
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            Log.d(TAG_LOG,"onSessionStateChange");
            onSessionStateChange(session, state, exception);
        }
    };

    private FacebookDialog.Callback dialogCallback = new FacebookDialog.Callback() {
        @Override
        public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
            Log.d(TAG_LOG, String.format("Error: %s", error.toString()));
        }

        @Override
        public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
            Log.d(TAG_LOG, "Success!");
        }
    };

    private TextView textUsernameView;
    private ProfilePictureView profilePictureView;
    private Intent controllerService;
    private GraphUser user;
    private ViewGroup controlsContainer;
    private ViewGroup statusProfileContainer;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(this, statusCallback);
        uiHelper.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        //Setta il titolo dell'app
        try {
            PackageManager pmanage = getPackageManager();
            PackageInfo pinfo = pmanage.getPackageInfo(getApplicationContext().getPackageName(), 0);
            this.setTitle(getString(R.string.app_name)+" "+pinfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        //Ricava la signature dell'app
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    getApplicationContext().getPackageName(),
                    PackageManager.GET_SIGNATURES);
            for (android.content.pm.Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d(TAG_KEY, Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }

        controllerService = new Intent(MainActivity.this, ControllerService.class);

        Button buttonSendRequest = (Button) findViewById(R.id.btnSendRequest);
		buttonSendRequest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent carListIntent = new Intent(MainActivity.this, CarListActivity.class);
                startActivity(carListIntent);
            }
        });
        Button buttonPublishPost = (Button) findViewById(R.id.button_publish_post);
		buttonPublishPost.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent insertTripIntent = new Intent(MainActivity.this, InsertTripActivity.class);
                startActivityForResult(insertTripIntent, MainActivity.RESULT_ACTIVITY_DIPOSREQUEST);
            }
        });
	    textUsernameView = (TextView)findViewById(R.id.profile_name);
		profilePictureView = (ProfilePictureView)findViewById(R.id.selection_profile_pic);
        controlsContainer = (ViewGroup) findViewById(R.id.main_ui_container);
        statusProfileContainer = (ViewGroup)findViewById(R.id.status_profile_container);
        String[] permission = getResources().getStringArray(R.array.permissions);

        //Check connessione internet
        if(!CheckIntConn()){
            showAlert(getString(R.string.cancelled), getString(R.string.errore_internet_connection_message));
            this.finish();
        }
        LoginButton loginButton = (LoginButton)findViewById(R.id.buttonLoginLogout);
        loginButton.setPublishPermissions(permission);
        loginButton.setSessionStatusCallback(statusCallback);
        loginButton.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                MainActivity.this.user = user;
                Log.d(TAG_LOG,"onUserInfoFetched");
                updateUI();
            }
        });

        //if(!isServiceRunning(controllerService.getComponent().getClassName()))
        //    startService(controllerService);
	}

    /**
     *
     * @param classNameService classe del mio servizio
     * @return true se il mio servizio è già attivo.
     */
    private boolean isServiceRunning(String classNameService) {
        final ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
        String temporaleService = "";

        for (int i = 0; i < services.size(); i++) {
            temporaleService=services.get(i).service.getClassName();
            if (classNameService.equals(temporaleService)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
	protected void onResume() {
		super.onResume();
        uiHelper.onResume();

		// Call the 'activateApp' method to log an app event for use in analytics and advertising reporting.  Do so in
		// the onResume methods of the primary Activities that an app may be launched into.
		AppEventsLogger.activateApp(this);

        updateUI();
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
	    if(resultCode == Activity.RESULT_OK)
	    {
		    if(requestCode == RESULT_ACTIVITY_DIPOSREQUEST)
		    {
			    try
			    {
                    CarpoolingApplication app = new CarpoolingApplication(Session.getActiveSession(),this);
				    JSONObject json = new JSONObject(data.getExtras().getString(CarpoolingApplication.BUNDLE_DISPOSREQUEST_ACTIVITY));
				    String mess = json.toString(); //.optString(CarpoolingApplication.STRING_POST);
				    //pubbblica post
				    app.PublishPost(mess);
			    }
			    catch (JSONException e) {
				    e.printStackTrace();
			    }
		    }
	    }
        uiHelper.onActivityResult(requestCode,resultCode,data,dialogCallback);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if ((exception instanceof FacebookOperationCanceledException ||
                exception instanceof FacebookAuthorizationException)) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.cancelled)
                    .setMessage(exception.getMessage())
                    .setPositiveButton(R.string.ok, null)
                    .show();
        } else if (state == SessionState.OPENED_TOKEN_UPDATED) {
            //Finisci il lavoro in sospeso
        }

        updateUI();
    }

    private void updateUI() {
        Session session = Session.getActiveSession();
        boolean enableButtons = (session != null && session.isOpened());
        Log.d(TAG_LOG,"UpdateUI " + enableButtons);
        if (enableButtons && user != null) {
            Log.d(TAG_LOG,"Sessione aperta");
            Log.i(TAG_LOG, URL_PREFIX_FRIENDS + session.getAccessToken());

            profilePictureView.setProfileId(user.getId());
            textUsernameView.setText(getResources().getText(R.string.titolo_app)+" " + user.getName());

            statusProfileContainer.setVisibility(View.VISIBLE);
            controlsContainer.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG_LOG,"Sessione chiusa");
            profilePictureView.setProfileId(null);
            textUsernameView.setText(null);

            statusProfileContainer.setVisibility(View.GONE);
            controlsContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        Log.i(TAG_LOG, "onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case START_SERVICE_MENU_OPTION:
                if(!isServiceRunning(controllerService.getComponent().getClassName()))
                    startService(controllerService);
                else
                    Toast.makeText(MainActivity.this,"Servizio già attivo.",Toast.LENGTH_SHORT).show();
                break;
            case STOP_SERVICE_MENU_OPTION:
                stopService(controllerService);
                break;
            default:
                return super.onContextItemSelected(item);
        }
        return  true;
    }

    private boolean CheckIntConn() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    private void showAlert(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
