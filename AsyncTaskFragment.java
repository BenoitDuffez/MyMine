import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import java.util.HashMap;
import java.util.Iterator;

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
		 * @param action The action ID, used to select what background task has to be done
		 * @param parameters The task parameters
		 */
		public void onPreExecute(int action, Object parameters);

		/**
		 * The background task. Must return the result data.
		 * @param action The action ID, used to select what background task has to be done
		 * @param parameters The task parameters
		 */
		public Object doInBackGround(int action, Object parameters);

		/**
		 * Called from the UI thread, when the background task is complete
		 * @param action The action ID, used to select what background task has to be done
		 * @param result The result data of the background task
		 */
		public void onPostExecute(int action, Object parameters, Object result);
	}

	private static final String TAG = "AsyncTaskFragment";
	private static final String FRAGMENT_TAG = "net.bicou.TaskFragmentTag";
	private TaskFragmentCallbacks mCallbacks;
	private HashMap<Integer, Object> mTasks = new HashMap<Integer, Object>();

	public static void attachAsyncTaskFragment(SherlockFragmentActivity activity) {
		FragmentManager fm = activity.getSupportFragmentManager();
		AsyncTaskFragment task = new AsyncTaskFragment();
		task.setArguments(new Bundle());
		fm.beginTransaction().add(task, FRAGMENT_TAG).commit();
	}

	/**
	 * Triggers the callbacks in the activity for a given action.<br />
	 * The activity has to implement TaskFragmentCallbacks.
	 * @param activity
	 * @param action
	 */
	public static void runTask(SherlockFragmentActivity activity, int action, Object parameters) {
		FragmentManager fm = activity.getSupportFragmentManager();
		Fragment f = fm.findFragmentByTag(FRAGMENT_TAG);
		if (f != null && f instanceof AsyncTaskFragment) {
			((AsyncTaskFragment) f).mTasks.put(action, parameters);
			((AsyncTaskFragment) f).run(action, parameters);
		} else {
			throw new IllegalStateException("Your activity must implement TaskFragmentCallbacks and call AsyncTaskFragment.attachAsyncTaskFragment() in its onCreate " +
					"method.");
		}
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
		Iterator<Integer> i = mTasks.keySet().iterator();
		Integer key;
		while (i.hasNext()) {
			key = i.next();
			mCallbacks.onPreExecute(key, mTasks.get(key));
		}
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
					mObject = mCallbacks.doInBackGround(action, parameters);
				}
				return null;
			}

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

