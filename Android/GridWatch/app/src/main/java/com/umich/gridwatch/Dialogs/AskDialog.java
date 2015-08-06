package com.umich.gridwatch.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.umich.gridwatch.R;

/**
 * Created by nklugman on 7/17/15.
 */
public class AskDialog extends DialogFragment {
    boolean result = false;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String tag = "all:AskDialog:onCreateDialog";

        final AskDialogListener activity = (AskDialogListener) getActivity();
        String message = getText(R.string.ask_dialog).toString();
        builder.setTitle("Possible Power Outage Detected!");
        builder.setMessage(message)
                .setPositiveButton(R.string.ask_dialog_affirmative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        result = true;
                        activity.onAskReturnValue(result);
                    }
                })
                .setNegativeButton(R.string.ask_dialog_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        activity.onAskReturnValue(result);
                    }
                });
        return builder.create();
    }

    public interface AskDialogListener {
        public void onAskReturnValue(Boolean result);
    }
}