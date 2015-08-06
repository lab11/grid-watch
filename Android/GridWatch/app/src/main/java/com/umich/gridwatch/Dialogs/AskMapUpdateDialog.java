package com.umich.gridwatch.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;

import com.umich.gridwatch.R;
import com.umich.gridwatch.Utils.SensorConfig;

/**
 * Created by nklugman on 7/19/15.
 */
public class AskMapUpdateDialog extends DialogFragment {

    SharedPreferences sharedpreferences;



    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final SharedPreferences.Editor editor = sharedpreferences.edit();


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String tag = "all:AskMapUpdateDialog:onCreateDialog";

        final AskMapUpdateDialogListener activity = (AskMapUpdateDialogListener) getActivity();
        String message = getText(R.string.ask_map_update_dialog_title).toString();
        builder.setTitle(R.string.ask_map_update_dialog);
        builder.setMessage(message)
                .setPositiveButton(R.string.ask_map_update_dialog_affirmative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        activity.onAskMapUpdateReturnValue(SensorConfig.yes);
                        editor.putString("map_update_pref", SensorConfig.yes);
                    }
                })
                .setNegativeButton(R.string.ask_map_update_dialog_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        activity.onAskMapUpdateReturnValue(SensorConfig.no);
                        editor.putString("map_update_pref", SensorConfig.no);

                    }
                })
                .setNeutralButton(R.string.ask_map_update_dialog_remember_affirmative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        activity.onAskMapUpdateReturnValue(SensorConfig.always);
                        editor.putString("map_update_pref", SensorConfig.always);
                    }
                });
        editor.apply();
        return builder.create();
    }

    public interface AskMapUpdateDialogListener {
        public void onAskMapUpdateReturnValue(String result);
    }
}