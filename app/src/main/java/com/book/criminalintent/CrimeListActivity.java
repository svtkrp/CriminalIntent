package com.book.criminalintent;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.TextView;

import java.util.UUID;

public class CrimeListActivity extends SingleFragmentActivity
    implements CrimeListFragment.Callbacks, CrimeFragment.Callbacks {

    private static final String SAVED_CRIME_ID = "crime_id";

    private TextView mTextInsteadDetailCrime;
    private UUID mSelectedCrimeId;

    @Override
    protected Fragment createFragment() {
        return new CrimeListFragment();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_masterdetail;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (findViewById(R.id.detail_fragment_container) != null) {
            if (savedInstanceState != null) {
                mSelectedCrimeId = (UUID) savedInstanceState.getSerializable(SAVED_CRIME_ID);
            } else {
                mSelectedCrimeId = null;
            }
            mTextInsteadDetailCrime = findViewById(R.id.text_instead_detail_crime);
            if (mSelectedCrimeId == null) {
                mTextInsteadDetailCrime.setVisibility(View.VISIBLE);
            } else {
                mTextInsteadDetailCrime.setVisibility(View.GONE);
                fillDetailContainer(mSelectedCrimeId);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(SAVED_CRIME_ID, mSelectedCrimeId);
    }

    @Override
    public void onCrimeSelected(Crime crime) {
        if (findViewById(R.id.detail_fragment_container) == null) {
            Intent intent = CrimePagerActivity.newIntent(this, crime.getId());
            startActivity(intent);
        } else {
            mTextInsteadDetailCrime.setVisibility(View.GONE);
            mSelectedCrimeId = crime.getId();
            fillDetailContainer(mSelectedCrimeId);
        }
    }

    private void fillDetailContainer(UUID crimeId) {
        Fragment newDetail = CrimeFragment.newInstance(crimeId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detail_fragment_container, newDetail)
                .commit();
    }

    @Override
    public void onCrimeUpdated(Crime crime) {
        CrimeListFragment listFragment = (CrimeListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        listFragment.updateUI();
    }

    @Override
    public void onCrimeDeleted(Crime crime) {
        onCrimeUpdated(crime);
        onSelectedCrimeDeleted();
    }

    @Override
    public void onCrimeTouchDeleted(Crime crime) {
        if (crime.getId().equals(mSelectedCrimeId)) {
            onSelectedCrimeDeleted();
        } else {
            // do nothing
        }
    }

    private void onSelectedCrimeDeleted() {
        CrimeFragment fragment = (CrimeFragment) getSupportFragmentManager()
                .findFragmentById(R.id.detail_fragment_container);
        if (fragment != null) getSupportFragmentManager().beginTransaction()
                // detach(fragment) works, but app is failed when screen rotation
                .remove(fragment)
                .commit();
        mTextInsteadDetailCrime.setVisibility(View.VISIBLE);
        mSelectedCrimeId = null;
    }
}
