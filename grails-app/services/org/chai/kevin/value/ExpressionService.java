package org.chai.kevin.value;

/* 
 * Copyright (c) 2011, Clinton Health Access Initiative.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chai.kevin.JaqlService;
import org.chai.location.LocationService;
import org.chai.kevin.Period;
import org.chai.kevin.PeriodService;
import org.chai.kevin.data.Calculation;
import org.chai.kevin.data.Data;
import org.chai.kevin.data.DataElement;
import org.chai.kevin.data.DataService;
import org.chai.kevin.data.NormalizedDataElement;
import org.chai.kevin.data.Type;
import org.chai.location.CalculationLocation;
import org.chai.location.DataLocation;
import org.chai.location.DataLocationType;
import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * This service deals with calculating the values of normalized data elements and calculations.
 * It does not store the calculated values into the database, this has to be done separately.
 */
public class ExpressionService {

	private static final Log log = LogFactory.getLog(ExpressionService.class);
	private static final Log expressionLog = LogFactory.getLog("ExpressionLog");
	
	private DataService dataService;
	private LocationService locationService;
	private ValueService valueService;
	private JaqlService jaqlService;
	private PeriodService periodService;
	private SessionFactory sessionFactory;
	
	public static class StatusValuePair {
		public Status status = null;
		public Value value = null;
		
		@Override
		public String toString() {
			return "StatusValuePair [status=" + status + ", value=" + value + "]";
		}
	}
	
	/**
	 * Evaluates the list of partial values for a particular calculation, location and period. 
	 * It will return at least as many partial values as there are data location types in the system. 
	 * 
	 * Using this method will not trigger caclulating the dependencies of the specified calculation
	 * if there are any. They have to have been calculated beforehand for this method to return
	 * correct result. None of the parameters can be null.
	 *
	 * @param calculation the calculation whose expression will be evaluated
	 * @param location the location for which the expression will be evaluated
	 * @param period the period for which the expression will be evaluated
	 *
	 * @return the list of partial values, at least one for each data location type in the system
	 */
	@Transactional(readOnly=true)
	public <T extends CalculationPartialValue> List<T> calculatePartialValues(Calculation<T> calculation, CalculationLocation location, Period period) {
		if (log.isDebugEnabled()) log.debug("calculatePartialValues(calculation="+calculation+",period="+period+",location="+location+")");
		
		List<T> result = new ArrayList<T>();
		for (String expression : calculation.getPartialExpressions()) {
			result.addAll(calculatePartialValues(calculation, expression, location, period));
		}
		return result;
	}
	
	private <T extends CalculationPartialValue> Set<T> calculatePartialValues(Calculation<T> calculation, String expression, CalculationLocation location, Period period) {
		if (log.isDebugEnabled()) log.debug("calculatePartialValues(expression="+expression+",period="+period+",location="+location+")");
		
		Set<T> result = new HashSet<T>();
		Map<DataLocationType, List<DataLocation>> dataLocationsByType = getDataLocationsByType(location);
		
		for (DataLocationType type : locationService.listTypes()) {
			List<DataLocation> dataLocations = dataLocationsByType.get(type);
			
			if (dataLocations != null && !dataLocations.isEmpty()) {
				Map<DataLocation, StatusValuePair> values = new HashMap<DataLocation, StatusValuePair>();
				
				Map cache = new HashMap();				
				Type calculationType = calculation.getType();
				for (DataLocation dataLocation : dataLocations) {					
					StatusValuePair statusValuePair = getExpressionStatusValuePair(expression, calculationType, period, dataLocation, DataElement.class, cache);
					values.put(dataLocation, statusValuePair);
				}
				result.add(calculation.getCalculationPartialValue(expression, values, location, period, type));
			}
		}
		return result;
	}
	
	private Map<DataLocationType, List<DataLocation>> getDataLocationsByType(CalculationLocation location) {
		Map<DataLocationType, List<DataLocation>> result = new HashMap<DataLocationType, List<DataLocation>>();
		List<DataLocation> dataLocations = location.collectDataLocations(null);
		for (DataLocation dataLocation : dataLocations) {
			DataLocationType type = dataLocation.getType();
			if (!result.containsKey(type)) {
				result.put(type, new ArrayList<DataLocation>());
			}
			List<DataLocation> locations = result.get(type);
			locations.add(dataLocation);
		}
		return result;
	}
	
