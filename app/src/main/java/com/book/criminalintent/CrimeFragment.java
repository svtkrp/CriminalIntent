package com.book.criminalintent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static android.widget.CompoundButton.*;

public class CrimeFragment extends Fragment {

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";
    private static final String DIALOG_TIME = "DialogTime";
    private static final String DIALOG_PHOTO = "DialogPhoto";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_TIME = 1;
    private static final int REQUEST_CONTACT = 2;
    private static final int REQUEST_PHOTO = 3;
    private static final int REQUEST_PHOTO_WAS_DELETED = 4;

    private static final int[] HOURS = {8, 12, 15, 17};

    private Crime mCrime;
    private File mPhotoFile;
    private ImageView mPhotoView;
    private ImageButton mPhotoButton;
    private EditText mTitleField;
    private Button mDateButton;

    private Spinner mTimeSpinner;
    private AdapterWithCustomItem mTimeAdapter;
    private String[] mTimes;
    private boolean mTimeItemWasClicked = false;

    private CheckBox mSolvedCheckBox;

    private Button mSuspectButton;
    private ImageButton mDeleteSuspectButton;
    private Button mReportButton;

    private Callbacks mCallbacks;

    public interface Callbacks {
        void onCrimeUpdated(Crime crime);
        void onCrimeDeleted(Crime crime);
    }

    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onPause() {
        super.onPause();

        CrimeLab.get(getActivity()).updateCrime(mCrime);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crime, container, false);

        mTitleField = view.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
                updateCrime();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mDateButton = view.findViewById(R.id.crime_date_date);

        mTimes = getResources().getStringArray(R.array.times);
        mTimeAdapter = new AdapterWithCustomItem(getActivity(), mTimes);
        mTimeSpinner = view.findViewById(R.id.crime_date_time);
        mTimeSpinner.setAdapter(mTimeAdapter);

