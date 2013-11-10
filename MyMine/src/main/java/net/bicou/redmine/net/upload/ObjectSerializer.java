package net.bicou.redmine.net.upload;

import android.content.Context;

import net.bicou.redmine.util.L;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Created by bicou on 07/08/13.
 */
public abstract class ObjectSerializer<T> {
	private enum FieldChange {
		NO_CHANGE,

		// Native object (integer, double, string...)
		ADDED,
		CHANGED,
		REMOVED,

		// Object (with an ID field)
		ID_ADDED,
		ID_CHANGED,
		ID_REMOVED,
	}

	public enum RemoteOperation {
		UNSET,
		ADD,
		EDIT,
		DELETE,
		NO_OP,
	}

	protected Context mContext;
	private String mObjectName;
	protected T mNewObject;
	protected T mOldObject;
	private RemoteOperation mRemoteOperation = RemoteOperation.UNSET;
	private HashMap<String, Object> mFields;

	public ObjectSerializer(final Context context, String objectName, T newObject) {
		mObjectName = objectName;
		mContext = context;
		mNewObject = newObject;
		mOldObject = getOldObject();
	}

	public void build() {
		mFields = getDeltas();

		if (mRemoteOperation == RemoteOperation.UNSET) {
			if (mOldObject == null) {
				mRemoteOperation = RemoteOperation.ADD;
			} else if (mFields.size() > 0) {
				mRemoteOperation = RemoteOperation.EDIT;
			} else {
				mRemoteOperation = RemoteOperation.NO_OP;
			}
		}
	}

	public ObjectSerializer(final Context context, String objectName, T newObject, boolean isDeleteOperation) {
		this(context, objectName, newObject);
		if (isDeleteOperation) {
			mRemoteOperation = RemoteOperation.DELETE;
		}
	}

	public RemoteOperation getRemoteOperation() {
		return mRemoteOperation;
	}

	public String convertToJson() {
		if (mFields.size() > 0) {
			mFields.put("updated_on", new GregorianCalendar());
			return buildJson(mFields);
		}
		return null;
	}

	/**
	 * Builds a HashMap of the differences between the two objects
	 */
	private final HashMap<String, Object> getDeltas() {
		HashMap<String, Object> fields = new HashMap<String, Object>();
		saveAdditionalParameters(fields);
		String[] fieldNames = getDefaultFields();
		for (String field : fieldNames) {
			handleField(mNewObject, mOldObject, field, fields);
		}
		return fields;
	}

	protected abstract T getOldObject();

	/**
	 * The list of fields members that should be serialized
	 */
	protected abstract String[] getDefaultFields();

	/**
	 * Can be overriden to add other data to the upload
	 */
	protected void saveAdditionalParameters(HashMap<String, Object> fields) {
	}

	/**
	 * Converts the HashMap of fields into a json representation
	 */
	private String buildJson(final HashMap<String, Object> fields) {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
		Iterator<String> iterator = fields.keySet().iterator();
		Object item;
		String key;
		boolean first = true;

		String json = String.format(Locale.ENGLISH, "{\"%s\":{", mObjectName);
		while (iterator.hasNext()) {
			if (first) {
				first = false;
			} else {
				json += ",";
			}

			key = iterator.next();
			item = fields.get(key);

			json += "\"" + key + "\":";

			if (item instanceof Integer || item instanceof Long) {
				json += String.format(Locale.ENGLISH, "%d", ((Number) item).longValue());
			} else if (item instanceof Float || item instanceof Double) {
				json += String.format(Locale.ENGLISH, "%.2f", ((Number) item).doubleValue());
			} else if (item instanceof String) {
				json += String.format(Locale.ENGLISH, "\"%s\"", ((String) item).replace("\"", "\\\""));
			} else if (item instanceof Calendar) {
				json += String.format(Locale.ENGLISH, "\"%s\"", sdf.format(((Calendar) item).getTime()));
			} else if (item instanceof Boolean) {
				json += (Boolean) item;
			} else {
				try {
					Field id = item.getClass().getField("id");
					json += (Long) id.get(item);
				} catch (NoSuchFieldException e) {
					L.e("Unhandled native type: " + item.getClass(), e);
				} catch (IllegalAccessException e) {
					L.e("Shouldn't happen", e);
				}
			}
		}
		json += "}}";

		return json;
	}

