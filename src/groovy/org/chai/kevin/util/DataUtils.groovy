/**
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
package org.chai.kevin.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import org.chai.kevin.data.Type;
import org.chai.kevin.data.Type.ValueType;
import org.chai.location.DataLocationType;
import org.chai.kevin.value.Value;
import org.chai.kevin.Period;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.RequestContextUtils;


/**
 * @author Jean Kahigiso M.
 * 
 */
public class DataUtils {
		
	public final static String DEFAULT_TYPE_CODE_DELIMITER = ";";
	
	private final static String DATE_FORMAT = "dd-MM-yyyy";
	
	public static Set<String> split(String string, String delimiter) {
		Set<String> result = new HashSet<String>();
		if (string != null) result.addAll(Arrays.asList(StringUtils.split(string, delimiter)));
		return result;
	}

	public static String unsplit(Object list, String delimiter) {
		List<String> result = new ArrayList<String>();
		
		if (list instanceof String) result.add((String) list);
		if (list instanceof Collection) result.addAll((Collection<String>)list);
		else result.addAll(Arrays.asList((String[]) list));
		
		for (String string : new ArrayList<String>(result)) {
			if (string.isEmpty()) result.remove(string);
		}
		
		return StringUtils.join(result, delimiter);
	}
	
	public static String formatDate(Date date) {
		if (date == null) return null;
		return new SimpleDateFormat(DATE_FORMAT).format(date);
	}
		
	public static Date parseDate(String string) throws ParseException {
		return new SimpleDateFormat(DATE_FORMAT).parse(string);
	}
	
	public static boolean containsId(String string, Long id) {
		return string.matches(".*\\\$"+id+"(\\D|\\z|\\s)(.|\\s)*");
	}
	
	public static Locale getCurrentLocale() {
		return RequestContextUtils.getLocale(RequestContextHolder.currentRequestAttributes().getRequest());
	}
	
	public static String noNull(String possiblyNull) {
		return possiblyNull==null?"":possiblyNull;
	}
	
	public static <E> List<E> removeDuplicates(List<E> list){
		Set<E> set = new LinkedHashSet<E>(list);
		list.clear();
		list.addAll(set);
		return list;
	}

	// TODO move this maybe in Value ?
	public static String getValueString(Type type, Value value){
		if(value != null && !value.isNull()){
			switch (type.getType()) {
			case ValueType.NUMBER:
				return value.getNumberValue().toString();
			case ValueType.BOOL:
				return value.getBooleanValue().toString();
			case ValueType.STRING:
				return value.getStringValue();
			case ValueType.TEXT:
				return value.getStringValue();
			case ValueType.DATE:
				if(value.getDateValue() != null) return DataUtils.formatDate(value.getDateValue());	
				else return value.getStringValue();
			case ValueType.ENUM:
				return value.getStringValue();
			default:
				throw new IllegalArgumentException("get value string can only be called on simple type");
			}			
		}
		return "";
	}
	
}
