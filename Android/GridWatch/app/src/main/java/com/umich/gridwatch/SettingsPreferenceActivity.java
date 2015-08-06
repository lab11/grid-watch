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

import com.umich.gridwatch.Utils.IntentConfig;

import java.util.List;
import java.util.UUID;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsPreferenceActivity extends PreferenceActivity {

    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;
    private boolean first_delete_result = false;
    private boolean second_delete_result = false;
    private boolean delete_result = false;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupSimplePreferencesScreen();
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        Log.d("SettingsPReferenceActivity:isValidFragment", fragmentName);
        return true;
        //return SettingsPreferenceActivity.class.getName().equals(fragmentName);
    }


    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.
        /*
        PreferenceCategory header1 = new PreferenceCategory(this);
        header1 = new PreferenceCategory(this);
        header1.setTitle(R.string.pref_header_user);
        getPreferenceScreen().addPreference(header1);
        */
        addPreferencesFromResource(R.xml.pref_user);


        // Add 'general' preferences.

        // Add 'notifications' preferences, and a corresponding header.
        PreferenceCategory fakeHeader = new PreferenceCategory(this);
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_power_company);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_power_company);
        addPreferencesFromResource(R.xml.pref_btns);

        try {
            bindPreferenceSummaryToValue(findPreference("power_company_name"));
            bindPreferenceSummaryToValue(findPreference("power_company_phone"));
            bindPreferenceSummaryToValue(findPreference("home_address_text"));
            bindPreferenceSummaryToValue(findPreference("id_text"));
            bindPreferenceSummaryToValue(findPreference("work_address_text"));
            bindPreferenceSummaryToValue(findPreference("gps_list"));
            bindPreferenceSummaryToValue(findPreference("gps_list_automatic"));
            bindPreferenceSummaryToValue(findPreference("map_update_pref"));
//            bindPreferenceSummaryToValue(findPreference("wifiOrNetwork"));
        }
        catch (java.lang.NullPointerException e) {
            Log.w("settingsPreference:setupSimplePreference", e.toString());
        }


        Preference button = (Preference)findPreference(getString(R.string.delete_data_btn));
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                //RRRRRRR hack to get around some random UI exception.. these should be spawned from the home activity... doesn't seem to work
                showFirstDialog();
                if (delete_result) {
                    Log.d("settingsPreferenceDialog:onPreferenceClick", "deleting data");
                }


                /*
                //code for what you want it to do
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

    private void showFirstDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsPreferenceActivity.this);
        final String tag = "all:ConfirmDeleteDialog:showFirstDialog";
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
                        first_delete_result = false;
                    }
                });
        builder.show();
    }

    private void showSecondDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsPreferenceActivity.this);
        final String tag = "all:ConfirmDeleteDialog:onCreateDialog";
        final String random = UUID.randomUUID().toString().substring(0, 4);
        final EditText edittext = new EditText(SettingsPreferenceActivity.this);
        edittext.setTextColor(Color.RED);
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
                Log.w(tag, "HIT");
                Log.w(tag, edittext.getText().toString());
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

    public void showThirdDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsPreferenceActivity.this);
        boolean result = false;
        final String tag = "all:DeleteBuilderDialog:onCreateDialog";
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
                            Log.w("tag", msg);
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
           showNoConnectionDialog();
        }
    }

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
        //return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return false; //HACK
        /*
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
                */
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
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

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            Log.w("SettingsPreferenceActivity:onPreferenceChanged", preference.toString());
            Log.w("SettingsPreferenceActivity:onPreferenceChanged", value.toString());


            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
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



            /*
            addPreferencesFromResource(R.xml.pref_general);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("example_text"));
            bindPreferenceSummaryToValue(findPreference("example_list"));
            */
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
            /*
            addPreferencesFromResource(R.xml.pref_notification);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
            */
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
            /*
            addPreferencesFromResource(R.xml.pref_data_sync);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
            */
        }
    }
}
