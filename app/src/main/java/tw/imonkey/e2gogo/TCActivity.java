package tw.imonkey.e2gogo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TCActivity extends AppCompatActivity {
    public static final String devicePrefs = "devicePrefs";
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;
    DatabaseReference mTCClient,mTCServer,mTCServerLive;
    TextView TVWork;
    String memberEmail,deviceId,myWork;


    MediaPlayer alertMP3 ;
    Vibrator alertVibrator ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(tw.imonkey.e2gogo.R.layout.activity_tc);
        Bundle extras = getIntent().getExtras();
        deviceId = extras.getString("deviceId");
        SharedPreferences settings = getSharedPreferences(devicePrefs, Context.MODE_PRIVATE);
        myWork = settings.getString(deviceId+":message","工作");
        TVWork= (TextView) findViewById(R.id.textViewWork);

        alertMP3 = MediaPlayer.create(getApplicationContext(), R.raw.jingle);
        alertVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user!=null){
                    memberEmail=user.getEmail();

                    mTCClient= FirebaseDatabase.getInstance().getReference("/LOG/TC/"+deviceId+"/CLIENT");
                    mTCClient.child(memberEmail.replace(".","_")).limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.child("message")!=null){
                                TVWork.setText(myWork);
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {}
                    });
                }


                mTCServer= FirebaseDatabase.getInstance().getReference("/LOG/TC/"+deviceId+"/SERVER");
                mTCServer.limitToLast(1).addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        if(dataSnapshot.child("message")!=null) {
                            if (dataSnapshot.child("message").getValue().toString().equals(myWork)){
                                alertVibrator.vibrate(new long[]{20, 100, 20, 200, 20, 300}, -1);
                                alertMP3.start();
                                addNotification("Hero打卡了!Go");
                            }
                        }
                    }
                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {}
                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });
            }
        };

        mTCServerLive=FirebaseDatabase.getInstance().getReference("/LOG/TC/"+deviceId+"/connection");
        mTCServerLive.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

    }

    public void addNotification(String message){
        final int notifyID = 1;
        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final Notification notification = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_launcher)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setContentText(message)
                .build();
        notificationManager.notify(notifyID, notification);

    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAuth.removeAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        finish();
    }
}