	/**
	 * Evaluates the specified normalized data element for the given location and period. 
	 *
	 * Using this method will not trigger caclulating the dependencies of the specified normalized data element
	 * if there are any. They have to have been calculated beforehand for this method to return
	 * correct result. None of the parameters can be null.
	 *
	 * @param normalizedDataElement the normalized data element whose value to evaluate
	 * @param dataLocation the data location for which to evaluate the normalized data element
	 * @param period the period for which to evalute the normalized data element
	 *
	 * @return the value
	 */
	@Transactional(readOnly=true)
	public NormalizedDataElementValue calculateValue(NormalizedDataElement normalizedDataElement, DataLocation dataLocation, Period period) {
		if (log.isDebugEnabled()) log.debug("calculateValue(normalizedDataElement="+normalizedDataElement+",period="+period+",dataLocation="+dataLocation+")");
		
		String expression = normalizedDataElement.getExpression(period, dataLocation.getType().getCode());
		
		StatusValuePair statusValuePair = getExpressionStatusValuePair(expression, normalizedDataElement.getType(), period, dataLocation, DataElement.class, null);
		NormalizedDataElementValue expressionValue = new NormalizedDataElementValue(statusValuePair.value, statusValuePair.status, dataLocation, normalizedDataElement, period);
		
		if (log.isDebugEnabled()) log.debug("getValue()="+expressionValue);
		return expressionValue;
	}

	private <T extends DataElement<S>, S extends DataValue> DataValue findInCache(Map<T, Map<CalculationLocation, S>> cache, T data, DataLocation dataLocation, Period period) {
		if (cache == null) {
			return valueService.getDataElementValue(data, dataLocation, period);
		}
		else {
			if (!cache.containsKey(data)) {
				if (log.isDebugEnabled()) log.debug("filling cache for data: "+data);

				List<S> values = valueService.listDataValues(data, null, period, new HashMap());
				Map<CalculationLocation, S> locationMap = new HashMap<CalculationLocation, S>();
				for (S value : values) {
					locationMap.put(value.getLocation(), value);
					sessionFactory.getCurrentSession().evict(value);
				}
				cache.put(data, locationMap);
				
				if (log.isDebugEnabled()) log.debug("done filling cache for data: "+data);
			}
			else {
				if (log.isDebugEnabled()) log.debug("found values in cache for data: "+data);	
			}
			return cache.get(data).get(dataLocation);
		}
	}

