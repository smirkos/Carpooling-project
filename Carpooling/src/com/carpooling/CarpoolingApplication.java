package com.carpooling;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Use a custom Application class to pass state data between Activities.
 */
public class CarpoolingApplication extends Application {
    private static final String TAG_LOG = "CarpoolingApplication";
	public static final String BUNDLE_DISPOSREQUEST_ACTIVITY = "bundleDisposRequestActivity";
	public static final String FROM_KEY = "From";
	public static final String TO_KEY = "To";
	public static final String DATE_KEY = "date";
	public static final String TIME_KEY = "time";
	public static final String TYPE_KEY = "type";
	public static final String STRING_POST = "keyStringPost";
    public static final String TYPE_1 = "carpooling";
    public static final String TYPE_2 = "request_carpooling";
    public static final String POSTIFREE_KEY = "postiFree";
    public static final String READMODE = "readModeKey";

    private final Session session;
    private final Context context;
    private List<GraphUser> selectedUsers;
	private GraphUser me;
    public ArrayList<JSONObject> listElementsMypost;
    public ArrayList<JSONObject> listElementsFriendPost;

    public CarpoolingApplication(Session session, Context context) {
        this.session = session;
        this.context = context;
        listElementsMypost = new ArrayList<JSONObject>();
        listElementsFriendPost = new  ArrayList<JSONObject>();
    }

    public List<GraphUser> getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers(List<GraphUser> users) {
        selectedUsers = users;
    }

	public void  setAboutMe(GraphUser me){
		this.me = me;
	}

    public GraphUser getMe(){
        return me;
    }

    public ArrayList<JSONObject> getListElementsMypost() {
        sendRequestsMyPosts(session);
        Log.d(TAG_LOG, "isEmpty:"+listElementsMypost.isEmpty());
        return listElementsMypost;
    }
    public ArrayList<JSONObject> getListElementsFriendPost() {
        getFriendPosts(session);
        Log.d(TAG_LOG, "isEmpty:"+listElementsFriendPost.isEmpty());
        return listElementsFriendPost;
    }

