package com.umich.gridwatch.Chat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.umich.gridwatch.GCM.GCMIntentService;
import com.umich.gridwatch.Main;
import com.umich.gridwatch.R;
import com.umich.gridwatch.GCM.GCMConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class CreateCategory extends AppCompatActivity {

    private String TAG = LoginActivity.class.getSimpleName();
    private EditText inputName;
    private TextInputLayout inputLayoutName;
    private EditText inputCountry;
    private TextInputLayout inputLayoutCountry;
    private EditText inputCity;
    private TextInputLayout inputLayoutCity;
    private EditText inputState;
    private TextInputLayout inputLayoutState;
    private CheckBox inputLoc;
    private Button btnEnter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_category);
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_chat_toolbar);
        setSupportActionBar(toolbar);
        inputLayoutName = (TextInputLayout) findViewById(R.id.input_layout_category_name);
        inputName = (EditText) findViewById(R.id.input_category_name);


        inputLayoutCountry = (TextInputLayout) findViewById(R.id.input_layout_category_country);
        inputCountry = (EditText) findViewById(R.id.input_category_country);
        inputCountry.addTextChangedListener(new MyTextWatcher(inputCountry));

        inputLayoutCity = (TextInputLayout) findViewById(R.id.input_layout_category_city);
        inputCity = (EditText) findViewById(R.id.input_category_city);
        inputCity.addTextChangedListener(new MyTextWatcher(inputCity));

        inputLayoutState = (TextInputLayout) findViewById(R.id.input_layout_category_state);
        inputState = (EditText) findViewById(R.id.input_category_state);
        inputState.addTextChangedListener(new MyTextWatcher(inputState));

        inputLoc = (CheckBox) findViewById(R.id.location_category_checkbox);

        btnEnter = (Button) findViewById(R.id.btn_send_category);
        inputName.addTextChangedListener(new MyTextWatcher(inputName));
        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createCategory();
            }
        });
    }

    /**
     * logging in user. Will make http post request with name, email
     * as parameters
     */
    private void createCategory() {
        if (!validateName()) {
            return;
        }

        final String name = inputName.getText().toString();
        final String country_name = inputCountry.getText().toString();
        final String city_name = inputCity.getText().toString();
        final String state_name = inputState.getText().toString();
        final boolean loc_ok = inputLoc.isChecked();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                GCMConfig.CREATE_CATEGORY, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.e(TAG, "response: " + response);

                try {
                    JSONObject obj = new JSONObject(response);

                    // check for error flag
                    if (obj.getBoolean("error") == false) {
                        Intent intent = new Intent(getApplicationContext(), GCMIntentService.class);
                        intent.putExtra(GCMConfig.KEY, GCMConfig.SUBSCRIBE);
                        intent.putExtra(GCMConfig.TOPIC, "topic_" + name);
                        startService(intent);

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
                params.put("chat_name", name);
                //params.put("country", country_name);
                //params.put("city", city_name);
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
            }
        }
    }

}
