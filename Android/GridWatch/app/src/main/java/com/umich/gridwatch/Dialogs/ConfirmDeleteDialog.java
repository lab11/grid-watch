package com.umich.gridwatch.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import com.umich.gridwatch.R;

import java.util.UUID;

/**
 * Created by nklugman on 7/19/15.
 */
public class ConfirmDeleteDialog extends DialogFragment {
    boolean result = false;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String tag = "all:ConfirmDeleteDialog:onCreateDialog";
        final String random = UUID.randomUUID().toString().substring(0, 4);

        final EditText edittext = new EditText(this.getActivity().getApplicationContext());
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
                    result = true;
                } else {
                    edittext.setTextColor(Color.RED);
                    result = false;
                }
            }
        });

        final ConfirmDeleteDialogListener activity = (ConfirmDeleteDialogListener) getActivity();
        String message = getText(R.string.confirm_delete_dialog).toString() + "\n\n" + random;
        builder.setTitle("Confirm Report");
        builder.setMessage(message)

                .setPositiveButton(R.string.confirm_delete_dialog_affirmative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                            if (!result || edittext.getCurrentTextColor() == Color.RED) {
                                FailureDialog fd = new FailureDialog();
                                fd.show(getFragmentManager(), "FailureDialog");
                            }
                            activity.onConfirmDeleteReturnValue(result);
                        }
                })
                .setNegativeButton(R.string.confirm_delete_dialog_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        activity.onConfirmDeleteReturnValue(result);
                    }
                });
        return builder.create();
    }

    public interface ConfirmDeleteDialogListener {
        public void onConfirmDeleteReturnValue(Boolean result);
    }
}

/*
        final ReportDialogListener activity = (ReportDialogListener) getActivity();
        String message = getText(R.string.report_dialog).toString() + "\n\n" + random;
        builder.setTitle("Confirm Report");

        builder.setMessage(message)
                .setPositiveButton(R.string.report, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (!result || edittext.getCurrentTextColor() == Color.RED) {
                            FailureDialog fd = new FailureDialog();
                            fd.show(getFragmentManager(), "FailureDialog");
                        }
                        activity.onDialogReturnValue(result);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        activity.onDialogReturnValue(result);
                    }
                });
        return builder.create();
 */