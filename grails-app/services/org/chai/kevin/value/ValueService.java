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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chai.kevin.Period;
import org.chai.kevin.data.Calculation;
import org.chai.kevin.data.Data;
import org.chai.kevin.data.DataElement;
import org.chai.kevin.data.NormalizedDataElement;
import org.chai.location.CalculationLocation;
import org.chai.location.DataLocation;
import org.chai.location.DataLocationType;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Transactional;
import org.chai.kevin.util.DataUtils;

/**
 * This class deals with retrieving and storing values to the database. It
 * does not trigger any expression calculation.
 */
public class ValueService {

	private static final Log log = LogFactory.getLog(ValueService.class);
	
	private SessionFactory sessionFactory;
	
	/**
	 * Saves the given value and sets the timestamp to the current time. This method will flush the session
	 *
	 * @param value the value to save
	 * @return the saved value
	 */
	@Transactional(readOnly=false)
	public <T extends StoredValue> T save(T value) {
		log.debug("save(value="+value+")");
		
		value.setTimestamp(new Date());
		sessionFactory.getCurrentSession().saveOrUpdate(value);
		
		return value;
	}
	
	/**
	 * Retrieves the value corresponding to the given data, data location and period.
	 *
	 * @param data the data
	 * @param dataLocation the data location
	 * @param period the period
	 * @return the corresponding value
	 */
	@Transactional(readOnly=true)
	public <T extends DataValue> T getDataElementValue(DataElement<T> data, DataLocation dataLocation, Period period) {
		if (log.isDebugEnabled()) log.debug("getDataElementValue(data="+data+", period="+period+", dataLocation="+dataLocation+")");
		Criteria criteria = getCriteria(data, dataLocation, period);
		T result = (T)criteria.uniqueResult();
		if (log.isDebugEnabled()) log.debug("getDataElementValue(...)="+result);
		return result;
	}
	
	/**
	 * Searches for data values belonging to a data location whose name or code matches the
	 * given search term.
	 *
	 * @param text the search term
	 * @param data the data element whose values to search for
	 * @param dataLocation the data location to restrict the search for or null to search all
	 * @param period the period to restrict the search for or null to search in all periods
	 * @param params the map with sort, order, offset and max information for the search
	 * @return a list of values whose location code or name matches
	 */
	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
	public <T extends DataValue> List<T> searchDataValues(String text, DataElement<T> data, DataLocation dataLocation, Period period, Map<String, Object> params) {
		if (log.isDebugEnabled()) log.debug("searchDataValues(text="+text+", data="+data+", period="+period+", dataLocation="+dataLocation+")");
		Criteria criteria = getCriteria(data, dataLocation, period);
		if (dataLocation == null) criteria.createAlias("location", "location");
		addSortAndLimitCriteria(criteria, params);
		addSearchCriteria(criteria, text);
		
		List<T> result = criteria.list();
		if (log.isDebugEnabled()) log.debug("searchDataValues(...)=");
		return result;
	}
	
	private void addSearchCriteria(Criteria criteria, String text) {
		Conjunction textRestrictions = Restrictions.conjunction();
		for (String chunk : StringUtils.split(text)) {
			Disjunction disjunction = Restrictions.disjunction();		
			disjunction.add(Restrictions.ilike("location.code", chunk, MatchMode.ANYWHERE));
			disjunction.add(Restrictions.ilike("location.names_" + DataUtils.getCurrentLocale().getLanguage(), chunk, MatchMode.ANYWHERE));
			textRestrictions.add(disjunction);
		}
		criteria.add(textRestrictions);
	}
	
	private void addSortAndLimitCriteria(Criteria criteria, Map<String, Object> params) {
		if (params.containsKey("sort")) {
			criteria.addOrder(params.get("order").equals("asc")?Order.asc(params.get("sort")+""):Order.desc(params.get("sort")+""));
		}
		
		if (params.get("offset") != null) criteria.setFirstResult((Integer)params.get("offset"));
		if (params.get("max") != null) criteria.setMaxResults((Integer)params.get("max"));
	}
	
