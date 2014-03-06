package net.bicou.redmine.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.SparseArray;

/**
 * Created by bicou on 04/07/13.
 * <p/>
 * Used to create an AsyncTask that doesn't leak the activity, but yet it provides the callbacks even when the activity is recreated (e.g. screen rotation)
 * <p/>
 * Add the AsyncTaskFragment to your activity:
 * <pre>
 *     class MyAct extends SherlockFragmentActivity implements TaskFragmentCallbacks {
 *         public void onCreate(Bundle savedInstanceState) {
 *             AsyncTaskFragment.attachAsyncTaskFragment(this);
 *         }
 *
 *         @Override
 *         public void onPreExecute(int action, Object parameters) {
 *             switch (action) {
 *                 case ID_TASK:
 *                     // prepare stuff
 *                     break;
 *                 }
 *             }
 *         }
 *
 *         @Override
 *         public void doInBackGround(int action, Object parameters) {
 *             switch (action) {
 *                 case ID_TASK:
 *                     // do the work
 *                     break;
 *                 }
 *             }
 *         }
 *
 *         @Override
 *         public void onPreExecute(int action, Object parameters) {
 *             switch (action) {
 *                 case ID_TASK:
 *                     // update UI
 *                     break;
 *                 }
 *             }
 *         }
 *     }
 * </pre>
 * <p/>
 * Then, somewhere:
 * <pre>
 *     public static final ID_TASK = 0;
 *
 *     // ...
 *
 *     AsyncTaskFragment.runTask(ID_TASK, stuff); // This will call onPreExecute, doInBackGround and onPostExecute from the activity, with stuff as parameters
 * </pre>
 */
public class AsyncTaskFragment extends Fragment {
	public interface TaskFragmentCallbacks {
		/**
		 * Called on the UI thread, before the background task is started.
		 *
		 * @param action     The action ID, used to select what background task has to be done
		 * @param parameters The task parameters
		 */
		public void onPreExecute(int action, Object parameters);

		/**
		 * The background task. Must return the result data.
		 *
		 * @param applicationContext The application Context, that can be used by the background task when a Context is required, and when the Activity may not be
		 *                           available.
		 * @param action             The action ID, used to select what background task has to be done
		 * @param parameters         The task parameters
		 */
		public Object doInBackGround(Context applicationContext, int action, Object parameters);

		/**
		 * Called from the UI thread, when the background task is complete
		 *
		 * @param action The action ID, used to select what background task has to be done
		 * @param result The result data of the background task
		 */
		public void onPostExecute(int action, Object parameters, Object result);
	}

	private static final String TAG = "AsyncTaskFragment";
	private static final String FRAGMENT_TAG = "net.bicou.TaskFragmentTag";
	private TaskFragmentCallbacks mCallbacks;
	protected static SparseArray<Object> mTasks = new SparseArray<Object>();
	private Context mAppContext;

	public static void attachAsyncTaskFragment(ActionBarActivity activity) {
		FragmentManager fm = activity.getSupportFragmentManager();
		AsyncTaskFragment task = new AsyncTaskFragment();
		task.setArguments(new Bundle());
		fm.beginTransaction().add(task, FRAGMENT_TAG).commit();
	}

	/**
	 * Triggers the callbacks in the activity for a given action.<br /> The activity has to implement TaskFragmentCallbacks.
	 */
	public static void runTask(ActionBarActivity activity, int action, Object parameters) {
		FragmentManager fm = activity.getSupportFragmentManager();
		Fragment f = fm.findFragmentByTag(FRAGMENT_TAG);
		if (f != null && f instanceof AsyncTaskFragment) {
			mTasks.put(action, parameters);
			((AsyncTaskFragment) f).run(action, parameters);
		} else {
			throw new IllegalStateException("Your activity must implement TaskFragmentCallbacks and call AsyncTaskFragment.attachAsyncTaskFragment() in its " + "onCreate method.");
		}
	}

	public static boolean isRunning(int action) {
		return mTasks.get(action) != null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof TaskFragmentCallbacks)) {
			throw new IllegalArgumentException("The activity that attaches a AsyncTaskFragment must implement TaskFragmentCallbacks");
		}
		mCallbacks = (TaskFragmentCallbacks) activity;
		int key;
		for (int i = 0; i < mTasks.size(); i++) {
			key = mTasks.keyAt(i);
			mCallbacks.onPreExecute(key, mTasks.get(key));
		}
		mAppContext = activity.getApplicationContext();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = null;
	}

	public void run(final int action, final Object parameters) {
		new AsyncTask<Void, Void, Void>() {
			Object mObject;

			private void waitForCallbacks() {
				int seconds = 0;
				while (mCallbacks == null && seconds < 15) {
					try {
						Thread.sleep(1000);
						seconds++;
					} catch (InterruptedException e) {
						Log.e(TAG, "Couldn't waitForCallbacks 1 second", null);
					}
				}
			}

			@Override
			protected void onPreExecute() {
				waitForCallbacks();
				if (mCallbacks != null) {
					mCallbacks.onPreExecute(action, parameters);
				}
			}

			@Override
			protected Void doInBackground(Void... voids) {
				waitForCallbacks();
				if (mCallbacks != null) {
					mObject = mCallbacks.doInBackGround(mAppContext, action, parameters);
				}
				return null;
			}

			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			@Override
			protected void onCancelled(Void aVoid) {
				super.onCancelled(aVoid);
				mTasks.remove(action);
			}

			@Override
			protected void onCancelled() {
				super.onCancelled();
				mTasks.remove(action);
			}

			@Override
			protected void onPostExecute(Void dummy) {
				mTasks.remove(action);
				waitForCallbacks();
				if (mCallbacks != null) {
					mCallbacks.onPostExecute(action, parameters, mObject);
				}
			}
		}.execute();
	}
}