	// location has to be a dataLocation
	private <T extends DataElement<S>, S extends DataValue> StatusValuePair getExpressionStatusValuePair(String expression, Type type, Period period, DataLocation dataLocation, Class<T> clazz, Map cache) {
		if (expressionLog.isInfoEnabled()) expressionLog.info("getting expression status-value for: expression={"+expression+"}, type={"+type+"}, period={"+period+"}, dataLocation={"+dataLocation+"}");
		
		if (log.isDebugEnabled())log.debug("getExpressionStatusValuePair(expression="+expression+", type="+type+", period="+period+", dataLocation="+dataLocation+", clazz="+clazz);
		
		StatusValuePair statusValuePair = new StatusValuePair();
		if (expression == null || expression.trim().isEmpty()) {
			statusValuePair.status = Status.MISSING_EXPRESSION;
			statusValuePair.value = Value.NULL_INSTANCE();
		}
		else {
			Map<String, T> datas = getDataInExpression(expression, clazz);
			if (hasNullValues(datas.values())) {
				if (expressionLog.isInfoEnabled()) expressionLog.info("data elements are missing");
				statusValuePair.value = Value.NULL_INSTANCE();
				statusValuePair.status = Status.MISSING_DATA_ELEMENT;
			}
			else {
				Map<String, Value> valueMap = new HashMap<String, Value>();
				Map<String, Type> typeMap = new HashMap<String, Type>();
				
				for (Entry<String, T> entry : datas.entrySet()) {
					DataValue dataValue = findInCache(cache, entry.getValue(), dataLocation, period);
					Value value = dataValue==null?null:dataValue.getValue();
					if (value == null) value = Value.NULL_INSTANCE();
					valueMap.put(entry.getValue().getId().toString(), value);
					typeMap.put(entry.getValue().getId().toString(), entry.getValue().getType());					
				}
				if (expressionLog.isTraceEnabled()) expressionLog.trace("values and types: valueMap={"+valueMap+"}, typeMap={"+typeMap+"}");
				
				try {
					if (expressionLog.isInfoEnabled()) expressionLog.info("no null values found, evaluating expression");
					statusValuePair.value = jaqlService.evaluate(expression, type, valueMap, typeMap);
					statusValuePair.status = Status.VALID;
				} catch (IllegalArgumentException e) {
					if (expressionLog.isErrorEnabled()) expressionLog.error("expression={"+expression+"}", e);
					if (expressionLog.isTraceEnabled()) expressionLog.trace("type={"+type+"}, period={"+period+"}, dataLocation={"+dataLocation+"}, valueMap={"+valueMap+"}, typeMap={"+typeMap+"}");
					if (log.isWarnEnabled()) log.warn("there was an error evaluating expression: "+expression, e);
					statusValuePair.value = Value.NULL_INSTANCE();
					statusValuePair.status = Status.ERROR;
				}
			}
		}
		if (expressionLog.isInfoEnabled()) expressionLog.info("returning result={"+statusValuePair+"}");
		return statusValuePair;
	}

	/** 
	 * Returns false if the given expression is invalid, true otherwise.
	 * 
	 * It will look for data in the formula, and validate them against the specified class.
	 * If for example, the allowedClazz is NormalizedDataElement, and a calculation is found
	 * in the expression, it will not be valid, because they are not compatible.
	 *
	 * @param expression the expression to validate
	 * @param allowedClazz the class of data that is allowed inside this formula
	 * 
	 * @return true if valid, false if invalid
	 */
	// TODO do this for validation rules
	@Transactional(readOnly=true)
	public <T extends Data<?>> boolean expressionIsValid(String expression, Class<T> allowedClazz) throws IllegalArgumentException {
		Map<String, T> variables = getDataInExpression(expression, allowedClazz);
		
		if (hasNullValues(variables.values())) return false;
		
		Map<String, String> jaqlVariables = new HashMap<String, String>();
		for (Entry<String, T> variable : variables.entrySet()) {
			Type type = variable.getValue().getType();
			jaqlVariables.put(variable.getKey(), type.getJaqlValue(type.getPlaceHolderValue()));
		}
		
		jaqlService.getJsonValue(expression, jaqlVariables);
		return true;
    }
	
	/**
	 * Returns a map of all the data in the given expression. The key of the map will be the "$<dataId>" found in the
	 * expression and the value the corresponding data or null if no data was found or was not of the specified class.
	 * 
	 * @param expression the expression from which to retrieve the data
	 * @param clazz the clazz for which to retrieve the data
	 * @return a map of the data
	 */
	@Transactional(readOnly=true)
    public <T extends Data<?>> Map<String, T> getDataInExpression(String expression, Class<T> clazz) {
    	if (log.isTraceEnabled()) log.trace("getDataInExpression(expression="+expression+", clazz="+clazz+")");
    	
        Map<String, T> dataInExpression = new HashMap<String, T>();
    	Set<String> placeholders = getVariables(expression);

    	for (String placeholder : placeholders) {
            T data = null;
            try {
            	data = dataService.getData(Long.parseLong(placeholder.replace("$", "")), clazz);
            }
            catch (NumberFormatException e) {
            	if (log.isErrorEnabled()) log.error("wrong format for dataelement: "+placeholder);
            }

            if (data != null && !clazz.isAssignableFrom(data.getClass())) {
            	if (log.isWarnEnabled()) log.warn("the data class "+data.getClass()+" is not assignable from the specified class: "+clazz);
            	data = null;
            }
        	dataInExpression.put(placeholder, data);
        }
    	
    	if (log.isTraceEnabled()) log.trace("getDataInExpression()="+dataInExpression);
        return dataInExpression;
    }
	