	/**
	 * Lists all the data values corresponding to the given data, data location and period. Data location
	 * and period can be null, in which case it lists all the values for all data locations or periods.
	 * 
	 * @param data the data to list values for. Cannot be null.
	 * @param dataLocation the data location to list values for, can be null
	 * @param period the period to list values for, can be null
	 * @param params the map with sort, order, offset and max information for the list
	 * @return the list of values corresponding to the given params
	 */
	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
	public <T extends DataValue> List<T> listDataValues(Data<T> data, DataLocation dataLocation, Period period, Map<String, Object> params) {
		if (log.isDebugEnabled()) log.debug("listDataValues(data="+data+", period="+period+", dataLocation="+dataLocation+")");
		Criteria criteria = getCriteria(data, dataLocation, period);
		addSortAndLimitCriteria(criteria, params);
		
		criteria.setFlushMode(FlushMode.COMMIT);
		List<T> result = criteria.list();
		if (log.isDebugEnabled()) log.debug("listDataValues(...)=");
		return result;
	}
	
	/**
	 * Counts all the data values corresponding to the given data, data location and period. Data location
	 * and period can be null, in which case it counts all the values for all data locations or periods.
	 * 
	 * @param data the data to list values for. Cannot be null.
	 * @param dataLocation the data location to list values for, can be null
	 * @param period the period to list values for, can be null
	 * @param params the map with sort, order, offset and max information for the list
	 * @return the number of values corresponding to the given params
	 */
	public <T extends DataValue> Long countDataValues(String text, Data<T> data, DataLocation dataLocation, Period period) {
		if (log.isDebugEnabled()) log.debug("countDataValues(data="+data+", period="+period+", dataLocation="+dataLocation+")");
		Criteria criteria = getCriteria(data, dataLocation, period);
		if (text != null) {
			criteria.createAlias("location", "location");
			addSearchCriteria(criteria, text);
		}
		
		Long result = (Long)criteria.setProjection(Projections.count("id")).uniqueResult();
		if (log.isDebugEnabled()) log.debug("countDataValues(...)=");
		return result;
	}
	
	private <T extends DataValue> Criteria getCriteria(Data<T> data, DataLocation dataLocation, Period period) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(data.getValueClass());
		criteria.add(Restrictions.eq("data", data));
		if (period != null) criteria.add(Restrictions.eq("period", period));
		if (dataLocation != null) {
			criteria.createAlias("location", "location");
			criteria.add(Restrictions.eq("location", dataLocation));
		}
//		criteria.add(Restrictions.isNull("type"));
		
