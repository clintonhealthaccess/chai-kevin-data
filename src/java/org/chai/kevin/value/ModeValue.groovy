package org.chai.kevin.value;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chai.kevin.Period;
import org.chai.kevin.data.Calculation;
import org.chai.kevin.data.Mode;
import org.chai.kevin.data.Type;
import org.chai.location.CalculationLocation;

public class ModeValue extends CalculationValue<ModePartialValue> {
	private static final Log log = LogFactory.getLog(ModeValue.class);
	
	public ModeValue(Set<ModePartialValue> calculationPartialValues, Mode calculation, Period period, CalculationLocation location) {
		super(new ArrayList<ModePartialValue>(calculationPartialValues), calculation, period, location);
	}
	
	public ModeValue(List<ModePartialValue> calculationPartialValues, Mode calculation, Period period, CalculationLocation location) {
		super(calculationPartialValues, calculation, period, location);
	}

	@Override
	public boolean isNull(){
		Value value = getValue()
		return value == null || value.isNull();
	}
	
	@Override
	public Value getValue() {
		List<Value> values = getValueList();
		if (values != null && values.size() > 0) return values.get(0);
		else return null;
	}

	/**
	 * Returns the mode value.
	 * if value == null, returns null
	 * if value != null, returns List<Value> maxModeValues
	 * 
	 * @return the list of modes
	 */ 
	public List<Value> getValueList() {	
		//if location = data location
		if(getLocation().collectsData()){
			if (getCalculationPartialValues().size() > 1) throw new IllegalStateException("Calculation for DataLocation must contain only 1 partial value");
		}
		
		//builds the mode map from the partial value mode maps
		Map<String,Double> modeMap = null;
		for (ModePartialValue partialValue : getCalculationPartialValues()) {
			if (log.isDebugEnabled()) log.debug("partialValue.location(partialValue="+partialValue.getValue()+")");
			Value partialValueMapValue = partialValue.getValue();
			if (!partialValueMapValue.isNull()) {
				// exclude null values from mode
				if (modeMap == null) modeMap = new HashMap<String,Double>();
				Map<String,Value> partialValueModeMap =  partialValueMapValue.getMapValue();
				if(partialValueModeMap != null){
					
					//if location = data location
					if(getLocation().collectsData()){
						if (partialValueModeMap.size() > 1) throw new IllegalStateException("Calculation for DataLocation must contain only 1 partial value mode value");
					}
					
					for(String partialModeValue : partialValueModeMap.keySet()){
						Double modeCount = 0d;
						Double partialModeCount = partialValueModeMap.get(partialModeValue).getNumberValue().doubleValue();
						if (log.isDebugEnabled()) log.debug("partialValue.location(modeJsonValue="+partialModeValue+", partialModeCount="+partialModeCount+")");
						if(modeMap.containsKey(partialModeValue)){
							modeCount = modeMap.get(partialModeValue);
						}
						modeCount += partialModeCount;
						modeMap.put(partialModeValue, modeCount);
						if (log.isDebugEnabled()) log.debug("partialValue.location(modeJsonValue="+partialModeValue+", modeCount="+modeCount+")");
					}
				}
			}
		}
		
		List<Value> maxModeValues = null;
		if(modeMap != null){
			maxModeValues = new ArrayList<Value>();
			// finds the mode max count
			Double maxCount = 0d;
			for(String modeValue : modeMap.keySet()){
				Double modeCount = modeMap.get(modeValue);
				if(modeCount > maxCount) maxCount = modeCount;
				if (log.isDebugEnabled()) log.debug("modeValue.location(maxCount="+maxCount+")");
			}
			// collects the modes
			for(String modeValue : modeMap.keySet()){
				Double modeCount = modeMap.get(modeValue);
				if(modeCount.equals(maxCount)){
					
					Type type = getData().getType();
					if (log.isDebugEnabled()) log.debug("modeValue.location(type="+type+")");
										
					if(type != null){
						Value maxModeValue = type.getValueFromJaql(modeValue);
						if (log.isDebugEnabled()) log.debug("modeValue.location(maxModeValue="+maxModeValue+")");
						maxModeValues.add(maxModeValue);
						if (log.isDebugEnabled()) log.debug("modeValue.location(maxModeValuesSize="+maxModeValues.size()+")");
					}
				}
					
				if (log.isDebugEnabled()) log.debug("modeValue.location(modeValue="+modeValue+", modeCount="+modeCount+")");
			}
		}
		
		//creates the value, in this case, a list of modes
		if (log.isDebugEnabled()) log.debug("modeValue.location(maxModeValues="+maxModeValues+")");
		return maxModeValues;
	}
	
	@Override
	public String toString() {
		return "ModeValue [getValue()=" + getValue() + "]";
	}

	@Override
	public Date getTimestamp() {
		return null;
	}

	@Override
	public Value getAverage() {
		// TODO Auto-generated method stub
		return null;
	}
	
}