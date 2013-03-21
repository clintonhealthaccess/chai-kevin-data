package org.chai.kevin.data;

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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import org.chai.kevin.data.Calculation;
import org.chai.kevin.data.Enum;
import org.chai.kevin.data.Data;
import org.chai.kevin.data.RawDataElement;
import org.chai.kevin.data.EnumOption;
import org.chai.kevin.data.NormalizedDataElement;
import org.chai.kevin.util.DataUtils;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.ObjectNotFoundException;
import org.chai.kevin.util.DataUtils;

class DataService {

	static transactional = true
	
	def valueService;
	def sessionFactory;
	
	/**
	 * Returns the enum with the given id or null if no such enum exists.
	 * 
	 * @param id the id 
	 * @return the enum with the given id
	 */
	public Enum getEnum(Long id) {
		return (Enum)sessionFactory.getCurrentSession().get(Enum.class, id);
	}
	
	/**
	 * Returns the enum with the give code or null if no such enum exists.
	 * 
	 * @param code the code
	 * @return the enum with the given code
	 */
	public Enum findEnumByCode(String code) {
		return Enum.findByCode(code, [cache: true])
	}
	
	/**
	 * Gets the data for the given id and the given class. If no such object exists, returns null.
	 * If a data exists with the given id, but is not of a compatible class, returns null.
	 * 
	 * @param id the id
	 * @param clazz the class 
	 * @return the data or null if none is found
	 */
	public <T extends Data<?>> T getData(Long id, Class<T> clazz) {
		if (id == null) return null;
		try {
			return (Data)sessionFactory.getCurrentSession().get(clazz, id);	
		} catch (ObjectNotFoundException e) {
			if (log.isWarnEnabled()) log.warn("object not found with id: "+placeholder+" and class: "+clazz, e);
		}
	}
	
	/**
	 * Gets the data for the given code and class. If no such object exists, returns null.
	 * If a data exists with the given id, but is not of a compatible class, returns null.
	 *
	 * @param code the code
	 * @param clazz the class
	 * @return the data or null if none is found
	 */
	public <T extends Data<?>> T getDataByCode(String code, Class<T> clazz) {		
		return (T) sessionFactory.getCurrentSession().createCriteria(clazz)
		.add(Restrictions.eq("code", code)).uniqueResult();
	}
	
	/**
	 * Saves the data, bypassing grails validation.
	 *
	 * @param data the data to save
	 * @return the saved data
	 */
	public <T extends Data<?>> T save(T data) {
		// we bypass validation in case there's something
		// it should be saved anyway
		data.save(validate: false, flush: true)
	}
	
	/**
	 * Deletes the given data. If that data still has value associated with it
	 * or if it is referenced by a normalized data element or a calculation, throws an 
	 * @IllegalArgumentException.
	 * 
	 * @throws IllegalArgumentException if the data element has values associated to it
	 * @param data the data to delete
	 */
	public void delete(Data data) {
		if (!getReferencingData(data).isEmpty()) throw new IllegalArgumentException("there are still data referencing the element being deleted")
		if (valueService.getNumberOfValues(data) != 0) throw new IllegalArgumentException("there are still values associated to the element being deleted");
		else data.delete();
	}
	
	/**
	 * Returns all the data that directly reference the given data. For example, if
	 * calculation $3 is $1 + $2, then calling this method with $1 as a parameter would return
	 * a set containing calculation $3. Note that this is not transitive and will only return
	 * the data that directly references the given data.
	 *
	 * Calling this is the same as getting the union of getReferencingNormalizedDataElement and getReferencingCalculation.
	 * 
	 * @param data the data to get the referencing data for
	 * @return a set of the referencing data
	 */
	public Set<Data<?>> getReferencingData(Data data) {
		def result = []
		result.addAll(getReferencingNormalizedDataElements(data))
		result.addAll(getReferencingCalculations(data))
		return result
	}
	
	/**
	 * Returns all the normalized data elements that directly reference the given data. For example, if
	 * normalized data element $3 is $1 + $2, then calling this method with $1 as a parameter would return
	 * a set containing calculation $3. Note that this is not transitive and will only return
	 * the data that directly references the given data.
	 *
	 * @param data the data to get the referencing data for
	 * @return a set of the referencing normalized data element
	 */
	public List<NormalizedDataElement> getReferencingNormalizedDataElements(Data data) {
		def criteria = sessionFactory.currentSession.createCriteria(NormalizedDataElement.class);
		def list = criteria.add(Restrictions.like("expressionMapString", "\$"+data.id, MatchMode.ANYWHERE)).list()
		return list.findAll { result ->
			return !result.expressions.findAll { expression ->
				return DataUtils.containsId(expression, data.id)
			}.isEmpty()
		}
	}
	
	/**
	 * Returns all the calculations that directly reference the given data. For example, if
	 * calculation $3 is $1 + $2, then calling this method with $1 as a parameter would return
	 * a set containing calculation $3. Note that this is not transitive and will only return
	 * the data that directly references the given data.
	 *
	 * @param data the data to get the referencing data for
	 * @return a set of the referencing normalized data element
	 */
	public List<Calculation> getReferencingCalculations(Data data) {
		def criteria = sessionFactory.currentSession.createCriteria(Calculation.class);
		def list = criteria.add(Restrictions.like("expression", "\$"+data.id, MatchMode.ANYWHERE)).list()
		return list.findAll { result ->
			return DataUtils.containsId(result.expression, data.id)
		}
	}
	
	/**
	 * Searches the data with the given text. Searches the code, name, info and type fields. Restricts the search
	 * to the given allowed types and class.
	 *
	 * @param clazz the class for which to search data
	 * @param text the string the search for
	 * @param allowedTypes types for which the search will be done
	 * @param params the map with sort, order, offset and max information for the search
	 * @return the list of data that matches
	 */
	public <T extends Data> List<T> searchData(Class<T> clazz, String text, List<String> allowedTypes, Map<String, String> params) {
		def dbFieldName = 'names_' + DataUtils.getCurrentLocale().getLanguage();
		def criteria = clazz.createCriteria()
		def data = criteria.list(offset:params.offset, max:params.max, sort:params.sort ?:"id", order: params.order ?:"asc"){
			StringUtils.split(text).each { chunk ->
				 or{
					 ilike("code","%"+chunk+"%")
					 ilike(dbFieldName,"%"+chunk+"%")
					 if (NumberUtils.isNumber(chunk)) {
						 eq("id", Long.parseLong(chunk))
					 }
					 if (clazz.equals(RawDataElement.class)) {
						 ilike("info", "%"+chunk+"%")
					 }
				 }
			}
			if (!allowedTypes.isEmpty()) {
				allowedTypes.each { type ->
					ilike("typeString", "%"+type+"%")
				}
			}
		}
		
		if (!allowedTypes.isEmpty()) {
			data.retainAll { element ->
				element.getType().type.name().toLowerCase() in allowedTypes 
			}
		}
		
		return data
    }
	
}
