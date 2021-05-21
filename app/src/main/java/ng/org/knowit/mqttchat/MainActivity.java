package ng.org.knowit.mqttchat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.eclipse.paho.android.service.MqttAndroidClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ng.org.knowit.mqttchat.Adapter.ChooseTopicListener;
import ng.org.knowit.mqttchat.Adapter.SuggestionAdapter;


public class MainActivity extends AppCompatActivity {

    private TextInputEditText topic_send_textbox, topic_recieve_textbox;

    private String clientId;

    private MqttAndroidClient client;

    private ArrayList<String> topicSendingList, topicRecievingList;
    private Map<String, String> topicSendingDict, topicRecievingDict;


    private RecyclerView sendTopicRecyclerView, recieveTopicRecyclerView;
    private SuggestionAdapter sendTopicAdapter, recieveTopicAdapter;


    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String CHANNEL_ID = "exampleServiceChannel";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        topicSendingList = new ArrayList<>();
        topicSendingList.add("send_topic_1");
        topicSendingList.add("send_topic_2");
        topicSendingList.add("test");

        topicRecievingList = new ArrayList<>();
        topicRecievingList.add("recieve_topic_1");
        topicRecievingList.add("recieve_topic_2");
        topicRecievingList.add("#");

        final ChooseTopicListener sendTopicChooser = new ChooseTopicListener(topicSendingList);
        final ChooseTopicListener recieveTopicChooser = new ChooseTopicListener(topicRecievingList);

        sendTopicAdapter = new SuggestionAdapter(this, topicSendingList, sendTopicChooser);
        sendTopicRecyclerView = findViewById(R.id.send_topics);
        sendTopicRecyclerView.setAdapter(sendTopicAdapter);
        sendTopicRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        sendTopicRecyclerView.setVisibility(View.VISIBLE);


        recieveTopicAdapter = new SuggestionAdapter(this, topicRecievingList, recieveTopicChooser);
        recieveTopicRecyclerView = findViewById(R.id.recieve_topics);
        recieveTopicRecyclerView.setAdapter(recieveTopicAdapter);
        recieveTopicRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        recieveTopicRecyclerView.setVisibility(View.VISIBLE);

        /*
        if (isOnline()){
            Fabric.with(this, new Crashlytics());
            connect();
        } else {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
        }
        chatIdInputLayout = findViewById(R.id.chatIdTextInputLayout);

        */
        Button chatButton = findViewById(R.id.chatButton);
        topic_send_textbox = findViewById(R.id.topic_send_manual);
        topic_recieve_textbox = findViewById(R.id.topic_recieve_manual);

        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String topic_send = "test";
                String topic_recieve = "test";

                String send_editTextString = topic_send_textbox.getText().toString();
                if (! send_editTextString.isEmpty()) {
                    topic_send = send_editTextString;
                } else {
                    String choice = sendTopicChooser.getChoice();
                    if (choice != null) {
                        topic_send = choice;
                    }
                }

                String recieve_editTextString = topic_recieve_textbox.getText().toString();
                if (! recieve_editTextString.isEmpty()) {
                    topic_recieve = recieve_editTextString;
                } else {
                    String choice = recieveTopicChooser.getChoice();
                    if (choice != null) {
                        topic_recieve = choice;
                    }
                }


                /*boolean noError = true;
                String editTextString = chatIdInputLayout.getEditText().getText().toString();
                if (editTextString.isEmpty()) {
                    chatIdInputLayout.setError(getResources().getString(R.string.error_string));
                    noError = false;
                } else {
                    chatIdInputLayout.setError(null);
                }*/


                //subscribe(editTextString.toLowerCase().trim());
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);


                intent.putExtra("subscribeTo", topic_recieve);
                intent.putExtra("sendTo", topic_send);
                //Toast.makeText(MainActivity.this, "Subcribed to "+ (editTextString.toLowerCase()), Toast.LENGTH_SHORT).show();

                startActivity(intent);


            }

        });

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Example Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

}

