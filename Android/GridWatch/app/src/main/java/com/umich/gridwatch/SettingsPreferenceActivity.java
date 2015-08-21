package com.umich.gridwatch;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.umich.gridwatch.Utils.GridWatchLogger;
import com.umich.gridwatch.Utils.IntentConfig;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * This is the most messed up class right now
 *
 * Issues:
 *    The number of depricated API calls is way too high... All of them need to go eventually
 *
 *    This only shows a simple settings page, which I am fine with for now. Proper design patterns
 *    dictate the implementation of all types of settings pages though.
 *
 *    Need to add logging to each setting changed event... This is a top level TODO
 *
 *    It drives me crazy how dialogs are launched. I played with including them like they are
 *    included in other activities, but kept on getting more and more cryptic UI errors. Ideally,
 *    I think these would be launched by the HomeActivity. Instead, they are hacked and
 *    in some cases reimplemented to launch from this class.
 *
 */
public class SettingsPreferenceActivity extends PreferenceActivity {
    private static final boolean ALWAYS_SIMPLE_PREFS = true;
    private boolean second_delete_result = false; //to pass results between dialogs... hack
    private boolean delete_result = false; //to pass results between dialogs... hack

    private final static String isValidFragmentTag = "SettingsPreferenceActivity:isValidFragment";
    private final static String setupSimplePreferenceScreenTag = "SettingsPreferenceActivity:setupSimplePreferenceScreen";
    private final static String confirmDeleteDialog = "all:ConfirmDeleteDialog:showFirstDialog";
    private final static String setupSimplePreferenceScreenOnPreferenceClickTag = "SettingsPreferenceActivity:setupSimplePreferenceScreen:onPreferenceClick";

    private final static String showSecondDialogTag = "all:ConfirmDeleteDialog:onCreateDialog";
    private final static String showThirdDialogTag = "all:DeleteBuilderDialog:onCreateDialog";
    private final static String onPreferenceChangedTag = "SettingsPreferenceActivity:onPreferenceChanged";

