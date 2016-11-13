package com.carpooling;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;
import com.facebook.Session;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Mirko
 * Date: 14/12/13
 * Time: 15.06
 * Servizio notifica passaggio disponibile
 */
public class ControllerService extends Service {

    private final static String LOG_TAG = "ControllerService";
    private static final int SIMPLE_NOTIFICATION_ID = 1;
    private BackgroundThread backgroundThread;
    private int notificationNumber;
    private NotificationManager notificationManager;
    private CarpoolingApplication app;
    private Session session;

    public IBinder onBind(Intent intent) {
        // Ritorniamo null in quanto non si vuole permettere
        // l'accesso al servizio da una applicazione diversa
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Facciamo partire il BackgroundThread
        backgroundThread = new BackgroundThread();
        backgroundThread.start();
        session = Session.getActiveSession();
        app = new CarpoolingApplication(session, this);
        // Otteniamo il riferimento al NotificationManager
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "Service Started");
        Toast.makeText(getApplicationContext(), "Servizio Avviato!", Toast.LENGTH_SHORT).show();
        // Inizializziamo il numero di notifiche inviate
        notificationNumber = 0;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        backgroundThread.running = false;
        Log.d(LOG_TAG, "Service Destroyed");
        Toast.makeText(getApplicationContext(), "Servizio Fermato!", Toast.LENGTH_SHORT).show();
    }

    public void sendNotification(CharSequence messaggio, String from, String to, String date, String time, String postiFree) {
        // Creaiamo l'intent dell'activity per inviare una richiesta all'autostoppista trova.
        Intent resultIntent = new Intent(this, InsertTripActivity.class);
        resultIntent.putExtra(CarpoolingApplication.TYPE_KEY,CarpoolingApplication.TYPE_2);
        resultIntent.putExtra(CarpoolingApplication.FROM_KEY,from);
        resultIntent.putExtra(CarpoolingApplication.TO_KEY,to);
        resultIntent.putExtra(CarpoolingApplication.DATE_KEY,date);
        resultIntent.putExtra(CarpoolingApplication.TIME_KEY,time);
        resultIntent.putExtra(CarpoolingApplication.POSTIFREE_KEY, postiFree);
        resultIntent.putExtra(CarpoolingApplication.READMODE,false);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(InsertTripActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
                .setContentTitle("CarpoolingApp")
                .setContentText(messaggio)
                .setSmallIcon(R.drawable.carpooling)
                .setNumber(++notificationNumber)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(resultPendingIntent);

        // La lanciamo attraverso il Notification Manager
        notificationManager.notify(SIMPLE_NOTIFICATION_ID, notification.build());
    }

    private final class BackgroundThread extends Thread{
        /*
		 * Tempo di sleep del thread in millisecondi. (60 secondi)
		 */
        private final static long DELAY = 60*1000;

        public boolean running = true;
        private boolean trovato = false;

        public void run(){
            while (running) {
                try {
                    Log.d(LOG_TAG, "Eseguo query.");
                    //Eseguo le query per ricavare gli ultimi post
                    ArrayList<JSONObject> listElementsMypost = app.getListElementsMypost();
                    //ArrayList<JSONObject> listElementsFriendPost = app.getListElementsFriendPost();

                    //final Calendar c = Calendar.getInstance();

                    //Controllare se esiste qualcuno che fa il mio stesso percorso
                    for (int i = 0; i < listElementsMypost.size(); i++) {
                        JSONObject a = new JSONObject(listElementsMypost.get(i).getString("message"));
                        /*int g = Integer.decode(a.optString(CarpoolingApplication.DATE_KEY).split("/")[0]);
                        int m = Integer.decode(a.optString(CarpoolingApplication.DATE_KEY).split("/")[1]);
                        int y = Integer.decode(a.optString(CarpoolingApplication.DATE_KEY).split("/")[2]);  */

                        if (a.optString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1)) {
                        //TODO: Risolvere problema per non far partire sempre la stessa notifica
                            Log.d(LOG_TAG,"Message A:" + a.toString());

                            if(app.controlloDisponibilita(a.optString(CarpoolingApplication.FROM_KEY),
                                    a.optString(CarpoolingApplication.TO_KEY),
                                    a.optString(CarpoolingApplication.DATE_KEY),a.optString(CarpoolingApplication.TIME_KEY))) {
                                //1)ho trovato una disponibilità che coincide con quella di un mio amico
                                Log.d(LOG_TAG, "Trovato un amico che fa la mia stessa strada. ");
                                sendNotification("Trovato passaggio disponibile",
                                        a.optString(CarpoolingApplication.FROM_KEY),
                                        a.optString(CarpoolingApplication.TO_KEY),
                                        a.optString(CarpoolingApplication.DATE_KEY),
                                        a.optString(CarpoolingApplication.TIME_KEY),
                                        a.optString(CarpoolingApplication.POSTIFREE_KEY));
                            }
                        }
                    }
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            Log.d(LOG_TAG, "BackgroundThread Stopped");
            // Al termine del metodo run terminiamo il servizio
            stopSelf();
        }
    }
}


