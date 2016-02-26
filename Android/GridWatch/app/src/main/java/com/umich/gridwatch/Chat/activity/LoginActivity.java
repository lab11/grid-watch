package com.umich.gridwatch.Chat.activity;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.umich.gridwatch.Chat.model.User;
import com.umich.gridwatch.GCM.GCMConfig;
import com.umich.gridwatch.Main;
import com.umich.gridwatch.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class LoginActivity extends AppCompatActivity {

    private String TAG = LoginActivity.class.getSimpleName();
    private EditText inputName, inputEmail;
    private TextInputLayout inputLayoutName, inputLayoutEmail;

    private EditText inputCountry;
    private EditText inputCity;
    private EditText inputState;
    private CheckBox inputLoc;

    private Button btnEnter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Check for login session. It user is already logged in
         * redirect him to main activity
         * */
        if (Main.getInstance().getPrefManager().getUser() != null) {
            startActivity(new Intent(this, MainChatActivity.class));
            finish();
        }

        setContentView(R.layout.activity_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_chat_toolbar);
        setSupportActionBar(toolbar);

        inputLayoutName = (TextInputLayout) findViewById(R.id.input_layout_name);
        inputLayoutEmail = (TextInputLayout) findViewById(R.id.input_layout_email);
        inputName = (EditText) findViewById(R.id.input_name);
        inputEmail = (EditText) findViewById(R.id.input_email);

        inputCountry = (EditText) findViewById(R.id.input_user_country);
        inputCountry.addTextChangedListener(new MyTextWatcher(inputCountry));
        inputCity = (EditText) findViewById(R.id.input_user_city);
        inputCity.addTextChangedListener(new MyTextWatcher(inputCity));
        inputState = (EditText) findViewById(R.id.input_user_state);
        inputState.addTextChangedListener(new MyTextWatcher(inputState));

        inputLoc = (CheckBox) findViewById(R.id.location_user_checkbox);

        btnEnter = (Button) findViewById(R.id.btn_enter);

        inputName.addTextChangedListener(new MyTextWatcher(inputName));
        inputEmail.addTextChangedListener(new MyTextWatcher(inputEmail));

        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });
    }

    /**
     * logging in user. Will make http post request with name, email
     * as parameters
     */
    private void login() {
        if (!validateName()) {
            return;
        }

        if (!validateEmail()) {
            return;
        }

        final String lat;
        String lat1 = "";
        final String lng;
        String lng1 = "";
        if (inputLoc.isChecked()) {
            Location last = getLastKnownLoaction(true, this);
            try {
                lat1 = String.valueOf(last.getLatitude());
                lng1 = String.valueOf(last.getLongitude());
            } catch (NullPointerException e) {
                lat1 = "";
                lng1 = "";
            }
            Log.d("NOAH", "getting loc");
        }
        lng = lng1;
        lat = lat1;

        final String name = inputName.getText().toString();
        final String email = inputEmail.getText().toString();
        final String country_name = inputCountry.getText().toString();
        final String city_name = inputCity.getText().toString();
        final String state_name = inputState.getText().toString();


        StringRequest strReq = new StringRequest(Request.Method.POST,
                GCMConfig.LOGIN, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.e(TAG, "response: " + response);

                try {
                    JSONObject obj = new JSONObject(response);

                    // check for error flag
                    if (obj.getBoolean("error") == false) {
                        // user successfully logged in

                        JSONObject userObj = obj.getJSONObject("user");
                        User user = new User(userObj.getString("user_id"),
                                userObj.getString("name"),
                                userObj.getString("email"),
                                userObj.getString("country"),
                                userObj.getString("state"),
                                userObj.getString("city")

                        );

                        // storing user in shared preferences
                        Main.getInstance().getPrefManager().storeUser(user);

                        // start main activity
                        startActivity(new Intent(getApplicationContext(), MainChatActivity.class));
                        finish();

                    } else {
                        // login error - simply toast the message
                        Toast.makeText(getApplicationContext(), "" + obj.getJSONObject("error").getString("message"), Toast.LENGTH_LONG).show();
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "json parsing error: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "Json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("name", name);
                params.put("email", email);
                params.put("country", country_name);
                params.put("city", city_name);
                params.put("state", state_name);
                params.put("cell_lat", lat);
                params.put("cell_lng", lng);
                Log.e(TAG, "params: " + params.toString());
                return params;
            }
        };

        //Adding request to request queue
        Main.getInstance().addToRequestQueue(strReq);
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    // Validating name
    private boolean validateName() {
        if (inputName.getText().toString().trim().isEmpty()) {
            inputLayoutName.setError(getString(R.string.err_msg_name));
            requestFocus(inputName);
            return false;
        } else {
            inputLayoutName.setErrorEnabled(false);
        }

        return true;
    }

    // Validating email
    private boolean validateEmail() {
        String email = inputEmail.getText().toString().trim();

        if (email.isEmpty() || !isValidEmail(email)) {
            inputLayoutEmail.setError(getString(R.string.err_msg_email));
            requestFocus(inputEmail);
            return false;
        } else {
            inputLayoutEmail.setErrorEnabled(false);
        }

        return true;
    }

    private static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private class MyTextWatcher implements TextWatcher {

        private View view;
        private MyTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.input_name:
                    validateName();
                    break;
                case R.id.input_email:
                    validateEmail();
                    break;
            }
        }



    }

    public static Location getLastKnownLoaction(boolean enabledProvidersOnly, Context context){
            LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location utilLocation = null;
            List<String> providers = manager.getProviders(enabledProvidersOnly);
            for(String provider : providers){
                try {
                    utilLocation = manager.getLastKnownLocation(provider); //TODO check time
                    if (utilLocation != null) return utilLocation;
                } catch (SecurityException e) {
                    Toast.makeText(context, "Permission error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            return null;
    }

    public void hideSoftKeyboard() {
        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

}
