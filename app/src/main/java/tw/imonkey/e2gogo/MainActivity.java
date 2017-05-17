package tw.imonkey.e2gogo;

import android.app.Activity;
import android.content.Context;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.os.Bundle;

import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


public class MainActivity extends Activity {

    String memberEmail,myDeviceId,shopDeviceId;//deviceId=topicId=ShopId
    FirebaseAuth mAuth;
    DatabaseReference mUserFile;
    FirebaseAuth.AuthStateListener mAuthListener;
    ListView LVShop;
    FirebaseListAdapter mLVShopAdapter;
    StorageReference mImageRef;
    public static final String devicePrefs = "devicePrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
        SharedPreferences settings = getSharedPreferences(devicePrefs, Context.MODE_PRIVATE);
        myDeviceId = settings.getString("deviceId",null);
        FirebaseMessaging.getInstance().subscribeToTopic(myDeviceId);

        if (!checkInternetConenction()){
            Toast.makeText(MainActivity.this,"網路沒開?手機失聯",Toast.LENGTH_LONG).show();
        }

        memberCheck();

    }

    @Override
    protected void onStart() {
        super.onStart();
       mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if( mLVShopAdapter!=null) {
            mLVShopAdapter.cleanup();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAuth.removeAuthStateListener(mAuthListener);
        if( mLVShopAdapter!=null) {
            mLVShopAdapter.cleanup();
        }
    }

    private void memberCheck() {
        //initializing firebase auth object
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    memberEmail = user.getEmail();
                    if (myDeviceId==null){
                        mUserFile= FirebaseDatabase.getInstance().getReference("/GUEST/" +memberEmail.replace(".", "_")+"/PFILE/");
                        myDeviceId =mUserFile.push().getKey();
                        Map<String, Object> addGUEST = new HashMap<>();
                        addGUEST.put("memberEmail",memberEmail);
                        addGUEST.put("deviceId",myDeviceId);
                        addGUEST.put("timeStamp", ServerValue.TIMESTAMP);
                        mUserFile.setValue(addGUEST);
                        SharedPreferences.Editor editor = getSharedPreferences(devicePrefs, Context.MODE_PRIVATE).edit();
                        editor.putString("deviceId",myDeviceId);
                        editor.putString("memberEmail",memberEmail);
                        editor.apply();
                        FirebaseMessaging.getInstance().subscribeToTopic(myDeviceId);
                    }
                   getShop();
                }
            }
        };
    }


    private void getShop() {
        Query refShop = FirebaseDatabase.getInstance().getReference("/SHOPPING/" + memberEmail.replace(".", "_")).limitToLast(10);
        LVShop = (ListView) findViewById(R.id.listViewShop);
        mLVShopAdapter = new FirebaseListAdapter<Shop>(this, Shop.class, R.layout.listview_device_layout, refShop) {

            @Override
            protected void populateView(View view, Shop shop, int position) {
                ((TextView) view.findViewById(R.id.deviceName)).setText(shop.getCompanyId() + "." + shop.getDevice() + ":" + shop.getDescription());
                if (shop.getAlert().get("message") != null) {
                    Calendar timeStamp = Calendar.getInstance();
                    timeStamp.setTimeInMillis(Long.parseLong(shop.getAlert().get("timeStamp").toString()));
                    SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss MM/dd", Locale.TAIWAN);
                    ((TextView) view.findViewById(R.id.deviceMessage)).setText(shop.getAlert().get("message").toString() + "\n" + df.format(timeStamp.getTime()));
                    ((TextView) view.findViewById(R.id.textViewDeviceType)).setText(df.format(timeStamp.getTime())+"  "+shop.getDeviceType());
                } else {
                    ((TextView) view.findViewById(R.id.deviceMessage)).setText("");
                    ((TextView) view.findViewById(R.id.textViewDeviceType)).setText(shop.getDeviceType());
                }
                String devicePhotoPath = "/devicePhoto/" + shop.getTopics_id();
                mImageRef = FirebaseStorage.getInstance().getReference(devicePhotoPath);
                ImageView imageView = (ImageView) view.findViewById(R.id.deviceImage);
                Glide.with(MainActivity.this)
                        .using(new FirebaseImageLoader())
                        .load(mImageRef)
                        .into(imageView);
            }
        };
        LVShop.setAdapter(mLVShopAdapter);

        LVShop.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                shopDeviceId = mLVShopAdapter.getRef(position).getKey();//deviceId=topicId=ShopId
                mLVShopAdapter.getRef(position).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String deviceType=snapshot.child("deviceType").getValue().toString();
                        Intent intent ;
                        if (deviceType.equals("主機")) {
                            intent = new Intent(MainActivity.this, GUESTActivity.class);
                            intent.putExtra("deviceId", shopDeviceId);
                            intent.putExtra("memberEmail", memberEmail);
                            startActivity(intent);
                        }else if(deviceType.equals("取號機")){
                            intent = new Intent(MainActivity.this, QMSActivity.class);
                            intent.putExtra("deviceId", shopDeviceId);
                            intent.putExtra("memberEmail", memberEmail);
                            startActivity(intent);
                        }else if(deviceType.equals("打卡機")){
                            intent = new Intent(MainActivity.this,TCActivity.class);
                            intent.putExtra("deviceId", shopDeviceId);
                            intent.putExtra("memberEmail", memberEmail);
                            startActivity(intent);
                        }else if(deviceType.equals("集點機")){
                            intent = new Intent(MainActivity.this, POINTSActivity.class);
                            intent.putExtra("deviceId", shopDeviceId);
                            intent.putExtra("memberEmail", memberEmail);
                            startActivity(intent);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });

                Intent intent = new Intent(MainActivity.this, QMSActivity.class);
                intent.putExtra("deviceId", shopDeviceId);
                startActivity(intent);

            }
        });
         /*
        if ( LVShop.getAdapter().getCount()==0){
            Toast.makeText(MainActivity.this,"初次使用,請按紅色+掃描QRcode",Toast.LENGTH_LONG).show();
        }
        */
        refShop.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
              if(snapshot.getValue()==null){
                  Toast.makeText(MainActivity.this,"初次使用,請按黃色按鈕掃描QRcode",Toast.LENGTH_LONG).show();
              }
            }
            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    public void scanQR(View v) {

        if (checkInternetConenction()){
            Intent intent = new Intent(MainActivity.this,  QRActivity.class);
            intent.putExtra("memberEmail", memberEmail);
            startActivity(intent);
            finish();
        }else {
            Toast.makeText(MainActivity.this,"網路沒開?手機失聯!",Toast.LENGTH_LONG).show();
        }

    }

    public boolean checkInternetConenction() {
        // get Connectivity Manager object to check connection
        ConnectivityManager CM= (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = CM.getActiveNetworkInfo();
        return (activeNetwork != null) ;
    }
}
