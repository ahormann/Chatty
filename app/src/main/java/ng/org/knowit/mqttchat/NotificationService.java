package ng.org.knowit.mqttchat;

import android.app.AlarmManager;
import android.app.Service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import org.eclipse.paho.client.mqttv3.MqttClient;

import static ng.org.knowit.mqttchat.MainActivity.CHANNEL_ID;

public class NotificationService extends Service {
    private String ACTION_STOP = "ACTION_STOP";
    private Handler mHandler = new Handler();
    private long frequency = 5 * 1000; // ms between location-publications
    private LocationManager locationManager;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();
    // Registered callbacks
    private ServiceCallback serviceCallback;

    // Class used for the client Binder.
    public class LocalBinder extends Binder {
        NotificationService getService() {
            // Return this instance of MyService so clients can call public methods
            return NotificationService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void setCallbacks(ServiceCallback callback) {
        serviceCallback = callback;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction() != null) {
                Log.d("NotificationService", intent.getAction());
                handleAction(intent.getAction());
            } else {
                return createService();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void handleAction(String action) {
        if (action.equals(ACTION_STOP)) {
            if (serviceCallback != null) {
                serviceCallback.stopService();
            } else {
                Log.e("NotificationService", "No callback exists!");
            }
        }
    }

    private int createService() {

        Intent stopIntent = new Intent(this, NotificationService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent pendingPlayIntent = PendingIntent.getService(this, 0, stopIntent, 0);
        NotificationCompat.Action stopAction = new NotificationCompat.Action(android.R.drawable.ic_menu_delete, "Stop", pendingPlayIntent);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_content))
                .setSmallIcon(R.drawable.bubble_circle)
                .setContentIntent(pendingIntent)
                .addAction(stopAction)
                //.setTicker(getText(R.string.ticker_text))
                .build();

        startForeground(1, notification);

        // run LocationRunnable every 'frequency' milliseconds

        //mHandler.postDelayed( LocationRunnable, frequency);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    final Runnable LocationRunnable = new Runnable() {
        public void run() {
            Toast.makeText(getApplicationContext(), "Sending Location",
                    Toast.LENGTH_LONG).show();

        mHandler.postDelayed(LocationRunnable, frequency);
        }
    };
}