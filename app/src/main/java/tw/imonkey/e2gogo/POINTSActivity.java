package tw.imonkey.e2gogo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class POINTSActivity extends AppCompatActivity {
    public static final String devicePrefs = "devicePrefs";
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;
    DatabaseReference mPOINTS, mDevice,mUsed;
    String memberEmail,deviceId,ACT;
    Integer buyTimes;
    TextView TVPoints,TVDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_points);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Bundle extras = getIntent().getExtras();
        deviceId = extras.getString("deviceId");
        SharedPreferences settings = getSharedPreferences(devicePrefs, Context.MODE_PRIVATE);
        ACT = settings.getString("ACT", null);
        TVPoints = (TextView) findViewById(R.id.textViewPoints);
        TVDescription = (TextView) findViewById(R.id.textViewDescription);
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    memberEmail = user.getEmail();
                    mDevice = FirebaseDatabase.getInstance().getReference("/DEVICE/" + deviceId);
                    mDevice.child("DESCRIPTION").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if(snapshot.getValue()!=null) {
                                TVDescription.setText(snapshot.getValue().toString());
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });

                    mDevice.child("BUYTIMES").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.getValue()!=null) {
                                buyTimes = (int) snapshot.getValue();
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                    mDevice.child("ACT").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.getValue()!=null) {
                                ACT =snapshot.getValue().toString();
                                getPoints();
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                }
            }
        };
    }

    private void getPoints(){
        mPOINTS = FirebaseDatabase.getInstance().getReference("/LOG/POINTS/" + deviceId + "/" + ACT + "/");
        mPOINTS.orderByChild("memberEmail").equalTo(memberEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    if (childSnapshot.child("USED").getValue() == null) {
                        count++;
                    }
                }
                TVPoints.setText(String.valueOf(count));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
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

    public void onClickChangePoints(View v) {
        if(buyTimes!=null && buyTimes<=(Integer.parseInt(TVPoints.getText().toString()))){
            mUsed=FirebaseDatabase.getInstance().getReference("/LOG/POINTS/" + deviceId + "/" + ACT + "/");
            mUsed.orderByChild("memberEmail").equalTo(memberEmail).limitToFirst(buyTimes).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        mDevice.child(childSnapshot.getKey()).child("USED").setValue(true);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });

            TVPoints.setText(Integer.parseInt(TVPoints.getText().toString())-buyTimes);
            DatabaseReference mPointChanged=FirebaseDatabase.getInstance().getReference("/DEVICE/"+ deviceId+"/CHANGED");
            Map<String, Object> data = new HashMap<>();
            data.clear();
            data.put("message","ACT:"+ACT+","+"POINTS:"+buyTimes);
            data.put("memberEmail",memberEmail);
            data.put("timeStamp", ServerValue.TIMESTAMP);
            mPointChanged.push().setValue(data);
        }
    }
}
