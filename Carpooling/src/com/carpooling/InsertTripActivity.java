package com.carpooling;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.facebook.Session;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Created with IntelliJ IDEA.
 * User: Mirko
 * Date: 05/12/13
 * Time: 10.30
 * Crea i post da pubblicare a seconda del tipo.
 */
public class InsertTripActivity extends Activity {
	private static final String TAG_LOG = "InsertTripActivity";

	private static final int DIALOG_DATE_ID = 0;
	private static final int DIALOG_HOUR_ID = 1;
	private static final int SAVE_MENU_OPTION = R.id.menu_save;
	private static final int CANCELS_MENU_OPTION = R.id.menu_cancel;
	private static final int SPINNER_ITEM_2 = 1;
	private static final int SPINNER_ITEM_1 = 0;

	private int mYear;
	private int mMonth;
	private int mDay;
	private Button btnDate, btnTime;
	private int mHour;
	private int mMinute;
	private String type_request;
    private TextView txtFrom, txtTo;
    private boolean modalitaLettura;
    private String numPostiLiberi = "1";
    private Spinner spinner_posti;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.insert_trip);
        this.setTitle(getString(R.string.title_insertTrip_activity));

        txtFrom = (TextView)this.findViewById(R.id.editText_From_Place);
        txtTo = (TextView)this.findViewById(R.id.editText_To_Place);
        btnDate = (Button) findViewById(R.id.button_data_partenza);
        btnTime = (Button) findViewById(R.id.button_ora_partenza);
        Spinner spinner_tipo = (Spinner) findViewById(R.id.spinner_typeRequest);
        spinner_posti = (Spinner)findViewById(R.id.spinnerNumPosti);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.request_type, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> adapter_posti = ArrayAdapter.createFromResource(this,
                R.array.numero_posti_liberi, android.R.layout.simple_spinner_item);
        adapter_posti.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_tipo.setAdapter(adapter);
        spinner_posti.setAdapter(adapter_posti);
        spinner_tipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (adapterView.getSelectedItemPosition()){
                    case SPINNER_ITEM_1:
                        type_request = CarpoolingApplication.TYPE_1;
                        break;
                    case SPINNER_ITEM_2:
                        type_request = CarpoolingApplication.TYPE_2;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                adapterView.setSelection(0);
            }
        });
        spinner_posti.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                numPostiLiberi = adapterView.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                adapterView.setSelection(0);
            }
        });
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE)+1;

        modalitaLettura = false;
        // Otteniamo le informazioni associate all'Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            Log.d(TAG_LOG,"visualizza parametri ricevuti.");
            modalitaLettura = extras.getBoolean(CarpoolingApplication.READMODE);
            if(extras.getString(CarpoolingApplication.TYPE_KEY).equals(CarpoolingApplication.TYPE_1))
                spinner_tipo.setSelection(SPINNER_ITEM_1);
            else
                spinner_tipo.setSelection(SPINNER_ITEM_2);
            spinner_tipo.setClickable(false);
            spinner_posti.setSelection(extras.getInt(CarpoolingApplication.POSTIFREE_KEY));
            spinner_posti.setClickable(false);
            btnTime.setClickable(false);
            btnDate.setClickable(false);
            txtFrom.setClickable(false);
            txtTo.setClickable(false);
            txtFrom.setText(extras.getString(CarpoolingApplication.FROM_KEY));
            txtTo.setText(extras.getString(CarpoolingApplication.TO_KEY));
            btnDate.setText(extras.getString(CarpoolingApplication.DATE_KEY));
            btnTime.setText(extras.getString(CarpoolingApplication.TIME_KEY));
        }
        if(!modalitaLettura){
            Log.d(TAG_LOG,"setta la data corrente");
            // get the current date and hourOfDay
            updateDisplay(DIALOG_DATE_ID);
            updateDisplay(DIALOG_HOUR_ID);
            spinner_posti.setClickable(true);
            btnTime.setClickable(true);
            btnDate.setClickable(true);
            txtFrom.setClickable(true);
            txtTo.setClickable(true);
        }
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
        if(!modalitaLettura)
		    inflater.inflate(R.menu.pubblica_post, menu);
		return super.onCreateOptionsMenu(menu);
	}
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId())
		{
			case SAVE_MENU_OPTION:
				saveData();
				break;
			case CANCELS_MENU_OPTION:
				this.finish();
				break;
			default:
				return super.onContextItemSelected(item);
		}
		return  true;
	}

	/**
	 * Passa i dati come risultato dell'activity, tramite stringa JSON.
	 */
	private void saveData() {
		if(txtFrom.getText().toString() == null || txtTo.length() == 0)  {
			Toast.makeText(InsertTripActivity.this, "Completare tutti i campi per favore.", Toast.LENGTH_SHORT).show();
			return;
		}

        if(type_request.equals(CarpoolingApplication.TYPE_1)) {
                if(ControlloDisponibilita()){
                    new AlertDialog.Builder(InsertTripActivity.this)
                            .setTitle("Info")
                            .setMessage("Ho trovato un tuo amico che fa la tua stessa strada, vuoi chiedergli un passaggio?")
                            .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //cambia il tipo in "Richiesta di passaggio"
                                    type_request = CarpoolingApplication.TYPE_2;
                                    Log.d(TAG_LOG,"Manda la richiesta di passaggio.");
                                    inviaPost();
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Log.d(TAG_LOG, "Richiesta non inviata. Pubblicare comunque il post?");
                                    inviaPost();
                                }
                            }).setCancelable(false).show();
                } else
                    inviaPost();
        } else
            inviaPost();
	}

    private void inviaPost() {
        new AlertDialog.Builder(this)
                .setTitle("Vuoi continuare?")
                .setMessage(getString(R.string.publishing_post))
                .setPositiveButton("Invia", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            //Stringa generata dal sistema che verrà pubblicata su facebook
                            /*String string_post = "{"+type_request+
                                    "[from="+txtFrom.getText().toString().trim()+
                                    "][to="+txtTo.getText().toString().trim()+
                                    "][date="+btnDate.getText()+
                                    "][time="+btnTime.getText()+"]}"; */
                            JSONObject json = new JSONObject()
                                    .put(CarpoolingApplication.TYPE_KEY, type_request)
                                    .put(CarpoolingApplication.FROM_KEY, txtFrom.getText().toString().trim())
                                    .put(CarpoolingApplication.TO_KEY, txtTo.getText().toString().trim())
                                    .put(CarpoolingApplication.DATE_KEY, btnDate.getText().toString())
                                    .put(CarpoolingApplication.TIME_KEY, btnTime.getText().toString())
                                    .put(CarpoolingApplication.POSTIFREE_KEY, numPostiLiberi);
                                    //.put(CarpoolingApplication.STRING_POST, string_post);

                            Intent resultData = getIntent().putExtra(CarpoolingApplication.BUNDLE_DISPOSREQUEST_ACTIVITY, json.toString());
                            setResult(RESULT_OK, resultData);
                            Log.d(TAG_LOG, "Invio post in corso...");
                            Toast.makeText(InsertTripActivity.this, "Invio post in corso.", Toast.LENGTH_SHORT).show();
                            InsertTripActivity.this.finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG_LOG, "Invio post annulato.");
                    }
                }).show();
    }

    /*
    * Se c'è già un utente che fa la mia stessa strada, gli mando una richiesta.
     */
    private boolean ControlloDisponibilita() {
        CarpoolingApplication app = new CarpoolingApplication(Session.getActiveSession(), this);
        if(app.controlloDisponibilita(txtFrom.getText().toString(),txtTo.getText().toString(),btnDate.getText().toString(),btnTime.getText().toString())) {
             return true;
        } else {
            Log.d(TAG_LOG, "Non è stata trovata nessuna disponibilità.");
            return false;
        }
    }

    public void onClickButtonOra(View v) {
		Log.d(TAG_LOG,"Visualizza Dialog DatePicker");
		//Visualizza la finestra di dialogo
		this.showDialog(DIALOG_HOUR_ID);
	}

	public void onClickButtonData(View v) {
		Log.i(TAG_LOG, "Visualizza Dialog DatePicker");
		//Visualizza la finestra di dialogo
		this.showDialog(DIALOG_DATE_ID);
	}

	/**
	 * Creo una finistra di dialogo per la data e l'ora
	 */
	protected Dialog onCreateDialog(int id) {
		Log.i(TAG_LOG,"Creazione del Dialog DataPicker");
		switch(id)
		{
			case DIALOG_DATE_ID:
				return new DatePickerDialog(this, mydataSetListener, mYear, mMonth, mDay);
			case DIALOG_HOUR_ID:
				return new TimePickerDialog(this, myTimeSetListener, mHour, mMinute, true);
			default:
				return super.onCreateDialog(id);
		}
	}

	/**
	 *  Crea dataset per impostare la data
	 */
	private DatePickerDialog.OnDateSetListener mydataSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int month, int day) {
			mYear = year;
			mMonth = month;
			mDay = day;
			updateDisplay(DIALOG_DATE_ID);
		}

	};

	/**
	 *  Crea dataset per impostare l'orario
	 */
	private TimePickerDialog.OnTimeSetListener myTimeSetListener = new TimePickerDialog.OnTimeSetListener(){

		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mHour = hourOfDay;
			mMinute = minute;
			updateDisplay(DIALOG_HOUR_ID);
		}

	};

	/**
	 * Aggiorna la data nella TextView
	 * @param id dialog_id data/ora
	 */
	private void updateDisplay(int id) {
		StringBuilder s;
		switch(id){
			case DIALOG_DATE_ID:
				Log.d(TAG_LOG,"Aggiornamento della data");
				s = new StringBuilder()
						// Month is 0 based so add 1
						.append(mDay).append("/")
						.append(mMonth+1).append("/")
						.append(mYear);
				btnDate.setText(s);
				break;
			case DIALOG_HOUR_ID:
				Log.d(TAG_LOG,"Aggiornamento dell'ora");
				s = new StringBuilder();
                if(mMinute<10) s.append(mHour).append(":0").append(mMinute);
                else s.append(mHour).append(":").append(mMinute);
				btnTime.setText(s);
				break;
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 && !modalitaLettura) {
			Log.d(TAG_LOG, "MyTab back");
			saveData();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}