        updateDate();

        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                DatePickerFragment dialog = DatePickerFragment.newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                dialog.show(fragmentManager, DIALOG_DATE);
            }
        });

        mTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mTimeItemWasClicked) {
                    if (position == mTimes.length-1) {
                        FragmentManager fragmentManager = getFragmentManager();
                        TimePickerFragment dialog = TimePickerFragment.newInstance(mCrime.getDate());
                        dialog.setTargetFragment(CrimeFragment.this, REQUEST_TIME);
                        dialog.show(fragmentManager, DIALOG_TIME);
                    } else {
                        mCrime.setDate(changeTimeNotDate(mCrime.getDate(), HOURS[position], 0));
                        updateDate();
                        updateCrime();
                    }
                } else {
                    mTimeItemWasClicked = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mSolvedCheckBox = view.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
                updateCrime();
            }
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = view.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        mDeleteSuspectButton = view.findViewById(R.id.crime_suspect_delete);
        mDeleteSuspectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCrime.setSuspect(null);
                mSuspectButton.setText(R.string.crime_suspect_button);
                mDeleteSuspectButton.setVisibility(GONE);
                updateCrime();
            }
        });

        if (mCrime.getSuspect() != null) {
            mSuspectButton.setText(getString(R.string.crime_suspect_chosen_button,
                    mCrime.getSuspect()));
        } else {
            mDeleteSuspectButton.setVisibility(GONE);
        }

        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(pickContact,
                PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }

        mReportButton = view.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(getActivity());
                Intent intent = builder.setType("text/plain")
                        .setText(getCrimeReport())
                        .setSubject(getString(R.string.crime_report_subject))
                        .setChooserTitle(R.string.send_report_title)
                        .createChooserIntent();
                startActivity(intent);
            }
        });

        mPhotoView = view.findViewById(R.id.crime_photo);
        updatePhotoView();
        mPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                PhotoFragment dialog = PhotoFragment.newInstance(mCrime.getPhotoFilename());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_PHOTO_WAS_DELETED);
                dialog.show(fragmentManager, DIALOG_PHOTO);
            }
        });

        mPhotoButton = view.findViewById(R.id.crime_camera);
        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        boolean canTakePhoto = mPhotoFile != null &&
                captureImage.resolveActivity(packageManager) != null;
        mPhotoButton.setEnabled(canTakePhoto);
        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = FileProvider.getUriForFile(getActivity(),
                        "com.book.criminalintent.fileprovider", mPhotoFile);
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                List<ResolveInfo> cameraActivities = getActivity().getPackageManager()
                        .queryIntentActivities(captureImage, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo activity : cameraActivities) {
                    getActivity().grantUriPermission(activity.activityInfo.packageName, uri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }
                startActivityForResult(captureImage, REQUEST_PHOTO);
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_DATE) {
            Date date = (Date) data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            updateDate();

        } else if (requestCode == REQUEST_TIME) {
            Date date = (Date) data.getSerializableExtra(TimePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            updateDate();

        } else if (requestCode == REQUEST_CONTACT && data != null) {
            Uri contactUri = data.getData();

            String[] queryFields = new String[] { ContactsContract.Contacts.DISPLAY_NAME };
            Cursor cursor = getActivity().getContentResolver().query(contactUri, queryFields,
                    null, null, null);

            try {
                if (cursor.getCount() == 0) {
                    return;
                }
                cursor.moveToFirst();
                String suspect = cursor.getString(0);
                mCrime.setSuspect(suspect);
                mSuspectButton.setText(getString(R.string.crime_suspect_chosen_button, suspect));
                mDeleteSuspectButton.setVisibility(VISIBLE);
            } finally {
                cursor.close();
            }
        } else if (requestCode == REQUEST_PHOTO) {
            Uri uri = FileProvider.getUriForFile(getActivity(),
                    "com.book.criminalintent.fileprovider", mPhotoFile);
            getActivity().revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            updatePhotoView();
        } else if (requestCode == REQUEST_PHOTO_WAS_DELETED) {
            updatePhotoView();
            Toast.makeText(getActivity(),
                    R.string.photo_was_deleted_text, Toast.LENGTH_LONG).show();
        } else return;

        updateCrime();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_crime:
                CrimeLab.get(getActivity()).deleteCrime(mCrime);
                mCallbacks.onCrimeDeleted(mCrime);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    private void updateCrime() {
        CrimeLab.get(getActivity()).updateCrime(mCrime);
        mCallbacks.onCrimeUpdated(mCrime);
    }

    private void updateDate() {
        mDateButton.setText(DateFormat.format
                (getString(R.string.date_date_format_detail_crime), mCrime.getDate()));
        mTimeAdapter.setCustomText((String)DateFormat.format
                (getString(R.string.date_time_format_detail_crime), mCrime.getDate()));
    }

    private Date changeTimeNotDate(Date date, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        return calendar.getTime();
    }

    private String getCrimeReport() {
        String title = mCrime.getTitle();
        if (title == null) title = getString(R.string.title_null_report);

        String solvedString;
        if (mCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_solved);
        } else {
            solvedString = getString(R.string.crime_report_unsolved);
        }

        String dateString = DateFormat.format
                (getString(R.string.date_format_report), mCrime.getDate()).toString();

        String suspect = mCrime.getSuspect();
        if (suspect == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }

        String report = getString(R.string.crime_report,
                title, dateString, solvedString, suspect);
        return report;
    }

    private void updatePhotoView() {
        Bitmap bitmap = PictureUtils.getScaledBitmapIfFileExists(mPhotoFile, getActivity());
        mPhotoView.setEnabled(bitmap != null);
        mPhotoView.setImageBitmap(bitmap);
    }

    private class AdapterWithCustomItem extends ArrayAdapter<String>
    {
        private String[] mOptions;

        private String mCustomText = "";

        public AdapterWithCustomItem(Context context, String[] options){
            super(context, android.R.layout.simple_spinner_dropdown_item, options);
            mOptions = options;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView textView = view.findViewById(android.R.id.text1);
            textView.setText(mCustomText);

            return view;
        }

        public void setCustomText(String customText) {
            mCustomText = customText;
            notifyDataSetChanged();
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return super.getDropDownView(position, convertView, parent);
        }
    }
}