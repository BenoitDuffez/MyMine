SplitActivity
=============

Activity master+content pattern library.


Behavior
--------

* On phones: only one pane (either master or content)
* On 7", portrait: same as phones
* On 7", landscape: same as tablets
* On tablets: two panes (master on the left, content on the right)

Java code
---------

* make your activity extend `SplitActivity`.
* implement `createMainFragment`: this method is called when the main fragment has to be created
* implement `createContentFragment`: this method is called when the content fragment has to be created
* you can override `createEmptyFragment` if you want to use a custom fragment when there is no content selected, but the UI has two panes.


Here's an example of a minimal `Activity`:


	public class MainActivity extends SplitActivity<MainListFragment, ContentFragment> {
		@Override
		protected MainListFragment createMainFragment(Bundle args) {
			return MainListFragment.newInstance(args);
		}
	
		@Override
		protected ContentFragment createContentFragment(final Bundle args) {
			return ContentFragment.newInstance(args);
		}
	
		@Override
		protected Fragment createEmptyFragment(Bundle args) {
			return new EmptyFragment();
		}
	}


Here's what your main fragment should do to load something (here we have a `ListView`, but it could be anything):


	public class ContentFragment extends SherlockFragment {
		// ...

		@Override
		public void onListItemClick(final ListView l, final View v, final int position, final long id) {
			final Bundle args = new Bundle();
			// ...
			// build your Bundle with args for your contents fragment
			((SplitActivity) getActivity()).selectContent(args);
		}
	}



XML code
--------

Just make your theme point to `Theme.SplitActivity` (or use this one as the parent of your theme). There is also `Theme.SplitActivity.Light` and all the family!

All the themes have `Theme.Sherlock.*` as their parent, so no worries about your ActionBarSherlock setup.

Themes:
Your main theme can implement these:

    <resources>
        <style name="Example" parent="Theme.SplitActivity.Light">
            <item name="mainPaneStyle">@style/MainPaneStyle</item>
            <item name="contentPaneStyle">@style/ContentPaneStyle</item>
            <item name="containerStyle">@style/ContainerStyle</item>
            <item name="separatorStyle">@style/SeparatorStyle</item>
        </style>
    </resources>

Here's an example of customization:

    <!-- The container exists only when there are two panes. It's the big container. -->
    <style name="ContainerStyle" parent="Widget.SplitActivity.Container">
        <item name="android:background">#00FF00</item>
    </style>

    <!-- This is the main pane style. -->
    <style name="MainPaneStyle" parent="Widget.SplitActivity.MainPane">
        <item name="android:layout_margin">12dp</item>
        <item name="android:layout_weight">2</item>
        <item name="android:background">#FFF</item>
    </style>

    <!-- This is the content pane style. It is used both when in single pane (phone) or dual pane (tablet) presentation. -->
    <style name="ContentPaneStyle" parent="Widget.SplitActivity.ContentPane">
        <item name="android:layout_margin">24dp</item>
        <item name="android:layout_weight">3</item>
        <item name="android:background">#FFF</item>
    </style>

    <!-- This is the separator view between the master and the content. Of course, only used in tablet presentation! -->
    <style name="SeparatorStyle" parent="Widget.SplitActivity.Separator">
        <item name="android:background">#F00</item>
        <item name="android:layout_width">2dp</item>
    </style>