	/**
	 * Try to get the value of one field from an object, and put that value in the HashMap
	 * <p/>
	 * If the object is a native object (Integer/Long, Float/Double, String, Boolean), its value is added to the Map.  The map key will be the field name.
	 * <p/>
	 * If the object is not a native object,this method will try to get the "id" field of this object. The Map key will be fieldName + "_id".
	 */
	protected void handleField(T newObject, T oldObject, String fieldName, Map<String, Object> fields) {
		Field field;
		try {
			field = newObject.getClass().getField(fieldName);
		} catch (NoSuchFieldException e) {
			L.e("Can't upload \"+mObjectName+\" change: " + fieldName, e);
			return;
		}

		Object oldOne;
		try {
			oldOne = field.get(oldObject);
		} catch (IllegalAccessException e) {
			L.e("Can't upload \"+mObjectName+\" change: " + fieldName, e);
			return;
		} catch (NullPointerException e) {
			oldOne = null;
		}

		Object newOne;
		try {
			newOne = field.get(newObject);
		} catch (IllegalAccessException e) {
			L.e("Can't upload " + mObjectName + " change: " + fieldName, e);
			return;
		} catch (NullPointerException e) {
			newOne = null;
		}

		FieldChange fieldChange = detectFieldChange(newOne, oldOne);
		if (fieldChange != FieldChange.NO_CHANGE) {
			L.d("Field " + fieldName + " " + fieldChange + ": o/n=" + oldOne + "/" + newOne);
		}
		switch (fieldChange) {
		case REMOVED:
			Object deletedValue;
			if (newOne instanceof Integer || newOne instanceof Long || newOne instanceof Float || newOne instanceof Double || newOne instanceof Calendar) {
				deletedValue = 0;
			} else {
				deletedValue = "";
			}
			fields.put(fieldName, deletedValue);
			break;

		case ADDED:
		case CHANGED:
			fields.put(fieldName, newOne);
			break;

		case ID_ADDED:
		case ID_CHANGED:
			try {
				Field id = newOne.getClass().getField("id");
				fields.put(fieldName + "_id", id.get(newOne));
			} catch (NoSuchFieldException e) {
				L.e("Shouldn't happen.", e);
			} catch (IllegalAccessException e) {
				L.e("Shouldn't happen.", e);
			}
			break;
		}
	}

	/**
	 * Detects the kind of change between the two values
	 */
	private FieldChange detectFieldChange(Object newOne, Object oldOne) {
		if (newOne == null && oldOne == null) {
			return FieldChange.NO_CHANGE;
		}

		if (oldOne == null) {
			try {
				Long newId = (Long) newOne.getClass().getField("id").get(newOne);
				if (newId > 0) {
					return FieldChange.ID_ADDED;
				} else {
					return FieldChange.NO_CHANGE;
				}
			} catch (NoSuchFieldException e) {
				return FieldChange.ADDED;
			} catch (IllegalAccessException e) {
				L.e("Should not happen", e);
				throw new IllegalStateException(e);
			}
		}

		if (newOne == null) {
			try {
				Long oldId = (Long) oldOne.getClass().getField("id").get(oldOne);
				if (oldId > 0) {
					return FieldChange.ID_REMOVED;
				} else {
					return FieldChange.NO_CHANGE;
				}
			} catch (NoSuchFieldException e) {
				return FieldChange.REMOVED;
			} catch (IllegalAccessException e) {
				L.e("Should not happen", e);
				throw new IllegalStateException(e);
			}
		}

		if (newOne instanceof Integer || newOne instanceof Long) {
			Long n = ((Number) newOne).longValue();
			Long o = ((Number) oldOne).longValue();
			if (o.equals(n)) {
				return FieldChange.NO_CHANGE;
			} else {
				return FieldChange.CHANGED;
			}
		}

		if (newOne instanceof Float || newOne instanceof Double) {
			Double n = ((Number) newOne).doubleValue(), o = ((Number) oldOne).doubleValue();
			if (o.equals(n)) {
				return FieldChange.NO_CHANGE;
			} else {
				return FieldChange.CHANGED;
			}
		}

		if (newOne instanceof String) {
			if (((String) newOne).compareTo((String) oldOne) == 0) {
				return FieldChange.NO_CHANGE;
			} else {
				return FieldChange.CHANGED;
			}
		}

		if (newOne instanceof Calendar) {
			if (((Calendar) newOne).compareTo((Calendar) oldOne) == 0) {
				return FieldChange.NO_CHANGE;
			} else {
				return FieldChange.CHANGED;
			}
		}

		if (newOne instanceof Boolean) {
			Boolean o = (Boolean) oldOne, n = (Boolean) newOne;
			if ((o == Boolean.FALSE && n == Boolean.TRUE) || (o == Boolean.TRUE && n == Boolean.FALSE)) {
				return FieldChange.CHANGED;
			} else {
				return FieldChange.NO_CHANGE;
			}
		}

		try {
			Field idField = newOne.getClass().getField("id");
			Long oldId = (Long) idField.get(oldOne), newId = (Long) idField.get(newOne);
			if (oldId <= 0 && newId > 0) {
				return FieldChange.ID_ADDED;
			}
			if (oldId > 0 && newId <= 0) {
				return FieldChange.ID_REMOVED;
			}
			if (oldId.equals(newId)) {
				return FieldChange.NO_CHANGE;
			}
			return FieldChange.ID_CHANGED;
		} catch (NoSuchFieldException e) {
			L.e("Unhandled native type: " + newOne.getClass().getName(), e);
		} catch (IllegalAccessException e) {
			L.e("Should not happen", e);
		}

		throw new IllegalStateException("Unreachable code (well, should be)");
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " { operation: " + getRemoteOperation() + ", # of changes: " + (mFields == null ? 0 : mFields.size()) + " }";
	}
}