    private static Context mContext;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupSimplePreferencesScreen();
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        Log.d(isValidFragmentTag, fragmentName);
        return true;
    }

    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }


        mContext = this.getApplicationContext();


        addPreferencesFromResource(R.xml.pref_user);

        PreferenceCategory fakeHeader = new PreferenceCategory(this);
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_power_company);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_power_company);
        addPreferencesFromResource(R.xml.pref_btns);

        try {
            //These names should be refactored... they are a bit confusing
            bindPreferenceSummaryToValue(findPreference("power_company_name"));
            bindPreferenceSummaryToValue(findPreference("power_company_phone"));
            bindPreferenceSummaryToValue(findPreference("home_address_text"));
            bindPreferenceSummaryToValue(findPreference("id_text"));
            bindPreferenceSummaryToValue(findPreference("work_address_text"));
            bindPreferenceSummaryToValue(findPreference("gps_list"));
            bindPreferenceSummaryToValue(findPreference("gps_list_automatic"));
            bindPreferenceSummaryToValue(findPreference("map_update_pref"));

            //bindPreferenceSummaryToValue(findPreference("power_company_update"));
            //bindPreferenceSummaryToValue(findPreference("wifi_or_network"));
            //bindPreferenceSummaryToValue(findPreference("make_data_public"));
        }
        catch (java.lang.NullPointerException e) {
            Log.w(setupSimplePreferenceScreenTag, e.toString());
        }

        Preference button = (Preference)findPreference(getString(R.string.delete_data_btn));
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                //RRRRRRR hack to get around various UI exceptions.. dialogs should be spawned
                //from the home activity but that doesn't seem to work
                showFirstDialog();
                if (delete_result) {
                    Log.d(setupSimplePreferenceScreenOnPreferenceClickTag , "deleting data");
                }

                /*
                //template for how this should look
                ConfirmDeleteDialog dialog = new ConfirmDeleteDialog();
                //dialog.show(getContentTransitionManager(), "confirmDeleteDialog");

                Log.d("settingsPreferenceActivity", "hit");
                Intent aIntent = new Intent(IntentConfig.INTENT_NAME);
                aIntent.putExtra(IntentConfig.INTENT_TO_HOME, IntentConfig.INTENT_EXTRA_EVENT_CONFIRM_DELETE);
                aIntent.setPackage("com.umich.gridwatch");
                sendBroadcast(aIntent);
                */
                return true;

            }
        });
    }

    //HACK!
    private void showFirstDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsPreferenceActivity.this);
        String message = getText(R.string.confirm_delete_dialog).toString();
        builder.setTitle("Confirm Delete");
        builder.setMessage(message)
                .setPositiveButton(R.string.confirm_delete_dialog_affirmative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        showSecondDialog();
                    }
                })
                .setNegativeButton(R.string.confirm_delete_dialog_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.show();
    }

    //HACK!
    private void showSecondDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsPreferenceActivity.this);
        final String random = UUID.randomUUID().toString().substring(0, 4);
        final EditText edittext = new EditText(SettingsPreferenceActivity.this);
        edittext.setTextColor(Color.RED);
        System.out.println("Happy Birthday!!");
        builder.setView(edittext);
        edittext.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        edittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(showSecondDialogTag, "HIT");
                Log.d(showSecondDialogTag, edittext.getText().toString());
                if (edittext.getText().toString().equals(random)) {
                    edittext.setTextColor(Color.GREEN);
                    second_delete_result = true;
                } else {
                    edittext.setTextColor(Color.RED);
                    second_delete_result = false;
                }
            }
        });
        String message = getText(R.string.confirm_delete_dialog_two).toString() + "\n\n" + random;
        builder.setTitle("Confirm Report");
        builder.setMessage(message)
                .setPositiveButton(R.string.confirm_delete_dialog_affirmative_two, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (!second_delete_result || edittext.getCurrentTextColor() == Color.RED) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(SettingsPreferenceActivity.this);
                            builder.setMessage(R.string.failure_delete_dialog)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                        }
                                    });
                            builder.show();
                        } else  {
                            showThirdDialog();
                        }
                    }
                })
                .setNegativeButton(R.string.confirm_delete_dialog_negative_two, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.show();
    }

    //HACK!
    public void showThirdDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsPreferenceActivity.this);
        boolean result = false;
        final EditText edittext = new EditText(SettingsPreferenceActivity.this);
        edittext.setSingleLine(false);
        edittext.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        edittext.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        builder.setView(edittext);
        edittext.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });
            builder.setTitle("Sorry to see you go!");
            builder.setMessage(R.string.confirm_delete_dialog_three)
                    .setNeutralButton(R.string.confirm_delete_dialog_affirmative_three, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String msg = edittext.getText().toString();
                            Log.d(showThirdDialogTag, msg);
                            send_delete(msg);
                        }
                    });
            builder.show();
    }


    public boolean isConnected() { //TODO consolodate this with the function in homeactivity
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void send_delete(String msg) {
        if (isConnected()) {
            Intent intent = new Intent(SettingsPreferenceActivity.this, GridWatchDeleteService.class);
            intent.putExtra(IntentConfig.INTENT_DELETE_KEY, IntentConfig.INTENT_DELETE);
            intent.putExtra(IntentConfig.INTENT_DELETE_MSG, msg);
            startService(intent);
        } else {
           showNoConnectionDialog(); //Want to fail fast here... make no promises that data has been
                                     //deleted until we know... Probably want to add ACKs back
        }
    }

    //ABSOLUTE HACK! Reimplementation of Dialogs.NoConnectionDialog. Kills me.
    public void showNoConnectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsPreferenceActivity.this);
        builder.setMessage(R.string.no_connection_dialog_delete)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.show();
    }

    /**
     *
     *
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return false; //HACK
    }


    private static boolean isXLargeTablet(Context context) {
        return false; //HACK
    }

    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }


    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            //Log.d(onPreferenceChangedTag, preference.toString());
            //Log.d(onPreferenceChangedTag, value.toString());

            DateFormat mDateFormat = DateFormat.getDateTimeInstance();
            GridWatchLogger mGWLogger = new GridWatchLogger(mContext);
            mGWLogger.log(mDateFormat.format(new Date()), "preference " + preference.toString() +  " changed to " + value.toString(), null);

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
    }
}
