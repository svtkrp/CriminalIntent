package com.book.criminalintent;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import java.io.File;

public class PhotoFragment extends DialogFragment {

    private static final String ARG_FILENAME = "filename";

    private ImageView mPhotoView;

    public static PhotoFragment newInstance(String filename) {
        Bundle args = new Bundle();
        args.putString(ARG_FILENAME, filename);

        PhotoFragment fragment = new PhotoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String filename = getArguments().getString(ARG_FILENAME);
        final File file = CrimeLab.get(getActivity()).getPhotoFile(filename);
        final Bitmap bitmap = PictureUtils.getScaledBitmapIfFileExists(file, getActivity());

        View view = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_crime_photo, null);

        mPhotoView = view.findViewById(R.id.crime_photo);
        mPhotoView.setImageBitmap(bitmap);

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(R.string.photo_dialog_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                })
                .setNeutralButton(R.string.delete_photo_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = FileProvider.getUriForFile(getActivity(),
                                "com.book.criminalintent.fileprovider", file);
                        getActivity().getContentResolver().delete(uri, null, null);
                        sendResult(Activity.RESULT_OK);
                    }
                })
                .create();
    }

    private void sendResult(int resultCode) {
        if (getTargetFragment() == null) {
            return;
        }
        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, new Intent());
    }
}
