package ng.org.knowit.mqttchat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
//import com.google.android.gms.tasks.OnFailureListener;
//import com.google.android.gms.tasks.OnSuccessListener;
/*import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseSmartReply;
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage;
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestion;
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestionResult;*/
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import ng.org.knowit.mqttchat.Adapter.SuggestionAdapter;
import ng.org.knowit.mqttchat.Models.Message;
import ng.org.knowit.mqttchat.Models.User;

public class ChatActivity extends AppCompatActivity implements SuggestionAdapter.OnListItemClickListener, LocationListener, ServiceCallback {

    MessagesList mMessagesList;

    private MessageInput mMessageInput;
    private TextView mTopicView;

    private String clientId, topic_send, topic_recieve;

    private MqttAndroidClient client;

    private MessagesListAdapter<Message> sendMessageAdapter;

    private ArrayList<String> commandList, keyboard;
    boolean keyboardAreCommands = true;
    private Map<String, String> commandDict, keyboardDict;
    private RecyclerView mRecyclerView;

    private String usedProvider;
    private String nextMessagePrefix = ""; // keep empty unless you want to edit next message

    private SuggestionAdapter mCommandSendAdapter;

    private ImageLoader mImageLoader;
    private static final String TAG = ChatActivity.class.getSimpleName();

    private User self_user, other_user;
    private long messageIdLong;

    private ServiceConnection serviceConnection;
    private NotificationService notificationService;
    private boolean boundNotificationService = false;
    private LocationManager locationManager;
    private long frequency = 30 * 1000; // ms between location-publications
    private float minDist = 20; // minimum difference between locations in meter to trigger update

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //mFirebaseSmartReply = FirebaseNaturalLanguage.getInstance().getSmartReply();

        mMessagesList = new MessagesList(this);
        mMessagesList = findViewById(R.id.messagesList);
        mMessageInput = findViewById(R.id.message_input);
        mTopicView = findViewById(R.id.topicView);

        mRecyclerView = findViewById(R.id.suggestionRecyclerView);

        //mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        // Dict with name of command in frontend as key and command sent to mqtt as value
        commandDict = new HashMap<String, String>();
        commandDict.put("commandToShow1", "/commandToSend1");
        commandDict.put("commandToShow2", "/commandToSend2");
        commandDict.put("commandToShow3", "/commandToSend3");
        commandDict.put("commandToShow4", "/commandToSend4");
        commandDict.put("#location", "#location");
        commandDict.put("#stopLocation", "#stopLocation");
        commandDict.put("#markLocation", "#markLocation");

        keyboardDict = new HashMap<String, String>();
        cloneDict(commandDict, keyboardDict);

        commandList = new ArrayList<>();
        commandList.addAll(commandDict.keySet());
        commandList.remove("#stopLocation");
        commandList.remove("#markLocation");

        keyboard = new ArrayList<>();
        keyboard.addAll(commandList);

        mCommandSendAdapter = new SuggestionAdapter(this, keyboard, ChatActivity.this);

        mRecyclerView.setAdapter(mCommandSendAdapter);
        mRecyclerView.setVisibility(View.VISIBLE);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //mRecyclerView.setVisibility(View.INVISIBLE);

        messageIdLong = 0;

        Intent intent = getIntent();
        topic_recieve = "test";
        topic_send = "test";
        if (intent != null && intent.hasExtra("subscribeTo")) {
            String top = intent.getStringExtra("subscribeTo");
            if (top != null) {
                topic_recieve = top;
            }
        }
        if (intent != null && intent.hasExtra("sendTo")) {
            String top = intent.getStringExtra("sendTo");
            if (top != null) {
                topic_send = top;
            }
        }

        mTopicView.setText("Send to " + topic_send + " and recieve from " + topic_recieve + ".");

        mImageLoader = new ImageLoader() {
            @Override
            public void loadImage(ImageView imageView, @Nullable String url,
                                  @Nullable Object payload) {
                Glide.with(ChatActivity.this).load(url).into(imageView);
            }
        };


        self_user = new User("John", "df", "avatar");
        other_user = new User("Paul", "df", null);
        sendMessageAdapter = new MessagesListAdapter<>("John", mImageLoader);
        mMessagesList.setAdapter(sendMessageAdapter);

        mMessageInput.setInputListener(new MessageInput.InputListener() {
            @Override
            public boolean onSubmit(CharSequence input) {
                return submitMessage(input);
            }
        });


        clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://broker.hivemq.com:1883",
                clientId);

        if (isOnline()) {
            boolean success = connect();
        } else {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
        }





        /** Callbacks for service binding, passed to bindService() */
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                // cast the IBinder and get MyService instance
                NotificationService.LocalBinder binder = (NotificationService.LocalBinder) service;
                notificationService = binder.getService();
                boundNotificationService = true;
                notificationService.setCallbacks(ChatActivity.this); // register
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                boundNotificationService = false;
            }
        };

    }

    private boolean submitMessage(CharSequence input) {

        if (!input.toString().trim().isEmpty()) {
            if (input.toString().startsWith("#")) {
                processCommand(input.toString());
            } else {

                //Add the generated character to the end of the message input
                String payload = input.toString();// + generatedChar;
                byte[] encodedPayload = new byte[0];
                try {
                    encodedPayload = payload.getBytes("UTF-8");
                    MqttMessage mqttmessage = new MqttMessage(encodedPayload);

                    if (client.isConnected()) {
                        //Publish to a specific topic; in this case Unique chat ID
                        client.publish(topic_send, mqttmessage);
                        //Toast.makeText(ChatActivity.this, topic + ", " + mqttmessage + ", " + client.getServerURI() , Toast.LENGTH_SHORT).show();
                    }
                    sendTextToUser(input.toString(), self_user);


                    //Add the message to conversation history for Firebase smart reply
                    //mFirebaseTextMessages.add(FirebaseTextMessage.createForLocalUser(input.toString(), System.currentTimeMillis()));
                } catch (UnsupportedEncodingException | MqttException e) {
                    sendTextToUser(e.getMessage(), other_user);
                    e.printStackTrace();
                }
            }

        } else {
            Toast.makeText(ChatActivity.this, "You can't send empty message", Toast.LENGTH_SHORT).show();
        }


        return true;
    }

    private void processCommand(String command) {
        if (command.equals("#location")) {
            Map<String, String> buttonDict = new HashMap<String, String>();

            for (String prov : locationManager.getAllProviders()) {
                buttonDict.put("#" + prov, "#" + prov);
            }
            keyboardAreCommands = false;
            setKeyboard(buttonDict);
        }
        if (command.equals("#stopLocation")) {
            stopService();
        }

        for (String prov : locationManager.getAllProviders()) {
            if (command.contains(prov)) {
                // edit keyboard
                commandList.remove("#location");
                commandList.add("#stopLocation");
                commandList.add("#markLocation");
                resetKeyboard();

                boolean success = startService(prov);
                if (success) {
                    usedProvider = prov;
                }
            }
        }

        if (command.equals("#markLocation")) {
            sendTextToUser("Describe where you are", other_user);
            nextMessagePrefix = "#favLocAnswer:\n";
        }

        if (command.startsWith("#favLocAnswer:\n")) {
            // check if user granted permission
            if (!hasLocationPermission()) {
                return;
            }
            Location loc = locationManager.getLastKnownLocation(usedProvider);
            String answer = command.substring(15); // everything after #favLocAnswer:
            sendLocation(loc, true, answer);
        }

    }

    private boolean hasLocationPermission() {
        boolean perm = ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!perm) {
            sendTextToUser("Permission for Location required!", other_user);
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
            try {
                ActivityCompat.requestPermissions(this, permissions, 347863345);
            } catch (Exception e) {
                sendTextToUser("Asking for Permission failed, set it manually!", other_user);
            }
        }
        return perm;
    }

    public boolean startService(String provider) {
        // check if user granted permission
        if (!hasLocationPermission()) {
            return false;
        }
        // create notification to be able to use foreground location
        Intent serviceIntent = new Intent(this, NotificationService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        //bind to notification service to enable it calling stopService
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // request regular location updates
        Log.i("Location", locationManager.getAllProviders().toString());
        Log.i("Location", locationManager.isLocationEnabled() ? "Enabled" : "Disabled");
        Location loc = locationManager.getLastKnownLocation(provider);


        /*if (loc == null) {
            sendTextToUser("Location is null, stopping service", other_user);
            stopService();
            return false;
        }*/


        //locationManager.getCurrentLocation();

        //locationManager.requestSingleUpdate(LocationManager.PASSIVE_PROVIDER, this, null);

        locationManager.requestLocationUpdates(provider, frequency, minDist, this);
            /*
            locationManager.requestLocationUpdates(provider, frequency, minDist, this);
            Log.i("Location", locationManager.getAllProviders().toString());
            Log.i("Location", locationManager.isLocationEnabled() ? "Enabled" : "Disabled");
            Location loc = locationManager.getLastKnownLocation(provider);
            if (loc != null) {
                Log.i("Location", loc.toString());
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, frequency, minDist, this);
            } else {
                sendTextToUser("GPS returned Null, using network...", other_user);
                loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, frequency, minDist, this);
            }
            */
        onLocationChanged(loc);

        return true;

    }

    public void stopService() {
        // remove notification
        Intent serviceIntent = new Intent(this, NotificationService.class);
        stopService(serviceIntent);
        // Unbind from service
        if (boundNotificationService) {
            notificationService.setCallbacks(null); // unregister
            unbindService(serviceConnection);
            boundNotificationService = false;
        }

        // edit keyboard
        commandList.remove("#stopLocation");
        commandList.remove("#markLocation");
        if (! commandList.contains("#location")) {
            commandList.add("#location");
        }
        resetKeyboard();

        // reload keyboard
        mCommandSendAdapter.notifyDataSetChanged();
        mRecyclerView.setVisibility(View.VISIBLE);

        //disable location-update
        locationManager.removeUpdates(this);
    }

    private void subscribe( String topic){
        int qos = 1;
        try {
            IMqttToken subToken = client.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // The message was published
                    Log.d(TAG, "Subscription successful");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                        Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards
                    sendTextToUser( "Failed to subscribe", other_user);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            sendTextToUser( "Failed to connect", other_user);
            Toast.makeText(ChatActivity.this, "Failed to connect", Toast.LENGTH_LONG).show();
        }

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                connect();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //Receiver will receive from publisher topic so it has to be subscribed to publisher topic
                //For receiver to receive it has to be subscribed to the sender/publisher topic

                processMessage(message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }


    /**
     * Processes incoming message to display it.
     * @param message
     */
    private void processMessage(MqttMessage message){
        String messageContent = message.toString();
        if (false){
            //do not show message

        } else {
            //show message
            String messageToDisplay;

            try {
                JSONObject json = new JSONObject(message.toString());
                if (json.has("text")) {
                    messageToDisplay = json.getString("text");//.substring(0, messageContent.length()-1);
                } else {
                    messageToDisplay = messageContent;
                }

                if (json.has("keyboard")) {
                    JSONArray buttons = json.getJSONArray("keyboard");
                    Map<String, String> buttonDict = new HashMap<String, String>();

                    keyboard.clear();
                    for (int i = 0; i < buttons.length(); i++)
                    {
                        String btn = buttons.getString(i);
                        buttonDict.put(btn.split(":")[0], btn.split(":")[1]);
                        keyboard.add(btn.split(":")[0]);
                    }
                    //keyboard.addAll(json.getString("keyboard"));
                    keyboardAreCommands = false;
                    //setKeyboard(buttonDict); wrong order

                    cloneDict(buttonDict, keyboardDict);
                    mCommandSendAdapter.notifyDataSetChanged();
                } else {
                    if (!keyboardAreCommands) {
                        resetKeyboard();
                    }
                }
                mRecyclerView.setVisibility(View.VISIBLE);

            } catch (JSONException e)
            {
                //messageToDisplay = "Error, " + e.getMessage() ;

                //assume message is valid plaintext
                messageToDisplay = messageContent;

                if (!keyboardAreCommands) {
                    resetKeyboard();
                }
            }

            sendTextToUser(messageToDisplay, other_user);

        }


    }

    /**
     * Connect with MQTT-server.
     */
    private boolean connect(){

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(TAG, "onSuccess");

                    subscribe(topic_recieve);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d(TAG, "onFailure");
                    sendTextToUser( "Failed to connect", other_user);
                    Toast.makeText(ChatActivity.this, "Failed to connect", Toast.LENGTH_LONG).show();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            sendTextToUser( "Failed to connect", other_user);
            Toast.makeText(ChatActivity.this, "Failed to connect", Toast.LENGTH_LONG).show();
        }

        /*if (! client.isConnected()) {
            sendTextToUser( "Failed to connect", other_user);
            return false;
        }*/
        return true;

    }

   private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    /**
     * Copy content of oldDict to newDict and return newDict.
     * @param oldDict
     * @param newDict
     * @return
     */
    private void cloneDict(Map<String, String> oldDict, Map<String, String> newDict) {
        newDict.clear();
        for (String key : oldDict.keySet()) {
            newDict.put(key, oldDict.get(key));
        }
    }

    /**
     * Set the keyboard (command-chips).
     * @param dict Dict with keys = text in UI
     *             and content = text to send
     */
    private void setKeyboard(Map<String, String> dict) {
        keyboard.clear();
        keyboard.addAll(dict.keySet());
        cloneDict(dict, keyboardDict);
        mCommandSendAdapter.notifyDataSetChanged();
        //mRecyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * Set keyboardDict to commandDict
     * and keyboard to commandList.
     */
    private void resetKeyboard() {
        keyboard.clear();
        keyboard.addAll(commandList);
        cloneDict(commandDict, keyboardDict);
        mCommandSendAdapter.notifyDataSetChanged();
        keyboardAreCommands = true;
        //mRecyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * Handles click on command-chip.
     * Inserts text into textfield and submits.
     */
    @Override
    public void onListItemClick(int position) {

        String selectedText = keyboard.get(position);
        String textToSend = keyboardDict.get(selectedText);

        mMessageInput.getInputEditText().setText(" ");
        mMessageInput.getInputEditText().setText(textToSend);
        mMessageInput.getButton().callOnClick();

        mMessageInput.getInputEditText().setText(nextMessagePrefix);
        nextMessagePrefix = "";
    }

    /**
     * Called by locationManager.
     * Send json with position as mqtt-message to server.
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        sendLocation(location, false, "");
    }

    public double round(double value, int digits) {
        double mod = Math.pow(10, digits);
        return Math.round(value * mod) / mod;
    }

    public void sendLocation(Location location, boolean favorite, String comment) {
        Log.d("Location", "updated");
        if (location == null) {
            sendTextToUser("Location is null", other_user);
            return;
        }

        Log.i("Location", location.toString());

        try {
            JSONObject payload_json = new JSONObject();

            if (favorite) {
                payload_json.put("status", "favorite")
                            .put("comment", comment);
            } else {
                payload_json.put("status", "position");
            }


            payload_json.put("latitude", round(location.getLatitude(), 6)) //precision around 10cm
                        .put("longitude", round(location.getLongitude(), 6)) //precision around 10cm
                        .put("altitude", round(location.getAltitude(), 3)) //precision 1cm
                        .put("speed", round(location.getSpeed(), 3)) //precision 0.001 m/s = 0.0036 kmh
                        .put("accuracy", round(location.getAccuracy(), 3)); //precision 1cm

            String payload = payload_json.toString();

            MqttMessage mqttmessage = new MqttMessage(payload.getBytes("UTF-8"));

            if (client.isConnected()) {
                //Publish to a specific topic; in this case Unique chat ID
                client.publish(topic_send, mqttmessage);
                //Toast.makeText(ChatActivity.this, topic + ", " + mqttmessage + ", " + client.getServerURI() , Toast.LENGTH_SHORT).show();
            }

            sendTextToUser(payload, self_user);

        } catch (UnsupportedEncodingException | MqttException | JSONException e) {
            e.printStackTrace();
            sendTextToUser(e.getMessage(), other_user);
        }

    }

    /**
     * Displays text as chat-message to user.
     * @param text Text to show
     * @param user User sending the message.
     *             e.g. self_user (displayed on right-hand-side)
     *             or other_user (displayed on left-hand-side)
     */
    public void sendTextToUser(String text, User user) {
        Date date = Calendar.getInstance().getTime();
        messageIdLong++;

        Locale current = getResources().getConfiguration().getLocales().get(0);
        String Id = String.format(current, "%09d", messageIdLong);
        Message message = new Message(Id, text, date, user);
        sendMessageAdapter.addToStart(message, true);
    }


    /**
     * Overwrite for LocationListener.
     * @param s
     * @param i
     * @param bundle
     */
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.d("Location","status");
    }

    /**
     * Overwrite for LocationListener.
     * @param s
     */
    @Override
    public void onProviderEnabled(String s) {
        Log.d("Location","provider enabled");
    }

    /**
     * Overwrite for LocationListener.
     * @param s
     */
    @Override
    public void onProviderDisabled(String s) {
        Log.d("Location","provider disabled");
    }
}