	/**
	 * Returns true if there are circular dependencies in the given normalized data element and false
	 * if not. A circular dependency happens when an expression in a normalized data element references
	 * itself either directly or one of the normalized data elements it uses references it.
	 *
	 * @param dataElement the normalized data element for which to check for circular dependency
	 * @return true if there are circular dependencies in the given normalized data element and false
	 * if not
	 */
	@Transactional(readOnly=true)
	public boolean hasCircularDependency(NormalizedDataElement dataElement) {
		List<NormalizedDataElement> path = new ArrayList<NormalizedDataElement>();
		path.add(dataElement);
		for (DataLocationType dataLocationType : locationService.listTypes()) {
			for (Period period : periodService.listPeriods()) {
				if (hasCircularDependency(dataElement, period, dataLocationType, new ArrayList<NormalizedDataElement>(path))) return true; 
			}
		}
		return false;
	}
	
	private boolean hasCircularDependency(NormalizedDataElement dataElement, Period period, DataLocationType locationType, List<NormalizedDataElement> pathToExpression) {
		String expression = dataElement.getExpression(period, locationType.getCode());
		if (expression == null) return false;
		Map<String, NormalizedDataElement> dataElementsInExpression = getDataInExpression(expression, NormalizedDataElement.class);
		for (NormalizedDataElement dependency : dataElementsInExpression.values()) {
			if (dependency != null) {
				if (pathToExpression.contains(dependency)) return true;
				else {
					List<NormalizedDataElement> pathToExpressionWithDependency = new ArrayList<NormalizedDataElement>(pathToExpression);
					pathToExpressionWithDependency.add(dependency);
					if (hasCircularDependency(dependency, period, locationType, pathToExpressionWithDependency)) return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns all the data variables found in the given expression. A data variable is 
	 * when a data is referred to using the $<dataId> notation. Returns a set of 
	 * $<dataId> in the expression.
	 * 
	 * @param expression an expression in which to look for data variables
	 * @return a set of all "$<dataId>" in the expression
	 */
    public static Set<String> getVariables(String expression) {
    	Set<String> placeholders = new HashSet<String>();
        if ( expression != null ) {
        	placeholders = new HashSet<String>();
            final Matcher matcher = Pattern.compile("\\$\\d+").matcher( expression );
            
            while (matcher.find())  {
            	String match = matcher.group();
	            placeholders.add(match);
            }
        }
        return placeholders;
    }
    
	/**
	 * Replaces the data variables in the given expression by the corresponding string given
	 * in the mapping passed as a parameter.
	 * 
	 * Example: given the expression "$1 + $2", and the mapping {"1": "test", "2": "blu"}, the
	 * result would be "test + blu"
	 * 
	 * @param mapping the replacement mapping
	 * @return the new expression with the replaced mapping
	 */
    public static String convertStringExpression(String expression, Map<String, String> mapping) {
        String result = expression;
        for (Entry<String, String> entry : mapping.entrySet()) {
        	// TODO validate key
        	if (!Pattern.matches("\\$\\d+", entry.getKey())) throw new IllegalArgumentException("key does not match expression pattern: "+entry);
        	result = result.replaceAll("\\"+entry.getKey()+"(\\z|\\D|$)", entry.getValue().replace("$", "\\$")+"$1");
		}
        return result;
    }
	
	private static <T extends Object> boolean hasNullValues(Collection<T> values) {
		for (Object object : values) {
			if (object == null) return true;
		}
		return false;
	}
	
	public void setDataService(DataService dataService) {
		this.dataService = dataService;
	}
	
	public void setValueService(ValueService valueService) {
		this.valueService = valueService;
	}
	
	public void setPeriodService(PeriodService periodService) {
		this.periodService = periodService;
	}
	
	public void setLocationService(LocationService locationService) {
		this.locationService = locationService;
	}
	
	public void setJaqlService(JaqlService jaqlService) {
		this.jaqlService = jaqlService;
	}
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
}
