package tw.imonkey.e2gogo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QRActivity extends AppCompatActivity {
    BarcodeDetector barcodeDetector;
    CameraSource cameraSource;
    SurfaceView cameraView;
    TextView barcodeValue;
    public static final String devicePrefs = "devicePrefs";
    DatabaseReference mMQSClient,mTCClient,mDevice,mAddClub;
    Map<String, Object> client = new HashMap<>();
    String memberEmail,deviceId,key,message,service;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(tw.imonkey.e2gogo.R.layout.activity_qr);
        Bundle extras = getIntent().getExtras();
        memberEmail = extras.getString("memberEmail");
        cameraView = (SurfaceView) findViewById(tw.imonkey.e2gogo.R.id.camera_view);
        barcodeValue = (TextView) findViewById(tw.imonkey.e2gogo.R.id.code_info);
        takeCard();
    }
    public void takeCard(){

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        if (!barcodeDetector.isOperational()) {
            barcodeValue.setText("Could not set up the detector!");
            return;
        }

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .setAutoFocusEnabled(true) //you should add this feature
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    //noinspection MissingPermission
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    barcodeValue.post(new Runnable() {
                        @Override
                        public void run() {
                            barcodeDetected(barcodes.valueAt(0).displayValue);
                            cameraSource.stop();
                        }
                    });
                }
            }
        });
    }

    private void barcodeDetected(String normalTextEnc){
        // encrypt and decrypt using AES Algorithms
        String normalText="" ;

        try {
            String seedValue = "imonkey.tw";
            normalText =AESHelper.decrypt(seedValue,normalTextEnc);
        } catch (Exception e) {
            e.printStackTrace();
            barcodeValue.setText("err");
        }
        //    normalText=normalTextEnc;
            barcodeValue.setText(normalText);
            deviceId = normalText.split(":")[0];
            service =normalText.split(":")[1];
            message =normalText.split(":")[2];
            key =normalText.split(":")[3];
            barcodeValue.setText(message);

            if (service.equals("QMS")) {
                QR2QMS();
            }
            if (service.equals("TC")) {
                QR2TC();
            }
    }

    private void QR2QMS(){
        mMQSClient= FirebaseDatabase.getInstance().getReference(service+"/"+deviceId+"/CLIENT/");
        client.clear();
        client.put("message",Integer.parseInt(message)+1);
        client.put("memberEmail",memberEmail);
        client.put("timeStamp", ServerValue.TIMESTAMP);
        mMQSClient.child(key).setValue(client);
        SharedPreferences.Editor editor = getSharedPreferences(devicePrefs, Context.MODE_PRIVATE).edit();
        editor.putString(deviceId+":message",message);
        editor.apply();
        mDevice= FirebaseDatabase.getInstance().getReference("/DEVICE/"+deviceId);
        mAddClub=FirebaseDatabase.getInstance().getReference("/CLUB/" + memberEmail.replace(".", "_") + "/SHOP/");
        mDevice.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()){
                    Shop shop=snapshot.getValue(Shop.class);
                    mAddClub.child(shop.getTopics_id()).setValue(shop);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        Intent intent = new Intent(this, QMSActivity.class);
        intent.putExtra("deviceId", deviceId);
        startActivity(intent);
        finish();
    }

    private void QR2TC(){
        mTCClient= FirebaseDatabase.getInstance().getReference(service+"/"+deviceId+"/CLIENT/");
        if (message.equals("A")){
            message="上班" ;
        }else if(message.equals("B")){
            message="下班" ;
        }else if(message.equals("C")){
            message="加班" ;
        }else if(message.equals("D")){
            message="結束" ;
        }else{
            message="異常";
        }
        client.clear();
        client.put("message",message);
        client.put("memberEmail",memberEmail);
        client.put("timeStamp", ServerValue.TIMESTAMP);
        mTCClient.child(key).setValue(client);
        SharedPreferences.Editor editor = getSharedPreferences(devicePrefs, Context.MODE_PRIVATE).edit();
        editor.putString(deviceId+":message",message);
        editor.apply();
        mDevice= FirebaseDatabase.getInstance().getReference("/DEVICE/"+deviceId);
        mAddClub=FirebaseDatabase.getInstance().getReference("/CLUB/" + memberEmail.replace(".", "_") + "/SHOP/");
        mDevice.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()){
                    Shop shop=snapshot.getValue(Shop.class);
                    mAddClub.child(shop.getTopics_id()).setValue(shop);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        Intent intent = new Intent(this, TCActivity.class);
        intent.putExtra("deviceId", deviceId);
        startActivity(intent);
        finish();
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSource!=null) {
            cameraSource.release();
        }
        if (barcodeDetector!=null) {
            barcodeDetector.release();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        finish();
    }
}
