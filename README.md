AsyncTaskFragment
=================

A fragment wrapper around an AsyncTask that handles screen rotations

This will add an invisible fragment to your activity. This fragment calls `setRetainInstance(true)` so it will never be deleted. Also, when it is detached from the activity (screen rotation, configuration change, etc.), it will nullify the pointer to the activity, so there is no memory leak. It will automatically call the callback methods when the activity is attached again after recreation.

Usage
-----

1. Make your activity implement `AsyncTaskFragment.TaskFragmentCallbacks`
2. Make your activity call `AsyncTaskFragment.attachAsyncTaskFragment(this)` from the `onCreate` method.
3. Where you need a background task to be run, call `AsyncTaskFragment.runTask()`

Example
-------

	public class ToolsActivity extends SherlockFragmentActivity implements AsyncTaskFragment.TaskFragmentCallbacks {
		public static final int ACTION_HEADERS = 0;
		public static final int ACTION_SOCIAL = 1;
	
		@Override
		public void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
	
			requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	
			setContentView(R.layout.activity_tools);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	
			setSupportProgressBarIndeterminate(true);
			setSupportProgressBarIndeterminateVisibility(false);
	
			AsyncTaskFragment.attachAsyncTaskFragment(this); // Here we prepare the AsyncTaskFragment, but nothing happened yet

			EditText text = (EditText) findViewById(R.id.text);
			findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					// Here we actually trigger the background task
					AsyncTaskFragment.runTask(getActivity(), ACTION_SOCIAL, text.getText());
				}
			});
		}
	
		@Override
		public void onPreExecute(int action, Object parameters) {
			setSupportProgressBarIndeterminateVisibility(true);
			switch (action) {
			case ACTION_HEADERS:
				// prepare some stuff specific to the task 'ACTION_HEADERS'
				break;
	
			case ACTION_SOCIAL:
				// prepare some stuff specific to the task 'ACTION_SOCIAL'
				break;
			}
		}
	
		@Override
		public Object doInBackGround(int action, Object parameters) {
			switch (action) {
			case ACTION_HEADERS:
				// background processing for task ACTION_HEADERS
				break;
	
			case ACTION_SOCIAL:
				// that's an example, try to rotate the screen during the background processing
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				break;
			}
	
			return null;
		}
	
		@Override
		public void onPostExecute(int action, Object parameters, Object result) {
			setSupportProgressBarIndeterminateVisibility(false);
			// here you can update the UI with the results of the background thread work
	
			switch (action) {
			case ACTION_HEADERS:
				break;
	
			case ACTION_SOCIAL:
				break;
			}
		}
	
	}

As you can see, it is possible to launch different tasks which code are in the same activity. The `doInBackGround` method is called off the UI thread so you need to ensure that you don't touch the UI. You can use `onPreExecute` and `onPostExecute` to prepare and update the UI.


LICENSE
=======

	Copyright 2013 Benoit Duffez
	
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
	   http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.



