package org.chai.kevin.value;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chai.kevin.json.JSONValue;
import org.chai.kevin.util.DataUtils;

public class Value extends JSONValue {
	
	private static final Log log = LogFactory.getLog(Value.class);
	
	public static final String MAP_KEY = "map_key";
	public static final String MAP_VALUE = "map_value";
	public static final String VALUE_STRING = "value";
	
	private static final String[] KEYWORDS = new String[]{MAP_KEY, MAP_VALUE, VALUE_STRING};
	
	public static final Value NULL_INSTANCE() {
		return new Value("{"+VALUE_STRING+": null}");
	}
	
	public Value() {super();}
	
	public Value(String jsonValue) {
		super(jsonValue);
	}
	
	// use this method with caution, never set directly a JSONObject coming
	// from another Value, as it could cause side effects
	// should be "protected"
	public Value(JSONObject object) {
		super(object);
	}
	
	private List<Value> listValue = null;
	private Map<String, Value> mapValue = null;
	private String stringValue = null;
	private String enumValue = null;
	private Number numberValue = null;
	private Boolean booleanValue = null;
	private Date dateValue = null;
	
	protected void clearCache() {
		this.listValue = null;
		this.mapValue = null;
		this.numberValue = null;
		this.stringValue = null;
		this.enumValue = null;
		this.booleanValue = null;
		this.dateValue = null;
	}
	
	@Override
	protected String[] getReservedKeywords() {
		return KEYWORDS;
	}
	
	public boolean isNull() {
		return JSONUtils.isNull(getJsonObject().get(VALUE_STRING));
	}
	
	public Value getValueWithoutAttributes() {
		if (isNull()) return Value.NULL_INSTANCE();
		else {
			JSONObject object = new JSONObject();
			try {
				object.put(VALUE_STRING, value.get(VALUE_STRING));
			} catch (JSONException e) {
				return null;
			}
			return new Value(object);
		}
	}
	
	public Number getNumberValue() {
		if (numberValue == null) {
			try {
				numberValue = getJsonObject().getDouble(VALUE_STRING);
			} catch (JSONException e) {
				numberValue = null;
				if (log.isDebugEnabled()) log.debug("value(numberValue="+numberValue+")");
			}
		}
		return numberValue;
	}
	
	public String getStringValue() {
		if (stringValue == null) {
			try {
				if (isNull()) stringValue = null;
				else stringValue = getJsonObject().getString(VALUE_STRING);
			} catch (JSONException e) {
				stringValue = null;
				if (log.isDebugEnabled()) log.debug("value(stringValue="+stringValue+")");
			}
		}
		return stringValue;
	}
	
	public Boolean getBooleanValue() {
		if (booleanValue == null) {
			try {
				booleanValue = getJsonObject().getBoolean(VALUE_STRING);
			} catch (JSONException e) {
				booleanValue = null;
				if (log.isDebugEnabled()) log.debug("value(booleanValue="+booleanValue+")");
			}
		}
		return booleanValue;
	}
	
	public String getEnumValue() {
		// TODO think that through
		if (enumValue == null) {
			try {
				enumValue = getJsonObject().getString(VALUE_STRING);
			} catch (JSONException e) {
				enumValue = null;
				if (log.isDebugEnabled()) log.debug("value(enumValue="+enumValue+")");
			}
		}
		return enumValue;
	}
	
	public Date getDateValue() {
		if (dateValue == null) {
			try {
				dateValue = DataUtils.parseDate(getJsonObject().getString(VALUE_STRING));
			} catch (JSONException e) {
				dateValue = null;
				if (log.isDebugEnabled()) log.debug("value(dateValue="+dateValue+")");
			} catch (ParseException e) {
				//TODO this should never be null!
				dateValue = null;
				if (log.isDebugEnabled()) log.debug("value(dateValue="+dateValue+")");
			}
		}
		return dateValue;
	}
	
	public List<Value> getListValue() {
		if (listValue == null) {
			try {
				List<Value> result = new ArrayList<Value>();
				JSONArray array = getJsonObject().getJSONArray(VALUE_STRING);
				for (int i = 0; i < array.size(); i++) {
					JSONObject object = array.getJSONObject(i);
					result.add(new Value(object));
				}
				listValue = result;
			} catch (JSONException e) {
				listValue = null;
				if (log.isDebugEnabled()) log.debug("value(listValue="+listValue+")");
			}
		}
		return listValue;
	}
	
	public Map<String, Value> getMapValue() {
		if (mapValue == null) {
			try {
				Map<String, Value> result = new LinkedHashMap<String, Value>();
				JSONArray array = getJsonObject().getJSONArray(VALUE_STRING);
				for (int i = 0; i < array.size(); i++) {
					JSONObject object = array.optJSONObject(i);
					try {
						result.put(object.getString(MAP_KEY), new Value(object.getJSONObject(MAP_VALUE)));
					} catch (JSONException e) {}
				}
				mapValue = result;
			} catch (JSONException e) {
				mapValue = null;
				if (log.isDebugEnabled()) log.debug("value(mapValue="+mapValue+")");
			}
		}
		return mapValue;
	}

	public static Value VALUE_DATE(Date value) {
		return new Value("{\""+VALUE_STRING+"\":"+DataUtils.formatDate(value)+"}");
	}
	
	public static Value VALUE_BOOL(Boolean value) {
		return new Value("{\""+VALUE_STRING+"\":"+value.toString()+"}");
	}

	public static Value VALUE_NUMBER(Number value) {
		return new Value("{\""+VALUE_STRING+"\":"+value.toString()+"}");
	}

	public static Value VALUE_STRING(String value) {
		return new Value("{\""+VALUE_STRING+"\":\""+value.toString()+"\"}");
	}

	public static Value VALUE_LIST(List<Value> values) {
		return new Value("{\""+VALUE_STRING+"\":"+values.toString()+"}");
	}

	public static Value VALUE_MAP(Map<String, Value> values) {
		StringBuffer json = new StringBuffer();
		for (Entry<String, Value> entry : values.entrySet()) {
			json.append("{\""+MAP_KEY+"\":\""+entry.getKey()+"\",\""+MAP_VALUE+"\":"+entry.getValue().toString()+"}");
			json.append(",");
		}
		return new Value("{\""+VALUE_STRING+"\":["+json.toString()+"]}");
	}
	
}
