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

import groovy.transform.EqualsAndHashCode;

import java.util.ArrayList
import java.util.List

import org.chai.kevin.util.DataUtils

@i18nfields.I18nFields
@EqualsAndHashCode(includes='code')
class Enum {

	// deprecated
	Long id
	
	String code
	String names
	String descriptions

	static hasMany = [enumOptions: EnumOption]
	
	static i18nFields = ['names', 'descriptions']
	
	static mapping = {
		table 'dhsst_enum'
		enumOptions lazy: false, cache: true
		code unique: true
		cache true
	}
	
	static constraints =  {
		code (nullable: false, blank: false, unique: true)
		names (nullable: true)
		descriptions (nullable: true)
	}
	
	public List<EnumOption> getActiveEnumOptions() {
		List<EnumOption> result = new ArrayList<EnumOption>();
		for (EnumOption enumOption : enumOptions) {
			if (enumOption.getInactive() == null || !enumOption.getInactive()) result.add(enumOption);
		}
		return result;
	}

	public EnumOption getOptionForValue(String value) {
		for (EnumOption enumOption : enumOptions) {
			if (enumOption.getValue().equals(value)) return enumOption;
		}
		return null;
	}
	
	public List<EnumOption> getAllEnumOptions() {
		return new ArrayList(enumOptions?:[])
	}

	public boolean hasValue(String value) {
		for (EnumOption enumOption : enumOptions) {
			if (enumOption.getValue().equals(value)) return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "Enum[getId()=" + getId() + ", getCode()=" + getCode() + "]";
	}
	
}