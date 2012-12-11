package org.chai.kevin;

import java.text.ParseException;

import grails.plugin.spock.UnitSpec

import org.chai.kevin.data.Type;
import org.chai.kevin.util.DataUtils;
import org.chai.kevin.value.Value;

public class UtilUnitSpec extends UnitSpec {
	
	def "test contains id"() {
		expect:
		DataUtils.containsId("\$123", 123)
		!DataUtils.containsId("\$1234", 123)
		DataUtils.containsId("\$1 + \$2", 1)
		DataUtils.containsId("\$1 + \$2", 2)
		DataUtils.containsId("\$1+\$2", 1)
		DataUtils.containsId("\$1+\$2", 2)
		!DataUtils.containsId("1+2", 2)
		!DataUtils.containsId("1+2", 2)
		
		DataUtils.containsId("\$10218[_].test\n", 10218)
		DataUtils.containsId("\$10218\n", 10218)
		DataUtils.containsId(
			"(\$10218[_].basic.monthly_targets.july + \n" +
			"\$10218[_].basic.monthly_targets.august + \n" +
			"\$10218[_].basic.monthly_targets.september + \n" +
			"\$10218[_].basic.monthly_targets.october + \n" +
			"\$10218[_].basic.monthly_targets.november + \n" +
			"\$10218[_].basic.monthly_targets.december + \n" +
			"\$10218[_].basic.monthly_targets.january + \n" +
			"\$10218[_].basic.monthly_targets.february + \n" +
			"\$10218[_].basic.monthly_targets.march + \n" +
			"\$10218[_].basic.monthly_targets.april+ \n" +
			"\$10218[_].basic.monthly_targets.may + \n" +
			"\$10218[_].basic.monthly_targets.june) == \n" +
			"\$10218[_].basic.targets.target_number_of_cases", 10218)
	}
	
	def "test parse date"() {
		when:
		DataUtils.parseDate("12-rr-2011")
		
		then:
		thrown ParseException
		
	}
	
	def "test getStringValue"(){
		setup:
		boolean boolValue= true;
		def nowDate = new Date();
		
		def typeString = Type.TYPE_STRING();
		def typeDate = Type.TYPE_DATE();
		def typeNumber = Type.TYPE_NUMBER();
		def typeBool = Type.TYPE_BOOL();
		def typeEnum = Type.TYPE_ENUM();
	
		def valueBool = Value.VALUE_BOOL(boolValue);
		def valueString = Value.VALUE_STRING("Value Text");
		def valueNumber = Value.VALUE_NUMBER(100);
		def valueDate = Value.VALUE_DATE(nowDate);
		
		when:
		def string = DataUtils.getValueString(typeString,valueString);
		def number = DataUtils.getValueString(typeString,valueNumber);
		def date = DataUtils.getValueString(typeString,valueDate);
		def bool = DataUtils.getValueString(typeString,valueBool);
		def enumValue= DataUtils.getValueString(typeEnum,valueString);
		
		then:
		string.equals("Value Text");
		number.equals("100");
		bool.equals(boolValue.toString());
		enumValue.equals("Value Text")
		date.equals(DataUtils.formatDate(nowDate));	
			
	}

	def "test format export code"(){
		when:
		def normalString = DataUtils.formatExportCode("blah")
		def emptyString = DataUtils.formatExportCode("")
		def nullString = DataUtils.formatExportCode(null)
		
		then:
		normalString == "~blah~"
		emptyString == "~~"
		nullString == "~null~"
	}
	
}
