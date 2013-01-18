package org.chai.kevin;

import java.text.ParseException;

import grails.plugin.spock.UnitSpec

import org.chai.kevin.data.Type;
import org.chai.kevin.util.DataUtils;
import org.chai.kevin.value.Value;

public class DataUtilUnitSpec extends UnitSpec {
	
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
			
		!DataUtils.containsId("count(\$)", 1)
		!DataUtils.containsId("\$.work_history.primary_fop_function == true", 1)
		DataUtils.containsId('count_by_function = if (\$3983 == "null") [] else (\$3983 -> group by function = \$.work_history.primary_fop_function into {function, head_count : count(\$)});\n\ncount_by_function;', 3983);
		
		DataUtils.containsId('doctor_count =((\$5829 -> filter \$.function=="doctor")[*].head_count -> sum());\n'+
		'nurse_maternity_midwife_count =((\$5829 -> filter \$.function=="nurse_maternity_midwife")[*].head_count -> sum());\n'+
		'nurse_a1_count =((\$5829 -> filter \$.function=="nurse_a1")[*].head_count -> sum());\n'+
		'nurse_a2_count =((\$5829 -> filter \$.function=="nurse_a2")[*].head_count -> sum());\n'+
		'nutritionist_count =((\$5829 -> filter \$.function=="nutritionist")[*].head_count -> sum());\n'+
		'laboratory_technician_count =((\$5829 -> filter \$.function=="laboratory_technician")[*].head_count -> sum());\n'+
		'social_worker_count =((\$5829 -> filter \$.function=="social_worker")[*].head_count -> sum());\n'+
		'max_working_minutes_any_staff= 104640;\n'+
		'total_service_minutes_per_doctor = \$4411 -> transform each service ( ( (\$5913-> filter \$.name== service.basic.service.service_type)[*].doctor_minute->sum()) *service.basic.targets.target_number_of_cases)->sum();\n'+
		'ideal_number_of_doctor= roundup( (total_service_minutes_per_doctor) / (max_working_minutes_any_staff * \$5852));\n'+
		'total_service_minutes_per_nurse_A1 = \$4411 -> transform each service ( (((\$5916-> filter \$.name==service.basic.service.service_type)[*].nurse_a1_minute->sum())*service.basic.targets.target_number_of_cases))->sum();\n'+
		'', 5829)
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
	
}
