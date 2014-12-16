package com.alorma.github.ui.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.alorma.github.R;
import com.alorma.github.sdk.bean.dto.response.Branch;
import com.alorma.github.sdk.bean.dto.response.Repo;
import com.alorma.github.sdk.bean.info.RepoInfo;
import com.alorma.github.sdk.security.ApiConstants;
import com.alorma.github.sdk.services.client.BaseClient;
import com.alorma.github.sdk.services.repo.GetRepoClient;
import com.alorma.github.sdk.services.repo.actions.CheckRepoStarredClient;
import com.alorma.github.sdk.services.repo.actions.CheckRepoWatchedClient;
import com.alorma.github.sdk.services.repo.actions.StarRepoClient;
import com.alorma.github.sdk.services.repo.actions.UnstarRepoClient;
import com.alorma.github.sdk.services.repo.actions.UnwatchRepoClient;
import com.alorma.github.sdk.services.repo.actions.WatchRepoClient;
import com.alorma.github.ui.ErrorHandler;
import com.alorma.github.ui.activity.base.BackActivity;
import com.alorma.github.ui.fragment.detail.repo.SourceListFragment;
import com.alorma.github.ui.fragment.detail.repo.MarkdownFragment;
import com.alorma.github.ui.fragment.detail.repo.RepoSettingsFragment;
import com.alorma.github.ui.fragment.issues.IssuesFragment;
import com.alorma.github.ui.listeners.RefreshListener;
import com.alorma.github.ui.view.SlidingTabLayout;
import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;

import java.util.ArrayList;
import java.util.List;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Bernat on 17/07/2014.
 */
public class RepoDetailActivity extends BackActivity implements RefreshListener, BaseClient.OnResultCallback<Repo>, AdapterView.OnItemSelectedListener {

	public static final String OWNER = "OWNER";
	public static final String REPO = "REPO";
	public static final String FROM_INTENT_FILTER = "FROM_INTENT_FILTER";
	public static final String DESCRIPTION = "DESCRIPTION";

	private Uri shareUri;
	private String description;
	private RepoInfo repoInfo;
	private boolean fromIntentFilter;
	private boolean repoStarred;
	private boolean repoWatched;
	private Repo currentRepo;
	private MarkdownFragment markdownFragment;
	private SourceListFragment sourceListFragment;
	private IssuesFragment issuesFragment;
	private Branch currentBranch;
	private RepoSettingsFragment settingsFragment;
	private ViewPager viewPager;
	private List<Fragment> listFragments;

	public static Intent createLauncherActivity(Context context, String owner, String repo, String description) {
		Bundle bundle = new Bundle();
		bundle.putString(OWNER, owner);
		bundle.putString(REPO, repo);
		bundle.putString(DESCRIPTION, description);
		bundle.putBoolean(FROM_INTENT_FILTER, false);

		Intent intent = new Intent(context, RepoDetailActivity.class);
		intent.putExtras(bundle);
		return intent;
	}

	public static Intent createIntentFilterLauncherActivity(Context context, String owner, String repo, String description) {
		Bundle bundle = new Bundle();
		bundle.putString(OWNER, owner);
		bundle.putString(REPO, repo);
		bundle.putString(DESCRIPTION, description);
		bundle.putBoolean(FROM_INTENT_FILTER, true);

		Intent intent = new Intent(context, RepoDetailActivity.class);
		intent.putExtras(bundle);
		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_repo_detail);