    /**
     * Controllare se esiste qualcuno che fa il mio stesso percorso
    */
    public boolean controlloDisponibilita(String from, String to, String date, String time){
        try{
            getFriendPosts(session);
            for (JSONObject friendPosti : listElementsFriendPost) {
                JSONObject b = new JSONObject(friendPosti.getString("message"));
                if (b.optString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1)) {
                    Log.d(TAG_LOG, "Message B:" + b.toString());
                    if(b.optString(CarpoolingApplication.FROM_KEY).equals(from) &&
                            b.optString(CarpoolingApplication.TO_KEY).equals(to) &&
                            b.optString(CarpoolingApplication.DATE_KEY).equals(date)) {
                        //1)Ho trovato una disponibilità che coincide con quella di un utente
                        Log.d(TAG_LOG,"Disponibilità trovata. return true");
                        return true;
                    }
                    if(b.optString(CarpoolingApplication.TO_KEY).equals(to) &&
                            b.optString(CarpoolingApplication.DATE_KEY).equals(date)) {
                        //2)Ho solo la destinazione in comune
                        Log.d(TAG_LOG,"Disponibilità trovata con 1 destinazione in comune. return true");
                        return true;
                    }
                    /*if(b.optString(CarpoolingApplication.FROM_KEY).equals(from) &&
                            b.optString(CarpoolingApplication.DATE_KEY).equals(date)) {
                        //cerca un secondo amico con uguale destinazione
                        for (JSONObject childFriendPost : listElementsFriendPost) {
                            JSONObject c = new JSONObject(childFriendPost.getString("message"));
                            if(c.optString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1) &&
                                    c.optString(CarpoolingApplication.TO_KEY).equals(to) &&
                                    c.optString(CarpoolingApplication.DATE_KEY).equals(date)) {
                                //3) trovati due amici B e C che hanno il percorso in comune con A
                                Log.d(TAG_LOG,"Disponibilità trovata con 1 partenza e 1 destinazione in comune. return true");
                                return true; //TODO: Forse da cancellare
                            }
                        }
                    } */
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }


    private void sendRequestsMyPosts(Session session) {
        String fqlQuery = "SELECT post_id,source_id, message " +
                "FROM stream " +
                "WHERE source_id = me() " +
                "AND app_id=" + context.getString(R.string.app_id_facebook) +
                "AND strpos(lower(message), '{') >= 0 " +
                "ORDER BY created_time DESC LIMIT 50";
        Log.d(TAG_LOG,fqlQuery);
        Bundle params = new Bundle();
        params.putString("q", fqlQuery);
        final Request request = new Request(session, "/fql", params, HttpMethod.GET, new Request.Callback() {
                    @Override
                    public void onCompleted(Response response) {
                        if (response.getError() != null) {
                            Log.d(TAG_LOG, "Errore:sendRequestsMyPosts: " + response.getError());
                        } else {
                            Log.d(TAG_LOG,"Stampa risultato.");
                            final GraphObject graphObject = response.getGraphObject();
                            JSONObject json = graphObject.getInnerJSONObject();

                            try {
                                JSONArray arrayMessage = json.getJSONArray("data");
                                Log.d(TAG_LOG, "arrayMessage.Length:" +arrayMessage.length());
                                for (int i = 0; i < arrayMessage.length(); i++) {
                                    Log.d(TAG_LOG, "message:" + i);
                                    JSONObject objectMess = arrayMessage.getJSONObject(i);
                                    Log.d(TAG_LOG, objectMess.toString());
                                    JSONObject jsonMess = new JSONObject(objectMess.getString("message"));
                                    //Filtra i posts
                                    if (jsonMess.optString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1) ||
                                            jsonMess.optString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_2))
                                        listElementsMypost.add(objectMess);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                            Log.d(TAG_LOG, "Query MyPost");
                        }
                    }
                });
        request.executeAndWait();
    }

    private void getFriendPosts(Session session){
        String fqlQuery = "SELECT post_id,source_id,message " +
                "FROM stream " +
                "WHERE source_id IN (SELECT uid FROM user WHERE is_app_user = 1 AND uid IN " +
                "(SELECT uid2 FROM friend WHERE uid1 = me())) " +
                "AND app_id=" + context.getString(R.string.app_id_facebook) +
                "AND strpos(lower(message), '{') >= 0 " +
                "ORDER BY created_time DESC LIMIT 50";
        Bundle params = new Bundle();
        params.putString("q", fqlQuery);
        final Request request = new Request(session, "/fql", params, HttpMethod.GET,
                new Request.Callback() {
                    @Override
                    public void onCompleted(Response response) {
                        if (response.getError() != null) {
                            Log.d(TAG_LOG, "Errore:getFriendPosts: " + response.getError());
                        } else {
                            try {
                                JSONArray arrayMessage = response.getGraphObject().getInnerJSONObject().getJSONArray("data");
                                for (int i = 0; i < arrayMessage.length(); i++) {
                                    JSONObject objectMess = arrayMessage.getJSONObject(i);
                                    JSONObject jsonMess = new JSONObject(objectMess.getString("message"));
                                    //Filtra i posts
                                    if (jsonMess.optString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1) ||
                                            jsonMess.optString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_2)) {
                                        listElementsFriendPost.add(objectMess);
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                            Log.d(TAG_LOG, "Query FriendPost");
                        }
                    }
                });
        request.executeAndWait();
    }

    public void PublishPost(String messToPost) {
        Session session = Session.getActiveSession();
        Request request = Request.newStatusUpdateRequest(session, messToPost, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                if ((response != null ? response.getError() : null) != null) {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.cancelled)
                            .setMessage(response.getError().toString())
                            .setPositiveButton(R.string.ok, null).show();
                }else {
                    Toast.makeText(context.getApplicationContext(), "Post pubblicato.", Toast.LENGTH_LONG).show();
                }
            }
        });
        request.executeAsync();
    }
}
