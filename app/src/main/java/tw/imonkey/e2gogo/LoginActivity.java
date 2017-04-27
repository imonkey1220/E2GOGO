package tw.imonkey.e2gogo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
//    private static final String TAG = LoginActivity.class.getSimpleName();
 //   private static final String PATH_TOS = "";
    String memberEmail,myDeviceId,token;//deviceId=topicId=ShopId
    public static final String devicePrefs = "devicePrefs";
    DatabaseReference mUserFile;

    Button loginButton;
    private FirebaseAuth auth;
    private static final int RC_SIGN_IN = 200;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        if(isUserLogin()){
            loginUser();
        }
        setContentView(tw.imonkey.e2gogo.R.layout.activity_login);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        loginButton = (Button)findViewById(tw.imonkey.e2gogo.R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(AuthUI.getInstance()
                        .createSignInIntentBuilder()
                     //   .setLogo(R.drawable.logo_googleg_color_144dp)
                        .setProviders(Arrays.asList(
                                new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()
                        ))
                        .build(), RC_SIGN_IN);
            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN){
            if(resultCode == RESULT_OK){
                loginUser();
            }
            if(resultCode == RESULT_CANCELED){
                displayMessage(getString(tw.imonkey.e2gogo.R.string.signin_failed));
            }
            return;
        }else {
            displayMessage(getString(tw.imonkey.e2gogo.R.string.unknown_response));
        }
    }
    private boolean isUserLogin(){
        return auth.getCurrentUser() != null;
    }
    private void loginUser(){
        if (myDeviceId==null && auth.getCurrentUser()!=null){
            token = FirebaseInstanceId.getInstance().getToken();
            memberEmail=auth.getCurrentUser().getEmail();
            mUserFile= FirebaseDatabase.getInstance().getReference("/USER/" +memberEmail.replace(".", "_"));
            myDeviceId =mUserFile.push().getKey();
            Map<String, Object> addUser = new HashMap<>();
            addUser.put("memberEmail",memberEmail);
            addUser.put("deviceId",myDeviceId);
            addUser.put("token",token);
            addUser.put("timeStamp", ServerValue.TIMESTAMP);
            mUserFile.setValue(addUser);
            SharedPreferences.Editor editor = getSharedPreferences(devicePrefs, Context.MODE_PRIVATE).edit();
            editor.putString("deviceId",myDeviceId);
            editor.putString("memberEmail",memberEmail);
            editor.apply();
            FirebaseMessaging.getInstance().subscribeToTopic(myDeviceId);
        }

        Intent loginIntent = new Intent(LoginActivity.this, QRActivity.class);
        startActivity(loginIntent);
        finish();
    }
    private void displayMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}