		return criteria;
	}
	
	/**
	 * Returns the calculation value corresponding the given calculation, location, period and set of location types.
	 *
	 * @param calculation the calculation for which to retrieve the value
	 * @param location the location for which to retrieve the value
	 * @param period the period for which to retrieve the value
	 * @param types the types for which to retrieve the value
	 * @return the calculation value
	 */
	@Transactional(readOnly=true)
	public <T extends CalculationPartialValue> CalculationValue<T> getCalculationValue(Calculation<T> calculation, CalculationLocation location, Period period, Set<DataLocationType> types) {
		if (log.isDebugEnabled()) log.debug("getCalculationValue(calculation="+calculation+", period="+period+", location="+location+", types="+types+")");
		List<T> partialValues = getPartialValues(calculation, location, period, types);
		CalculationValue<T> result = calculation.getCalculationValue(partialValues, period, location);
		if (log.isDebugEnabled()) log.debug("getCalculationValue(...)="+result);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
	private <T extends CalculationPartialValue> List<T> getPartialValues(Calculation<T> calculation, CalculationLocation location, Period period) {
		return (List<T>)sessionFactory.getCurrentSession().createCriteria(calculation.getValueClass())
		.add(Restrictions.eq("period", period))
		.add(Restrictions.eq("location", location))
		.add(Restrictions.eq("data", calculation)).list();
	}
	
	@SuppressWarnings("unchecked")
	public <T extends CalculationPartialValue> List<T> getPartialValues(Calculation<T> calculation, CalculationLocation location, Period period, Set<DataLocationType> types) {
		return (List<T>)sessionFactory.getCurrentSession().createCriteria(calculation.getValueClass())
		.add(Restrictions.eq("period", period))
		.add(Restrictions.eq("location", location))
		.add(Restrictions.eq("data", calculation))
		.add(Restrictions.in("type", types)).list();
	}
	
	/**
	 * Returns the number of values stored in the database for the given period and data.
	 *
	 * @param data the data for which to count values
	 * @param period the period for which to count values
	 * @return the number of stored values
	 */
	public Long getNumberOfValues(Data<?> data, Period period) {
		return (Long)sessionFactory.getCurrentSession().createCriteria(data.getValueClass())
		.add(Restrictions.eq("data", data))
		.add(Restrictions.eq("period", period))
		.setProjection(Projections.count("id"))
		.uniqueResult();
	}
	
	/**
	 * Returns the number of values stored in the database for the given period.
	 *
	 * @param period the period for which to count values
	 * @return the number of stored values
	 */
	public Long getNumberOfValues(Period period) {
		return (Long)sessionFactory.getCurrentSession().createCriteria(StoredValue.class)
		.add(Restrictions.eq("period", period))
		.setProjection(Projections.count("id"))
		.uniqueResult();
	}
	
	/**
	 * Returns the number of values stored in the database for the given data.
	 *
	 * @param data the data for which to count the values
	 * @return the number of stored values
	 */
	// if this is set readonly, it triggers an error when deleting a
	// data element through DataElementController.deleteEntity
	public Long getNumberOfValues(Data<?> data) {
		return (Long)sessionFactory.getCurrentSession().createCriteria(data.getValueClass())
		.add(Restrictions.eq("data", data))
		.setProjection(Projections.count("id"))
		.uniqueResult();
	}
	
	/**
	 * Returns the number of values stored in the database for the given data location restricted
	 * to the given data class.
	 *
	 * @param dataLocation the data location for which to count the values
	 * @param clazz the class to restrict the count for.
	 * @return the number of stored values
	 */
	// if this is set readonly, it triggers an error when deleting a
	// data element through DataElementController.deleteEntity
	public Long getNumberOfValues(DataLocation location, Class<?> clazz) {
		return (Long)sessionFactory.getCurrentSession().createCriteria(clazz)
		.add(Restrictions.eq("location", location))
		.setProjection(Projections.count("id"))
		.uniqueResult();
	}
	
	/** 
	 * Returns the number of values stored in the database with the given data, status and period. The
	 * data has to be a normalized data element otherwise it will throw an @IllegalArgumentException
	 *
	 * @param data the data for which to count the values. Has to be an instance of NormalizedDataElement.
	 * @param status the status to restrict the count to
	 * @param period the period for which to count the values for or null for all periods
	 * @return the number of stored values
	 */
	public Long getNumberOfValues(Data<?> data, Status status, Period period) {
		// TODO allow Calculation here
		if (!(data instanceof NormalizedDataElement)) {
			throw new IllegalArgumentException("wrong data type");
		}
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(data.getValueClass())
			.add(Restrictions.eq("data", data))
			.add(Restrictions.eq("status", status));
		if (period != null) criteria.add(Restrictions.eq("period", period));
		return (Long)criteria	
			.setProjection(Projections.count("id"))
			.uniqueResult();
	}
	
	/**
	 * Delete all the values that have the corresponding data, location and period.
	 *
	 * @param data the data for which to delete the values for, or null for all data
	 * @param location the location for which to delete the values for, or null for all locations
	 * @param period the period for which to delete the values for, or null all periods
	 */
	@Transactional(readOnly=false)
	public void deleteValues(Data<?> data, CalculationLocation location, Period period) {
		if (log.isDebugEnabled()) log.debug("deleteValues(data="+data+", location="+location+", period="+period+")");
		String valueClass = null;
		if (data != null) valueClass = data.getValueClass().getName();
		else valueClass = "StoredValue";
		String queryString = "delete from "+valueClass+" where 1 = 1";
		if (data != null) queryString += " and data = :data";
		if (location != null) queryString += " and location = :location";
		if (period != null) queryString += " and period = :period";
		Query query = sessionFactory.getCurrentSession()
		.createQuery(queryString);
		if (data != null) query.setParameter("data", data);
		if (location != null) query.setParameter("location", location);
		if (period != null) query.setParameter("period", period);
		query.executeUpdate();
	}
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
}
