package com.umich.gridwatch.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.umich.gridwatch.R;

/**
 * Created by nklugman on 7/27/15.
 */
public class ConsentDialog extends DialogFragment {
    boolean result = false;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String tag = "all:ConsentDialog:onCreateDialog";

        final ConsentDialogListener activity = (ConsentDialogListener) getActivity();
        String message = getText(R.string.consent_dialog).toString();
        builder.setTitle("Terms of Service");
        builder.setMessage(message)
                .setPositiveButton(R.string.consent_dialog_affirmative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        result = true;
                        activity.onConsentReturnValue(result);
                    }
                })
                .setNegativeButton(R.string.consent_dialog_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        activity.onConsentReturnValue(result);
                    }
                });
        return builder.create();
    }

    public interface ConsentDialogListener {
        public void onConsentReturnValue(Boolean result);
    }
}