		if (getIntent().getExtras() != null) {
			repoInfo = new RepoInfo();
			repoInfo.owner = getIntent().getExtras().getString(OWNER);
			repoInfo.repo = getIntent().getExtras().getString(REPO);

			setTitle(repoInfo.repo);

			setUpShare(repoInfo);

			description = getIntent().getExtras().getString(DESCRIPTION);
			fromIntentFilter = getIntent().getExtras().getBoolean(FROM_INTENT_FILTER);

			SlidingTabLayout slidingTabLayout = (SlidingTabLayout) findViewById(R.id.tabStrip);

			slidingTabLayout.setSelectedIndicatorColors(Color.WHITE);
			slidingTabLayout.setDividerColors(Color.TRANSPARENT);

			viewPager = (ViewPager) findViewById(R.id.pager);


			markdownFragment = MarkdownFragment.newInstance(repoInfo.owner, repoInfo.repo, null);
			sourceListFragment = SourceListFragment.newInstance(repoInfo.owner, repoInfo.repo, null, this);
			issuesFragment = IssuesFragment.newInstance(repoInfo.owner, repoInfo.repo, null);

			listFragments = new ArrayList<>();
			listFragments.add(markdownFragment);
			listFragments.add(sourceListFragment);
			listFragments.add(issuesFragment);

			viewPager.setAdapter(new NavigationPagerAdapter(getFragmentManager(), listFragments));
			slidingTabLayout.setViewPager(viewPager);
			load();
		} else {
			finish();
		}
	}

	private class NavigationPagerAdapter extends FragmentPagerAdapter {

		private List<Fragment> listFragments;

		public NavigationPagerAdapter(FragmentManager fm, List<Fragment> listFragments) {
			super(fm);
			this.listFragments = listFragments;
		}

		@Override
		public Fragment getItem(int position) {
			return listFragments.get(position);
		}

		@Override
		public int getCount() {
			return listFragments.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case 0:
					return getString(R.string.markdown_fragment_title);
				case 1:
					return getString(R.string.files_fragment_title);
				case 2:
					return getString(R.string.issues_fragment_title);
				case 3:
					return getString(R.string.commits_fragment_title);
			}
			return "";
		}
	}

	private void load() {
		GetRepoClient repoClient = new GetRepoClient(this, repoInfo.owner, repoInfo.repo);
		repoClient.setOnResultCallback(this);
		repoClient.execute();

		CheckRepoStarredClient starredClient = new CheckRepoStarredClient(this, repoInfo.owner, repoInfo.repo);
		starredClient.setOnResultCallback(new StarredResult());
		starredClient.execute();

		CheckRepoWatchedClient watcheClien = new CheckRepoWatchedClient(this, repoInfo.owner, repoInfo.repo);
		watcheClien.setOnResultCallback(new WatchedResult());
		watcheClien.execute();
	}

	private void setUpShare(RepoInfo info) {
		shareUri = Uri.parse(ApiConstants.WEB_URL);
		shareUri = shareUri.buildUpon().appendPath(info.owner).appendPath(info.repo).build();

		invalidateOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.repo_detail_activity, menu);

		if (menu != null) {
			MenuItem item = menu.findItem(R.id.share_repo);
			if (item != null) {
				IconDrawable iconDrawable = new IconDrawable(this, Iconify.IconValue.fa_share_alt);
				iconDrawable.color(Color.WHITE);
				iconDrawable.actionBarSize();
				item.setIcon(iconDrawable);
			}

			if (currentRepo == null || currentRepo.parent == null) {
				MenuItem parentItem = menu.findItem(R.id.action_show_parent);
				if (parentItem != null) {
					menu.removeItem(parentItem.getItemId());
				}
			}
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		if (item.getItemId() == R.id.share_repo) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setData(shareUri);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_SUBJECT, repoInfo.owner + "/" + repoInfo.repo);
			intent.putExtra(Intent.EXTRA_TEXT, shareUri);

			startActivity(Intent.createChooser(intent, "Share repository!"));
		} else if (item.getItemId() == R.id.action_show_parent) {
			if (currentRepo != null && currentRepo.parent != null) {
				String parentFullName = currentRepo.parent.full_name;
				String[] split = parentFullName.split("/");
				String owner = split[0];
				String name = split[1];

				Intent launcherActivity = RepoDetailActivity.createLauncherActivity(this, owner, name, currentRepo.parent.description);
				startActivity(launcherActivity);
			}
		}

		return false;
	}

	private void changeStarStatus() {
		if (repoStarred) {
			UnstarRepoClient unstarRepoClient = new UnstarRepoClient(this, repoInfo.owner, repoInfo.repo);
			unstarRepoClient.setOnResultCallback(new UnstarActionResult());
			unstarRepoClient.execute();
		} else {
			StarRepoClient starRepoClient = new StarRepoClient(this, repoInfo.owner, repoInfo.repo);
			starRepoClient.setOnResultCallback(new StarActionResult());
			starRepoClient.execute();
		}
		showRefresh();
	}

	private void changeWatchedStatus() {
		if (repoWatched) {
			UnwatchRepoClient unwatchRepoClient = new UnwatchRepoClient(this, repoInfo.owner, repoInfo.repo);
			unwatchRepoClient.setOnResultCallback(new UnwatchActionResult());
			unwatchRepoClient.execute();
		} else {
			WatchRepoClient watchRepoClient = new WatchRepoClient(this, repoInfo.owner, repoInfo.repo);
			watchRepoClient.setOnResultCallback(new WatchActionResult());
			watchRepoClient.execute();
		}
		showRefresh();
	}

	@Override
	public void showRefresh() {
		// TODO START LOADING
	}

	@Override
	public void cancelRefresh() {
		// TODO STOP LOADING
	}

	@Override
	public void onResponseOk(Repo repo, Response r) {
		if (repo != null) {
			this.currentRepo = repo;

			setTitle(currentRepo.name);

			this.invalidateOptionsMenu();

			cancelRefresh();

			if (issuesFragment != null) {
				issuesFragment.setPermissions(repo.permissions);
			}
		}


		cancelRefresh();
	}

	@Override
	public void onFail(RetrofitError error) {
		ErrorHandler.onRetrofitError(this, "RepoDetailFragment", error);
		finish();
		cancelRefresh();
	}

	private void setUpFragment(Fragment fragment) {
		if (fragment != null) {
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			if (fragment.isAdded()) {
				ft.remove(fragment);
			}
			ft.replace(R.id.content, fragment);
			ft.commit();
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

	}

	/*
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({NAVIGATION_MARKDOWN, NAVIGATION_SOURCE, NAVIGATION_ISSUES, NAVIGATION_SETTINGS})
	public @interface NavigationItems {
	}
	*/

	@Override
	public void onNothingSelected(AdapterView<?> parent) {

	}

	/**
	 * Results for STAR
	 */
	private class StarredResult implements BaseClient.OnResultCallback<Object> {

		@Override
		public void onResponseOk(Object o, Response r) {
			if (r != null && r.getStatus() == 204) {
				repoStarred = true;
				invalidateOptionsMenu();
			}
			cancelRefresh();
		}

		@Override
		public void onFail(RetrofitError error) {
			if (error != null) {
				if (error.getResponse() != null && error.getResponse().getStatus() == 404) {
					repoStarred = false;
					invalidateOptionsMenu();
				}
			}
			cancelRefresh();
		}
	}

	private class UnstarActionResult implements BaseClient.OnResultCallback<Object> {

		@Override
		public void onResponseOk(Object o, Response r) {
			if (r != null && r.getStatus() == 204) {
				repoStarred = false;
				Toast.makeText(RepoDetailActivity.this, "Repo unstarred", Toast.LENGTH_SHORT).show();
				invalidateOptionsMenu();
			}
			cancelRefresh();
		}

		@Override
		public void onFail(RetrofitError error) {
			cancelRefresh();
		}
	}

	private class StarActionResult implements BaseClient.OnResultCallback<Object> {

		@Override
		public void onResponseOk(Object o, Response r) {
			if (r != null && r.getStatus() == 204) {
				repoStarred = true;
				invalidateOptionsMenu();
			}
			cancelRefresh();
		}

		@Override
		public void onFail(RetrofitError error) {
			if (error.getResponse() != null && error.getResponse().getStatus() == 404) {
			}
			cancelRefresh();
		}
	}

	/**
	 * RESULTS FOR WATCH
	 */

	private class WatchedResult implements BaseClient.OnResultCallback<Object> {

		@Override
		public void onResponseOk(Object o, Response r) {
			if (r != null && r.getStatus() == 204) {
				repoWatched = true;
				invalidateOptionsMenu();
			}
			cancelRefresh();
		}

		@Override
		public void onFail(RetrofitError error) {
			if (error != null) {
				if (error.getResponse() != null && error.getResponse().getStatus() == 404) {
					repoWatched = false;
					invalidateOptionsMenu();
				}
			}
			cancelRefresh();
		}
	}

	private class UnwatchActionResult implements BaseClient.OnResultCallback<Object> {

		@Override
		public void onResponseOk(Object o, Response r) {
			if (r != null && r.getStatus() == 204) {
				repoWatched = false;
				Toast.makeText(RepoDetailActivity.this, "Not watching repo", Toast.LENGTH_SHORT).show();
				invalidateOptionsMenu();
			}
			cancelRefresh();
		}

		@Override
		public void onFail(RetrofitError error) {
			cancelRefresh();
		}
	}

	private class WatchActionResult implements BaseClient.OnResultCallback<Object> {

		@Override
		public void onResponseOk(Object o, Response r) {
			if (r != null && r.getStatus() == 204) {
				repoWatched = true;
				Toast.makeText(RepoDetailActivity.this, "Watching repo", Toast.LENGTH_SHORT).show();
				invalidateOptionsMenu();
			}
			cancelRefresh();
		}

		@Override
		public void onFail(RetrofitError error) {
			cancelRefresh();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}
}
