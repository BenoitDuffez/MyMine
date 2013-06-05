package net.bicou.redmine.app.wiki;

import net.bicou.redmine.R;
import net.bicou.redmine.app.AbsMyMineActivity;

import android.os.Bundle;

public class WikiActivity extends AbsMyMineActivity {
	private static final String WIKI_CONTENTS_TAG = "wiki";

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wiki);

		// Setup fragments
		if (savedInstanceState == null) {
			final Bundle args = new Bundle(getIntent().getExtras());
			if (findViewById(R.id.wiki_contents) != null) {
				getSupportFragmentManager().beginTransaction().replace(R.id.wiki_contents, WikiPageFragment.newInstance(args), WIKI_CONTENTS_TAG).commit();
			}
		}
	}

	@Override
	protected boolean shouldDisplayProjectsSpinner() {
		return true;
	}

	@Override
	public void onPreCreate() {
		prepareIndeterminateProgressActionBar();
	}

	@Override
	protected void onCurrentProjectChanged() {
		final WikiPageFragment f = (WikiPageFragment) getSupportFragmentManager().findFragmentById(R.id.wiki_contents);
		f.updateCurrentProject(getCurrentProject());
	}
}
