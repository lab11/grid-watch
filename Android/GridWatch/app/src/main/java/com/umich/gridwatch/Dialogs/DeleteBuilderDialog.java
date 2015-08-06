package com.umich.gridwatch.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;

import com.umich.gridwatch.R;

/**
 * Created by nklugman on 7/19/15.
 */
public class DeleteBuilderDialog extends DialogFragment {
    boolean result = false;
    String msg = "";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String tag = "all:DeleteBuilderDialog:onCreateDialog";

        final EditText edittext = new EditText(this.getActivity().getApplicationContext());
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
                msg = edittext.getText().toString();
            }
        });

        final DeleteBuilderDialogListener activity = (DeleteBuilderDialogListener) getActivity();
        builder.setTitle("Sorry to see you go!");
        builder.setMessage(R.string.confirm_delete_dialog_three)
                .setNeutralButton(R.string.confirm_delete_dialog_affirmative_three, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        activity.onDeleteBuilderReturnValue(msg);
                    }
                });
        return builder.create();
    }

    public interface DeleteBuilderDialogListener {
        public void onDeleteBuilderReturnValue(String msg);
    }
}
