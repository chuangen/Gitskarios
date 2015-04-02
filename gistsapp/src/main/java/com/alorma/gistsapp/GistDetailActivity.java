package com.alorma.gistsapp;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import com.alorma.gistsapp.ui.fragment.GistDetailFragment;

/**
 * Created by Bernat on 02/04/2015.
 */
public class GistDetailActivity extends ActionBarActivity {

    public static Intent createLauncherIntent(Context context, String id) {
        Intent intent = new Intent(context, GistDetailActivity.class);

        intent.putExtra(GistDetailFragment.GIST_ID, id);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.generic_toolbar);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(android.R.id.content, GistDetailFragment.newInstance(getIntent().getStringExtra(GistDetailFragment.GIST_ID)));
        ft.commit();
    }
}
