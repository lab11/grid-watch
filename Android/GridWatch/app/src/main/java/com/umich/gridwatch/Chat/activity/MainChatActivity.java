package com.umich.gridwatch.Chat.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.umich.gridwatch.Chat.adapter.ChatRoomsAdapter;
import com.umich.gridwatch.Chat.helper.NotificationUtils;
import com.umich.gridwatch.Chat.helper.SimpleDividerItemDecoration;
import com.umich.gridwatch.Chat.model.ChatRoom;
import com.umich.gridwatch.Chat.model.Message;
import com.umich.gridwatch.Dialogs.AboutChatDialog;
import com.umich.gridwatch.GCM.GCMConfig;
import com.umich.gridwatch.GCM.GCMIntentService;
import com.umich.gridwatch.Main;
import com.umich.gridwatch.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class MainChatActivity extends AppCompatActivity  {

    private String TAG = MainChatActivity.class.getSimpleName();
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ArrayList<ChatRoom> chatRoomArrayList;
    private ChatRoomsAdapter mAdapter;
    private RecyclerView recyclerView;

    public SearchView search;
    LocationManager locationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Check for login session. If not logged in launch
         * login activity
         * */
        if (Main.getInstance().getPrefManager().getUser() == null) {
            launchPreLoginActivity();
        }

        setContentView(R.layout.activity_chat_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_chat_toolbar);
        setSupportActionBar(toolbar);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        search = (SearchView) findViewById( R.id.chat_search);

        hideKeyboard(this);


        /**
         * Broadcast receiver calls in two scenarios
         * 1. gcm registration is completed
         * 2. when new push notification is received
         * */
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // checking for type intent filter
                if (intent.getAction().equals(GCMConfig.REGISTRATION_COMPLETE)) {
                    // gcm successfully registered
                    // now subscribe to `global` topic to receive app wide notifications
                    subscribeToGlobalTopic();
                    subscribeToMapTopic();

                } else if (intent.getAction().equals(GCMConfig.SENT_TOKEN_TO_SERVER)) {
                    // gcm registration id is stored in our server's MySQL
                    Log.e(TAG, "GCM registration id is sent to our server");

                } else if (intent.getAction().equals(GCMConfig.PUSH_NOTIFICATION)) {
                    // new push notification is received
                    handlePushNotification(intent);
                }
            }
        };

        chatRoomArrayList = new ArrayList<>();
        mAdapter = new ChatRoomsAdapter(this, chatRoomArrayList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(
                getApplicationContext()
        ));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);


        recyclerView.addOnItemTouchListener(new ChatRoomsAdapter.RecyclerTouchListener(getApplicationContext(), recyclerView, new ChatRoomsAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                // when chat is clicked, launch full chat thread activity
                ChatRoom chatRoom = chatRoomArrayList.get(position);
                Intent intent = new Intent(MainChatActivity.this, ChatRoomActivity.class);
                intent.putExtra("chat_room_id", chatRoom.getId());
                intent.putExtra("name", chatRoom.getName());
                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, int position) {
                //Do subscribe/unsubscribe here
                ChatRoom chatRoom = chatRoomArrayList.get(position);
                if (chatRoom.getSubscribed()) {
                    unsubscribeToTopic(chatRoom.getId());
                    chatRoomArrayList.get(position).setSubscribed(false);
                    Main.getInstance().getPrefManager().storeChatRoom(chatRoom);
                    Log.w("HIT", "HIT");
                } else {
                    subscribeToTopic(chatRoom.getId());
                    chatRoomArrayList.get(position).setSubscribed(true);
                    Main.getInstance().getPrefManager().storeChatRoom(chatRoom);
                }
                mAdapter.notifyDataSetChanged();  // data set changed
            }
        }));


        SearchView.OnQueryTextListener listener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String query) {

                query = query.toLowerCase();
                final ArrayList<ChatRoom> filteredList = new ArrayList<>();
                for (int i = 0; i < chatRoomArrayList.size(); i++) {
                    final String text = chatRoomArrayList.get(i).getName().toLowerCase();
                    if (text.contains(query)) {
                        filteredList.add(chatRoomArrayList.get(i));
                    }
                }
                recyclerView.setLayoutManager(new LinearLayoutManager(MainChatActivity.this));
                mAdapter = new ChatRoomsAdapter(MainChatActivity.this,filteredList);
                recyclerView.setAdapter(mAdapter);
                chatRoomArrayList = filteredList;
                mAdapter.notifyDataSetChanged();  // data set changed
                return true;
            }
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
        };
        search.setOnQueryTextListener(listener); // call the QuerytextListner.

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        /**
         * Always check for google play services availability before
         * proceeding further with GCM
         * */
        if (checkPlayServices()) {
            registerGCM();
            fetchChatRooms();
        }
    }

    /**
     * Handles new push notification
     */
    private void handlePushNotification(Intent intent) {
        int type = intent.getIntExtra("type", -1);

        // if the push is of chat room message
        // simply update the UI unread messages count
        if (type == GCMConfig.PUSH_TYPE_CHATROOM) {
            Message message = (Message) intent.getSerializableExtra("message");
            String chatRoomId = intent.getStringExtra("chat_room_id");

            if (message != null && chatRoomId != null) {
                updateRow(chatRoomId, message);
            }
        } else if (type == GCMConfig.PUSH_TYPE_USER) {
            // push belongs to user alone
            // just showing the message in a toast
            Message message = (Message) intent.getSerializableExtra("message");
            Toast.makeText(getApplicationContext(), "New push: " + message.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Updates the chat list unread count and the last message
     */
    private void updateRow(String chatRoomId, Message message) {
        for (ChatRoom cr : chatRoomArrayList) {
            if (cr.getId().equals(chatRoomId)) {
                if (cr.getSubscribed()) {
                    int index = chatRoomArrayList.indexOf(cr);
                    cr.setLastMessage(message.getMessage());
                    cr.setUnreadCount(cr.getUnreadCount() + 1);
                    chatRoomArrayList.remove(index);
                    chatRoomArrayList.add(index, cr);
                    break;
                }
            }
        }
        mAdapter.notifyDataSetChanged();
    }


    /**
     * fetching the chat rooms by making http call
     */
    private void fetchChatRooms() {
        StringRequest strReq = new StringRequest(Request.Method.GET,
                GCMConfig.CHAT_ROOMS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.e(TAG, "response: " + response);

                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.getBoolean("error") == false) {
                        JSONArray chatRoomsArray = obj.getJSONArray("chat_rooms");
                        for (int i = 0; i < chatRoomsArray.length(); i++) {
                            JSONObject chatRoomsObj = (JSONObject) chatRoomsArray.get(i);
                            ChatRoom cr = new ChatRoom();
                            cr.setId(chatRoomsObj.getString("chat_room_id"));
                            cr.setName(chatRoomsObj.getString("name"));
                            cr.setLastMessage("");
                            cr.setUnreadCount(0);
                            cr.setTimestamp(chatRoomsObj.getString("created_at"));



                            boolean found = false;
                            for (int j = 0; j < chatRoomArrayList.size()-1; j++) {
                                if (chatRoomArrayList.get(j).getName().equals(chatRoomsObj.getString("name"))) {
                                    found = true;
                                }
                            }
                            if (!found) {
                                chatRoomArrayList.add(cr);
                            }
                        }
                    } else {
                        // error in fetching chat rooms
                        Toast.makeText(getApplicationContext(), "" + obj.getJSONObject("error").getString("message"), Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "json parsing error: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "Json parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
                mAdapter.notifyDataSetChanged();
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        //Adding request to request queue
        Main.getInstance().addToRequestQueue(strReq);
    }

    // subscribing to global topic
    private void subscribeToGlobalTopic() {

        Intent intent = new Intent(this, GCMIntentService.class);
        intent.putExtra(GCMConfig.KEY, GCMConfig.SUBSCRIBE);
        intent.putExtra(GCMConfig.TOPIC, GCMConfig.TOPIC_GLOBAL);
        startService(intent);

    }

    private void subscribeToMapTopic() {

        Intent intent = new Intent(this, GCMIntentService.class);
        intent.putExtra(GCMConfig.KEY, GCMConfig.SUBSCRIBE);
        intent.putExtra(GCMConfig.TOPIC, GCMConfig.TOPIC_MAPS);
        startService(intent);

    }

    private void unsubscribeToTopic(String topicName) {

        Intent intent = new Intent(this, GCMIntentService.class);
        intent.putExtra(GCMConfig.KEY, GCMConfig.UNSUBSCRIBE);
        intent.putExtra(GCMConfig.TOPIC, "topic_" + topicName);
        startService(intent);

    }

    private void subscribeToTopic(String topicName) {
        Intent intent = new Intent(this, GCMIntentService.class);
        intent.putExtra(GCMConfig.KEY, GCMConfig.SUBSCRIBE);
        intent.putExtra(GCMConfig.TOPIC, "topic_" + topicName);
        startService(intent);
    }

    // Subscribing to all chat room topics
    // each topic name starts with `topic_` followed by the ID of the chat room
    // Ex: topic_1, topic_2
    private void subscribeToAllTopics() {
        for (ChatRoom cr : chatRoomArrayList) {
            Intent intent = new Intent(this, GCMIntentService.class);
            intent.putExtra(GCMConfig.KEY, GCMConfig.SUBSCRIBE);
            intent.putExtra(GCMConfig.TOPIC, "topic_" + cr.getId());
            startService(intent);
        }
    }

    private void launchPreLoginActivity() {
        Intent intent = new Intent(MainChatActivity.this, PreLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void launchCreateCategory() {
        Intent intent = new Intent(MainChatActivity.this, CreateCategory.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register GCM registration complete receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(GCMConfig.REGISTRATION_COMPLETE));

        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(GCMConfig.PUSH_NOTIFICATION));

        // clearing the notification tray
        NotificationUtils.clearNotifications();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // starting the service to register with GCM
    private void registerGCM() {
        Intent intent = new Intent(this, GCMIntentService.class);
        intent.putExtra("key", "register");
        startService(intent);

    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported. Google Play Services not installed!");
                Toast.makeText(getApplicationContext(), "This device is not supported. Google Play Services not installed!", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_logout:
                Main.getInstance().logout();
                break;
            case R.id.action_create_new_category:
                launchCreateCategory();
                break;
            case R.id.action_about_chat:
                DialogFragment dialog = new AboutChatDialog();
                dialog.show(getSupportFragmentManager(), "AboutChatDialogFragment");
                break;
        }

        return super.onOptionsItemSelected(menuItem);
    }

    public Location getLocation() {
        String locationProvider = LocationManager.NETWORK_PROVIDER;
        if ( ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            Toast.makeText(this, getString(R.string.gps_err), Toast.LENGTH_LONG).show();
        }
        return locationManager.getLastKnownLocation(locationProvider);
    }


}
