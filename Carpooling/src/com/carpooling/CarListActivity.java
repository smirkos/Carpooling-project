package com.carpooling;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.facebook.*;
import com.facebook.model.GraphUser;
import com.facebook.widget.ProfilePictureView;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarListActivity extends Activity {
	private static final String TAG_LOG = "CarListActivity";
    private static final int MENU_ITEM1 = 0;
    private static final String LINE1 = "TEXT1";
    private static final String LINE2 = "TEXT2";
    private static final String LINE3 = "USERID";
    private static final String LINE4 = "POSTID";

    private static final int MENU_MAIL = 0;
    private static final int MENU_ELIMINA_POST = 1;
    private static final int MENU_RICHIESTA_PASSAGGIO = 2;

    private Session session;
	private Session.StatusCallback statusCallback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            if ((exception instanceof FacebookOperationCanceledException ||
                    exception instanceof FacebookAuthorizationException)) {
                showAlert(getString(R.string.cancelled),exception.getMessage());

            } else if (state == SessionState.OPENED_TOKEN_UPDATED) {
                //Finisci il lavoro in sospeso
            }

            updateView();
        }
    };

    private ProfilePictureView profilePictureView;
	private TextView userNameView, txtInfoSelectionTrip;
	private ExpandableListView listView;
    private SimpleExpandableListAdapter expandableListAdapter;
    private List<Map<String, String>> listGroup;
    private List<List<Map<String, String>>> listItems;
    private GraphUser objAboutMe;
    private List<GraphUser> user;
    private ArrayList<JSONObject> listElementsFriendPost;
    private ArrayList<JSONObject> listElementsMypost;
    private boolean stampato1 = false;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.selection_trip);

        this.setTitle(getString(R.string.title_listTrip_activity));

        userNameView = (TextView)findViewById(R.id.selection_user_name);
		profilePictureView = (ProfilePictureView)findViewById(R.id.selection_profile_pic);
        txtInfoSelectionTrip = (TextView)findViewById(R.id.txtInfo_selection_trip);

		session = Session.getActiveSession();
		//listView = (ListView) findViewById(R.id.listViewPost);
        listView = (ExpandableListView)findViewById(R.id.listViewPost);
        listView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View view, int i, long l) {
                return parent.getChildCount() < 0;
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int i, long id) {
                Log.d(TAG_LOG, "item: " + parent.getItemAtPosition(i).toString());
                String[] tokener = parent.getItemAtPosition(i).toString().split("=");
                final String ID = tokener[9].substring(0,tokener[9].length()-1);
                final String ID_post = tokener[3].split(",")[0];
                //final int posti = Integer.decode(tokener[1].split(",")[0]);
                final String time = tokener[2].split(",")[0];
                final String date = tokener[7].split(",")[0];
                final  String from = tokener[8].split(",")[0];
                final String to = tokener[4].split(",")[0];
                Log.d(TAG_LOG, ID);
                Log.d(TAG_LOG, ID_post);

                Log.d(TAG_LOG,"Creazione del Dialog Contatta utente");
                AlertDialog.Builder builder = new AlertDialog.Builder(CarListActivity.this);
                builder.setTitle(R.string.contact_dialog_title)
                        .setItems(R.array.elenco_voci_contatto, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which){
                                    case MENU_MAIL:
                                        inviaMail(ID);
                                        break;
                                    case MENU_ELIMINA_POST:
                                        eliminaPost(ID_post);
                                        break;
                                    case MENU_RICHIESTA_PASSAGGIO:
                                        Intent insertTripIntent = new Intent(CarListActivity.this, InsertTripActivity.class);
                                        insertTripIntent.putExtra(CarpoolingApplication.TYPE_KEY,CarpoolingApplication.TYPE_2);
                                        insertTripIntent.putExtra(CarpoolingApplication.FROM_KEY,from);
                                        insertTripIntent.putExtra(CarpoolingApplication.TO_KEY,to);
                                        insertTripIntent.putExtra(CarpoolingApplication.DATE_KEY,date);
                                        insertTripIntent.putExtra(CarpoolingApplication.TIME_KEY,time);
                                        insertTripIntent.putExtra(CarpoolingApplication.POSTIFREE_KEY,1);
                                        insertTripIntent.putExtra(CarpoolingApplication.READMODE, false);
                                        startActivityForResult(insertTripIntent, MainActivity.RESULT_ACTIVITY_DIPOSREQUEST);
                                        break;
                                }
                                dialog.dismiss();
                            }
                        }).show();
                return true;
            }
        });

        //listItems = new ArrayList<Object>();  //Veccio arrayList usato per visualizzare una lista semplice
		listItems = new ArrayList<List<Map<String,String>>>();
        listGroup = new ArrayList<Map<String, String>>();

        //Crea un array adapter che servirà ad organizzare i dati da mostrare nella listView.
		//arrayAdapter = new ArrayAdapter<Object>(this,android.R.layout.simple_list_item_1, listItems);
        expandableListAdapter = new SimpleExpandableListAdapter(
                this,
                listGroup,
                R.layout.listgroup,
                new String[] { LINE1, LINE2, LINE3, LINE4, CarpoolingApplication.POSTIFREE_KEY, CarpoolingApplication.DATE_KEY, CarpoolingApplication.TIME_KEY, CarpoolingApplication.FROM_KEY,CarpoolingApplication.TO_KEY },
                new int[] { R.id.lblListHeader, R.id.text2, R.id.textID, R.id.textIDPOST },
                listItems,
                R.layout.listitem,
                new String[] { LINE1 },
                new int[] { R.id.lblListItem }
        );

        updateView();
	}

    @Override
	public void onStart() {
		super.onStart();
		Session.getActiveSession().addCallback(statusCallback);
	}

	@Override
	public void onStop() {
		super.onStop();
		Session.getActiveSession().removeCallback(statusCallback);
	}

	@Override
	public void onResume() {
		super.onResume();

		AppEventsLogger.activateApp(this);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK)
        {
            if(requestCode == MainActivity.RESULT_ACTIVITY_DIPOSREQUEST)
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
        //updateView();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Session session = Session.getActiveSession();
		Session.saveSession(session, outState);
	}

	private void updateView() {
		final Session session = Session.getActiveSession();
		if (session.isOpened()) {
            final ProgressDialog progress = ProgressDialog.show(CarListActivity.this, "", "Loading...");
            final CarpoolingApplication app = new CarpoolingApplication(session, this);
            new Thread() {
                public void run() {
                    try {
                        CarListActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listItems.clear();
                                listGroup.clear();

                                makeUserRequest();
                                makeMeRequest();

                                //Eseguo le query per ricavare i post
                                listElementsMypost = app.getListElementsMypost();
                                listElementsFriendPost = app.getListElementsFriendPost();

                                matchingMyPosts();
                                searchMatchFriendOffert();

                                listView.setAdapter(expandableListAdapter); //Mostra dati nella listview
                                if(expandableListAdapter.isEmpty())
                                    txtInfoSelectionTrip.setVisibility(TextView.VISIBLE);
                                else
                                    txtInfoSelectionTrip.setVisibility(TextView.GONE);
                            }
                        });
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                    progress.dismiss();
                }
            }.start();
		} else {
			profilePictureView.setProfileId(null);
			userNameView.setText("");
		}
	}

    /**
     * Eseguo i match fra i miei post
     */
    void recursiveMatchMyPosts(int i){
        if(i >= listElementsMypost.size() || listElementsMypost.isEmpty())
            return;

        try {
            JSONObject MyPosti = listElementsMypost.get(i);
            int contRichieste = 0;
            List<Map<String, String>> children = null;
            //La disponibilità è la mia
            JSONObject jsonMyMess = new JSONObject(MyPosti.getString("message"));
            GraphUser Mid = searchId(MyPosti.getString("source_id"));

            if(jsonMyMess.getString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1)) {
                children = new ArrayList<Map<String, String>>();
                stampaDisponibilita(jsonMyMess, Mid, MyPosti);//es. Se (IO) devo andare da FAENZA a CESENA
                Log.d(TAG_LOG,"Stampa My Offerta:"+i);
            }

            //if(i == 0)
                recursiveMatchFriendPosts(jsonMyMess, 0,false, contRichieste, children, Mid);
            //else
            //    recursiveMatchFriendPosts(jsonMyMess, 0, true, contRichieste, children, Mid);
            stampato1=true;
            recursiveMatchMyPosts(next(i));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    /**
     * Eseguo i match fra i post degli utenti
     */
    private void recursiveMatchFriendPosts(JSONObject jsonMyMess, int i, boolean stampato, int contRichieste, List<Map<String, String>> children, GraphUser Mid) {
        if(i >= listElementsFriendPost.size() || listElementsFriendPost.isEmpty())
            return;

        try {
            JSONObject FriendPosty = listElementsFriendPost.get(i);
            JSONObject jsonFriendMess = new JSONObject(FriendPosty.getString("message"));
            GraphUser Fid = searchId(FriendPosty.getString("source_id"));
            List<Map<String, String>> children2 = null;
            if(!stampato1){
                //children2 = new ArrayList<Map<String,String>>();

                if(jsonFriendMess.getString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1)) {
                    stampaDisponibilita(jsonFriendMess, Fid, FriendPosty); //es. Se (IO) devo andare da FAENZA a CESENA
                    Log.d(TAG_LOG,"Stampa Offerta utente:"+i);
                }
            } /*else {
                if(jsonFriendMess.getString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1) && !listItems.get(i).isEmpty()) {
                    Log.d(TAG_LOG," (riferimento lista figli) listitems["+i+"]="+listItems.get(i).size());
                    children2 = listItems.get(i);
                } else
                    children2 = new ArrayList<Map<String,String>>();
            }     */

            if (jsonMyMess.getString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1)) {
                //Cerco un match con le richieste degli utenti
                if (jsonFriendMess.getString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_2) &&
                        jsonMyMess.getString(CarpoolingApplication.FROM_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.FROM_KEY)) &&
                        jsonMyMess.getString(CarpoolingApplication.TO_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.TO_KEY)) &&
                        jsonMyMess.getString(CarpoolingApplication.DATE_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.DATE_KEY)) &&
                        jsonMyMess.getInt(CarpoolingApplication.POSTIFREE_KEY) > contRichieste) {
                    Log.d(TAG_LOG," Mia Offerta match con la richiesta amico.");
                    contRichieste += jsonFriendMess.getInt(CarpoolingApplication.POSTIFREE_KEY);
                    stampaRichiesta(jsonFriendMess, children, Fid); //es. un utente chiede un passaggio.
                    listItems.add(children);
                    recursiveMatchFriendPosts(jsonMyMess, next(i), stampato, contRichieste, children, Mid);
                    return;
                }

                //Cerco di combinare due disponibilità del tipo: "se (A) va da FAENZA a forli e (B) da forli a CESENA"
                if (jsonFriendMess.getString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1) &&
                        jsonMyMess.getString(CarpoolingApplication.FROM_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.FROM_KEY)) &&
                        jsonMyMess.getString(CarpoolingApplication.DATE_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.DATE_KEY))) {
                    //cerca un secondo amico con la destinazione uguale a (ME)
                    for (JSONObject FriendPosti : listElementsFriendPost) {
                        JSONObject c = new JSONObject(FriendPosti.getString("message"));
                        GraphUser Fid2 = searchId(FriendPosti.getString("source_id"));
                        if (c.getString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1) &&
                                c.optString(CarpoolingApplication.FROM_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.TO_KEY)) && //Destinazione di A == alla partenza di B
                                c.optString(CarpoolingApplication.TO_KEY).equals(jsonMyMess.getString(CarpoolingApplication.TO_KEY)) &&
                                c.optString(CarpoolingApplication.DATE_KEY).equals(jsonMyMess.getString(CarpoolingApplication.DATE_KEY)) &&
                                jsonMyMess.getInt(CarpoolingApplication.POSTIFREE_KEY) > contRichieste) {
                            //trovati due amici A e B che hanno il percorso in comune con me
                            contRichieste += 2;
                            Log.d(TAG_LOG, "Offerta trovata con 1 partenza e 1 destinazione in comune.");
                            stampaRichiesta(jsonFriendMess, children, Fid); //A
                            stampaRichiesta(c, children, Fid2); //B
                            listItems.add(children);
                            recursiveMatchFriendPosts(jsonMyMess, next(i), stampato, contRichieste, children, Mid);
                            return;
                        }
                    }
                }
                listItems.add(children);
            }
            /*
            //Se l'offerta è di un utente,
            //cerco un match con le mie richieste
            if (jsonFriendMess.getString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1) &&
                    jsonMyMess.getString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_2)) {
                if (jsonMyMess.getString(CarpoolingApplication.FROM_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.FROM_KEY)) &&
                        jsonMyMess.getString(CarpoolingApplication.TO_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.TO_KEY)) &&
                        jsonMyMess.getString(CarpoolingApplication.DATE_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.DATE_KEY))) {
                    Log.d(TAG_LOG, "Offerta amico con la mia richiesta.");
                    stampaRichiesta(jsonMyMess, children2, Mid);
                    listItems.add(children2);
                    recursiveMatchFriendPosts(jsonMyMess, next(i), stampato, contRichieste, children, Mid);
                    return;
                }
                if (!jsonMyMess.getString(CarpoolingApplication.FROM_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.FROM_KEY)) &&
                        jsonMyMess.getString(CarpoolingApplication.TO_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.TO_KEY)) &&
                        jsonMyMess.getString(CarpoolingApplication.DATE_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.DATE_KEY))) {
                    //Ho solo la destinazione in comune
                    Log.d(TAG_LOG, "Offerta trovata solo con 1 destinazione in comune.");
                    stampaRichiesta(jsonMyMess, children2, Mid);
                    listItems.add(children2);
                    recursiveMatchFriendPosts(jsonMyMess, next(i), stampato, contRichieste, children, Mid);
                }
            } else
                listItems.add(children2);  */
        }  catch (JSONException e) {
            e.printStackTrace();
        }
        recursiveMatchFriendPosts(jsonMyMess, next(i), true, contRichieste, children, Mid);
    }

    private int next(int i) {
        return i+1;
    }

    /**
     * Eseguo i match con i miei post
     */
    private void matchingMyPosts() {
        try {
            for (JSONObject MyPosti : listElementsMypost) {
                int contRichieste = 0;

                JSONObject jsonMyMess = new JSONObject(MyPosti.getString("message"));
                GraphUser Mid = searchId(MyPosti.getString("source_id"));

                if(jsonMyMess.getString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1)) {
                    List<Map<String, String>> children = new ArrayList<Map<String, String>>();
                    stampaDisponibilita(jsonMyMess, Mid, MyPosti);//es. Se (IO) devo andare da FAENZA a CESENA

                    searchMatchFriendRequest(jsonMyMess, children, contRichieste);
                }
            }
        } catch (JSONException e) {  e.printStackTrace();   }
    }

    private void searchMatchFriendOffert() {

        for (int i = 0; i < listElementsFriendPost.size(); i++) {
            try{
                JSONObject FriendPosty = listElementsFriendPost.get(i);
                JSONObject jsonFriendMess = new JSONObject(FriendPosty.getString("message"));
                GraphUser Fid = searchId(FriendPosty.getString("source_id"));

                if(jsonFriendMess.getString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1)) {
                    List<Map<String, String>> children = new ArrayList<Map<String,String>>();
                    stampaDisponibilita(jsonFriendMess, Fid, FriendPosty);//es. Se (IO) devo andare da FAENZA a CESENA

                    for (JSONObject MyPosti : listElementsMypost) {
                        //La disponibilità è la mia
                        JSONObject jsonMyMess = new JSONObject(MyPosti.getString("message"));
                        GraphUser Mid = searchId(MyPosti.getString("source_id"));

                        //Se l'offerta è di un mio amico
                        //Cerco un match con le mie richieste
                        if (jsonMyMess.getString(CarpoolingApplication.FROM_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.FROM_KEY)) &&
                                jsonMyMess.getString(CarpoolingApplication.TO_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.TO_KEY)) &&
                                jsonMyMess.getString(CarpoolingApplication.DATE_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.DATE_KEY))) {
                            Log.d(TAG_LOG, "Offerta amico con la mia richiesta.");
                            stampaRichiesta(jsonMyMess, children, Mid);
                            break;
                        }
                        if (!jsonMyMess.getString(CarpoolingApplication.FROM_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.FROM_KEY)) &&
                                jsonMyMess.getString(CarpoolingApplication.TO_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.TO_KEY)) &&
                                jsonMyMess.getString(CarpoolingApplication.DATE_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.DATE_KEY))) {
                            //2)Ho solo la destinazione in comune
                            Log.d(TAG_LOG, "Offerta trovata solo con 1 destinazione in comune.");
                            stampaRichiesta(jsonMyMess, children, Mid);
                            break;
                        }
                    }
                    listItems.add(children);
                }

            } catch (JSONException e){ e.printStackTrace();}
        }

    }

    private void searchMatchFriendRequest(JSONObject jsonMyMess, List<Map<String, String>> children, int contRichieste) {
        for (int i = 0; i < listElementsFriendPost.size(); i++) {
            try{
                JSONObject FriendPosty = listElementsFriendPost.get(i);
                JSONObject jsonFriendMess = new JSONObject(FriendPosty.getString("message"));
                GraphUser Fid = searchId(FriendPosty.getString("source_id"));

                //Cerco un match con le richieste degli amici
                if (jsonFriendMess.getString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_2) &&
                        jsonMyMess.getString(CarpoolingApplication.FROM_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.FROM_KEY)) &&
                        jsonMyMess.getString(CarpoolingApplication.TO_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.TO_KEY)) &&
                        jsonMyMess.getString(CarpoolingApplication.DATE_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.DATE_KEY)) &&
                        jsonMyMess.getInt(CarpoolingApplication.POSTIFREE_KEY) > contRichieste) {
                    Log.d(TAG_LOG," Mia Offerta match con la richiesta amico.");
                    contRichieste += jsonFriendMess.getInt(CarpoolingApplication.POSTIFREE_KEY);
                    stampaRichiesta(jsonFriendMess, children, Fid); //es. un mio amico chiede un passaggio.
                    break;
                }
                //Cerco di combinare due disponibilità del tipo: "se (A) va da FAENZA a forli e (B) da forli a CESENA"
                if (jsonFriendMess.getString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1) &&
                        jsonMyMess.getString(CarpoolingApplication.FROM_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.FROM_KEY)) &&
                        jsonMyMess.getString(CarpoolingApplication.DATE_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.DATE_KEY))) {
                    //cerca un secondo amico con la destinazione uguale a (ME)
                    for (JSONObject FriendPosti : listElementsFriendPost) {
                        JSONObject c = new JSONObject(FriendPosti.getString("message"));
                        GraphUser Fid2 = searchId(FriendPosti.getString("source_id"));
                        if (c.getString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1) &&
                                c.optString(CarpoolingApplication.FROM_KEY).equals(jsonFriendMess.getString(CarpoolingApplication.TO_KEY)) && //Destinazione di A == alla partenza di B
                                c.optString(CarpoolingApplication.TO_KEY).equals(jsonMyMess.getString(CarpoolingApplication.TO_KEY)) &&
                                c.optString(CarpoolingApplication.DATE_KEY).equals(jsonMyMess.getString(CarpoolingApplication.DATE_KEY)) &&
                                jsonMyMess.getInt(CarpoolingApplication.POSTIFREE_KEY) > contRichieste) {
                            //3) trovati due amici A e B che hanno il percorso in comune con me
                            contRichieste += 2;
                            Log.d(TAG_LOG, "Offerta trovata con 1 partenza e 1 destinazione in comune.");
                            stampaRichiesta(jsonFriendMess, children, Fid); //A
                            stampaRichiesta(c, children, Fid2); //B
                            break;
                        }
                    }
                }

            } catch (JSONException e){   e.printStackTrace();   }
        }
        listItems.add(children);
    }

    /**
     * Ricavo i dati personali dell'utente loggato
     *
     */
    private void makeMeRequest() {
		Log.d(TAG_LOG, "Richiesta dati profilo utente.");
		Request meRequest = Request.newMeRequest(session, new Request.GraphUserCallback() {
            @Override
            public void onCompleted(GraphUser user, Response response) {
                if (session == Session.getActiveSession()) {
                    if (user != null) {
                        profilePictureView.setProfileId(user.getId());
                        userNameView.setText(user.getName());
                        objAboutMe = user;
                    }
                }
                if ((response != null ? response.getError() : null) != null) {
                    showAlert("Errore:makeMeRequest",getString(R.string.ok));
                }
            }
        });
		meRequest.executeAndWait();
	}

    /**
     * Ricavo la lista degli amici
     *
     */
    private void makeUserRequest() {
        Request request = Request.newMyFriendsRequest(Session.getActiveSession(), new Request.GraphUserListCallback() {
            @Override
            public void onCompleted(List<GraphUser> users, Response response) {
                if ((response != null ? response.getError() : null) != null) {
                    showAlert("Errore: makeUserRequest",getString(R.string.ok));
                } else {
                    if (!users.isEmpty()) {
                        user = users;
                    }
                }
            }
        });
        Bundle params = request.getParameters();
        params.putString("fields", "username,email,name,first_name,last_name");
        request.setParameters(params);
        request.executeAndWait();
    }

    private void stampaDisponibilita(JSONObject jsonObject, GraphUser user1, JSONObject Post){
        //Caso 1: DISPONIBILITA'
        Map<String, String> curGroupMap = new HashMap<String, String>();
        listGroup.add(curGroupMap);
        StringBuilder s = new StringBuilder();
        if(user1.equals(objAboutMe))
            s.append("Io:");
        else
            s.append(user1.getName()).append(":");
        if(jsonObject.optString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1))
            s.append("\t[offerta]");
        else
            s.append("\t[richiesta]");
        curGroupMap.put(LINE1,s.toString());
        curGroupMap.put(LINE2, new StringBuilder().append("Vado da ").append(jsonObject.optString(CarpoolingApplication.FROM_KEY))
                .append(" a ").append(jsonObject.optString(CarpoolingApplication.TO_KEY))
                .append(" il ").append(jsonObject.optString(CarpoolingApplication.DATE_KEY))
                .append(", partenza ore ").append(jsonObject.optString(CarpoolingApplication.TIME_KEY))
                .append(", posti disponibili ").append(jsonObject.optString(CarpoolingApplication.POSTIFREE_KEY)).toString());
        curGroupMap.put(LINE3, user1.getId());
        curGroupMap.put(LINE4, Post.optString("post_id"));
        curGroupMap.put(CarpoolingApplication.POSTIFREE_KEY, jsonObject.optString(CarpoolingApplication.POSTIFREE_KEY));
        curGroupMap.put(CarpoolingApplication.FROM_KEY, jsonObject.optString(CarpoolingApplication.FROM_KEY));
        curGroupMap.put(CarpoolingApplication.TO_KEY, jsonObject.optString(CarpoolingApplication.TO_KEY));
        curGroupMap.put(CarpoolingApplication.DATE_KEY, jsonObject.optString(CarpoolingApplication.DATE_KEY));
        curGroupMap.put(CarpoolingApplication.TIME_KEY, jsonObject.optString(CarpoolingApplication.TIME_KEY));
    }

    private GraphUser searchId(String source_id) {
        for (GraphUser anUser : user) {
            if (anUser.getId().equals(source_id)) {
                return anUser;
            }
        }
        if(source_id.equals(objAboutMe.getId()))
            return objAboutMe;
        return null;
    }

    private void stampaRichiesta(JSONObject jsonObject, List<Map<String, String>> children, GraphUser user1){
        //Caso 2: Richieste
        Map<String, String> curChildMap = new HashMap<String, String>();
        children.add(curChildMap);
        StringBuilder s = new StringBuilder();
        if(user1.equals(objAboutMe))
            s.append("Io:");
        else
            s.append(user1.getName()).append(":");
        if(jsonObject.optString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1))
            s.append("\t[offerta]");
        else
            s.append("\t[richiesta]");
        s.append("\nRichiedo un passaggio da ").append(jsonObject.optString(CarpoolingApplication.FROM_KEY))
            .append(" a ").append(jsonObject.optString(CarpoolingApplication.TO_KEY))
            .append(" il ").append(jsonObject.optString(CarpoolingApplication.DATE_KEY))
            .append(",partenza ore ").append(jsonObject.optString(CarpoolingApplication.TIME_KEY));
        curChildMap.put(LINE1, s.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0,MENU_ITEM1,0,"Aggiorna").setIcon(android.R.drawable.stat_notify_sync);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case MENU_ITEM1:
                Log.d(TAG_LOG,"Aggiornamento dati in corso...");
                updateView();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void eliminaPost(final String id_post) {
        Log.d(TAG_LOG,"Elimina post.");
        final Request request = Request.newDeleteObjectRequest(Session.getActiveSession(), id_post, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                if ((response != null ? response.getError() : null) != null) {
                    Log.d(TAG_LOG, response.getError().toString());
                    showAlert("Errore:eliminaPost", getString(R.string.permission_not_granted));
                } else {
                    Toast.makeText(CarListActivity.this, "Post " + id_post + " cancellato.", Toast.LENGTH_SHORT).show();
                    updateView();
                }
            }
        });
        new AlertDialog.Builder(this)
                .setTitle("Cancella post")
                .setMessage("Vuoi davvero cancellare il tuo post?")
                .setPositiveButton(R.string.ok,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        request.executeAsync();
                    }
                })
                .setNegativeButton(R.string.cancelled,null).show();
    }

    private void showAlert(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    //Funzionalità aggiuntiva da completare
    private void chiamaGuidatore(String ID) {
        Log.d(TAG_LOG,"Chiama guidatore.");
        GraphUser u = searchId(ID);
        Intent i = new Intent(Intent.ACTION_CALL);
        Log.d(TAG_LOG, u.getProperty("mobile_phone").toString());
        i.putExtra(Intent.EXTRA_PHONE_NUMBER, u.getProperty("mobile_phone") != null? u.getProperty("mobile_phone").toString():" ");
        try {
            startActivity(Intent.createChooser(i, "Chiamata in corso..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(CarListActivity.this, "Applicazione per chiamare non trovata.", Toast.LENGTH_SHORT).show();
        }
    }

    private void inviaMail(String ID) {
        Log.d(TAG_LOG,"Invia mail su facebook.");
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        GraphUser u = searchId(ID);
        String mail = u.getUsername() + "@facebook.com";
        Log.d(TAG_LOG, mail);
        i.putExtra(Intent.EXTRA_EMAIL, new String[] { mail });
        i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.mail_subject)+" "+ u.getName());
        i.putExtra(Intent.EXTRA_TEXT, getString(R.string.mail_text) + u.getFirstName());
        try {
            startActivity(Intent.createChooser(i, "Invio mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(CarListActivity.this, "Non ci sono client email installati.", Toast.LENGTH_SHORT).show();
        }
    }
}



