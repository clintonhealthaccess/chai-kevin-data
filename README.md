CHAI Data Plugin for DHSST
===

This plugin is used by [Clinton Health Access Initiative][CHAI] at the Rwanda Ministry of Health as part of the [District Health Systems Strengthening Tool][DHSST] project. 

The data plugin allows storage, normalization and aggregation of data collected for example using a survey, or imported using the import/export functionality. It depends on the [CHAI location][grails-chai-locations] plugin to store values.

This plugin could potentially be used to store and aggregate any data from other potential application domains, although it is now used to aggregate Health Indicator data for the Rwanda Ministry of Health. The normalization and aggregation engine uses the [JAQL][JAQL] query language now part of Apache Hadoop (cf. JAQL TODO) and the data types stored can be either single values (bool, integer, string, …), lists or maps (cf. Data types TODO).

The data abstraction layer
---

#### Raw data elements

The data description and metadata is stored in an persisted object called a raw data element. The data plugin allows to persist values associated to those raw data elements. There can be one value stored for *each data element-data location-period* tuple, where *data-element* is the associated data element, *location* is the corresponding location from the [CHAI locations][grails-chai-locations] plugin, and *period* is the corresponding time period during which the value was collected or created.

For example if you want to collect the *number of nurses* for specific data locations and periods, you'll create a *number of nurses* raw data element and associate the values to it.

#### Normalized data elements

Collected data values are attached to **raw data elements**. Raw data elements can be normalized across period and data location type using a **normalized data element**. This is particularly useful if the data collected consistently across periods. The normalization formula is given using the JAQL language.

For example, if you collect a *number of nurses* data element for period 2007 and a *number of nurses A1* and *number of nurses A2* for 2008 (because you decide that suddenly you need more granularity in the collected data), you can create a *total number of nurses* normalized data element that would give, for 2007, the content of the *number of nurses* data element and for 2008, the content of *number of nurses A1* + *number of nurses A2*.

#### Calculations

Calculations are used to aggregate data along the location tree structure given by the [CHAI locations][grails-chai-locations] plugin. Say you want to know the number of nurses for a given location level (for example a district), then you need to create a *Sum* calculation that will aggregate the *number of nurses* data element. Calculations are defined using the JAQL language as well.

There are 2 types of calculations, **sums**, **modes** and **aggregations**. Sums simply take a number value and sums it up the location hierarchy, with the option of getting the average as well. Modes count the number of times a certain value occurs inside a certain location, giving the option of retrieving the whole map (will be a map of value-number of times) or the biggest mode.

TODO values stored by data location type too 

Data types
---

Types define what kind of data is stored in a data element. There are simple and complex types, which allow to store almost any type of data. The types can be retrieved and manipulated as an instance of the ```Type``` class. The types are defined using a JSON notation that's also described below:

The following simple types are available:

* number ```{type: number}```
* string ```{type: string}```
* text (a long string) ```{type: text}```
* bool ```{type: bool}```
* date ```{type: date}```
* enum ```{type: enum, code: "<enum_code>"}```, where ***<enum_code>*** is the code of the corresponding enum.

The following complex types are available:

* map ```{type: map, elements: <element_list>}```, where ***<element_list>*** is ```[{name: <element_name>, element_type: <type>}, …]```, where ***<element_name>*** is an arbitrary name to refer to the element inside the map, and ```<type>``` is any data type.
* list ```{type: list, list_type: <type>}```

So you can combine the complex types with simple types to create complex list and maps. For example, to record information about the staff at certain data locations, you can create the following type:

	- list (type: map)
		- first_name: string
		- last_name: string
		- birthday: date

The JSON notation for that type would be

	{type: list, list_type: {type: map, elements: [{name: "first_name", element_type: {type: string}}, {name: "last_name", element_type: {type: string}}, {name: "birthday", element_type: {type: date}}]}}

When a type is of complex type, each nested type can be accessed using an *address* string. There are 2 sorts of addresses (also called *prefix* or *suffix* in this document and in the code, apologies for the confusion), generic addresses and specific addresses. For example, a generic address for the ```first_name``` field above would be ```[_].first_name``` whereas a specific would be ```[2].first_name``` (it would specifically refer to the third row of the list). Addresses are built by adding ```[_]``` for lists and ```.<element_key>``` for the maps.

Let's take another example of type with nested maps :

	- list (type: map)
		- personal_information: map
			- first_name: string
			- last_name: string
			- birthday: date
		- work_history: map
			- employer
			- function
			
We would have the following generic addresses :

	element					address
	-------					-------
	list		
	elements of list		[_]
	personal information	[_].personal_information
	first name				[_].personal_information.first_name
	last name				[_].personal_information.last_name
	birthday				[_].personal_information.birthday
	work history			[_].work_history
	employer				[_].work_history.employer
	function				[_].work_history.function

The ```Type``` class also offers a bunch of methods to manipulate, transforma Value and Type objects. See the javadoc for more details.

Values
---

To store values, we use instances of the **Value** class. It works similarly to the Type class as it holds the value in a JSON format. The JSON format to store a value is the following for the given types:

	type			value						remarks
	----			-----						-------
	number			{"value": <number>}
	string			{"value": <string>}
	text			{"value": <text>}
	enum			{"value": <enum_value>}
	date			{"value": <date>}			format: dd-MM-yyyy
	bool			{"value": <bool>}			format: true or false
	
	list			{"value": [<value_1>, <value_2>, ...]}
	map				{"value": [{map_key:<key_1>, map_value:<value_for_key_1>}, {map_key: <key_2>, map_value:<value_for_key_2>}, …]}

A value object is always paired with its corresponding type object, and the Type class contains several methods that help manipulate the value itself (cf. javadoc). The type is not saved with the value though, but taken externally. For example if one is manipulating a data element of type ```number```, it can be assumed that all values retrieved for this data element will be of hold ```number``` values.

#### Null values

A value can be ```null```, for which the JSON representation is ```{"value": null}```. To test if a value is null, use the ```isNull()``` method on the Value class. Null values are handled specially by the JAQL language explained below.


TODO json attirbutes

JAQL
---

To create **normalized data elements** and **calculations**, you need to use an expression language based on [JAQL][JAQL]. That language allows one to create the most complex expression and to manipulate data stored in lists and maps. To use a data element value inside an expression, refer to it using the ```$<data_element_id>``` notation.

Beside simple arithmetic expressions, JAQL allows for example to count the number of elements in a list. Take the previous example, and say there is a raw data element with id ```1``` and the type given in the previous chapter. To count the number of employees, use the following JAQL expression :

	$1 -> count()
	
You will have to store it in a normalized data element of type ```{type: number}```. Or for example, if you want to filter based on a certain last name:

	$1 -> filter ($.last_name == "Smith")

TODO explain null vs. isnull

Class hierarchy & APIs
---

#### Class structures

The Data classes are structured as follows:

	- Data.groovy
		- DataElement.groovy
			- NormalizedDataElement.groovy
			- RawDataElement.groovy
		- Calculation.groovy
			- Summ.groovy
			- Mode.groovy
			- Aggregation.groovy

Data classes have corresponding value classes which describe how the values are stored in the database.

##### Values for data elements

Classes which inherit the ```DataElement``` class (**raw data element** and **normalized data element**) each have one associated value for each data location and period. They are instances of the ```RawDataElementValue``` class and ```NormalizedDataElementValue``` class respectively.

TODO explain value does not exist vs isNull
- for raw data element
- for normalized data element

##### Values for calculations

TODO explain no value vs isNull
TODO explain when values are skipped

Classes which inherit the ```Calculation``` class each have one associated value for each location, period and data location type. This allows one to look at calculations at a higher level and filter by data location type. Those values are called **partial values** are instances of the ```CalculationPartialValue``` class (this class is abstract and each calculation has its corresponding concrete implementation of that class).

Partial values are then combined together to give a value for a certain group of data location types into instances of the class ```CalculationValue```. This combining is done on the fly and the instances of that class are not persisted in the database. Similarly to the partial value class, the ```CalculationValue``` class is abstract and each calculation has its corresponding concrete implementation.

The Value classes are structured as follows:

	- DataValue.groovy (interface)
	
		- StoredValue.groovy (persisted)
			- RawDataElementValue.groovy
			- NormalizedDataElementValue.groovy
			- CalculationPartialValue.groovy
				- SummPartialValue.groovy
				- AggregationPartialValue.groovy
				- ModePartialValue.groovy
				
		- CalculationValue.groovy (not persisted)
			SummValue.groovy
			AggregationValue.groovy
			ModeValue.groovy

There are a few API that allow manipulation of data and values, they are the ```DataService``` that contains methods to retrieve data (data elements and calculations), ```ExpressionService``` that contains methods to calculate **normalized data elements** and **calculations**, the ```ValueService``` that contains methods to retrieve values from the database and the ```RefreshValueService``` that contains method to refresh some or all of the **normalized data elements** and **calculations** using the ```ValueService``` and ```ExpressionService```. 

The main public methods in those services are documented below.

##### DataService

The data service deals with data and allows you to query for data elements or calculations in the system.

	/* for Enum */
	public Enum getEnum(Long id);
	
	public Enum findEnumByCode(String code);
	
	/* for Data */
	public <T extends Data<?>> T getData(Long id, Class<T> clazz);
	
	public <T extends Data<?>> T getDataByCode(String code, Class<T> clazz);
	
	public <T extends Data<?>> T save(T data);
	
	public void delete(Data data);
	
	public Set<Data<?>> getReferencingData(Data data);
	
	public List<NormalizedDataElement> getReferencingNormalizedDataElements(Data data);
	
	public List<Calculation> getReferencingCalculations(Data data);
	
	public <T extends Data> List<T> searchData(Class<T> clazz, String text, List<String> allowedTypes, Map<String, String> params);


##### ExpressionService

The expression service deals with calculating the values for given normalized data elements and calculations. It never stores those values in the database, the caller of those methods has to take care of storing the returned values.

	/* for NormalizedDataElement */
	public NormalizedDataElementValue calculateValue(NormalizedDataElement normalizedDataElement, DataLocation dataLocation, Period period);

	/* for Calculation */
	public <T extends CalculationPartialValue> List<T> calculatePartialValues(Calculation<T> calculation, CalculationLocation location, Period period);

##### ValueService

The value service just deals with retrieving, saving and searching for values in the database. It never triggers any calculation of some values are missing.

	/* for DataElement */
	public <T extends DataValue> T getDataElementValue(DataElement<T> data, DataLocation dataLocation, Period period);
	
	public <T extends DataValue> List<T> searchDataValues(String text, DataElement<T> data, DataLocation dataLocation, Period period, Map<String, Object> params);

	/* for Calculation */
	getCalculationValue(Calculation<T> calculation, CalculationLocation location, Period period, Set<DataLocationType> types)

	/* for any Data */
	public <T extends StoredValue> T save(T value);
	
	public void deleteValues(Data<?> data, CalculationLocation location, Period period);
	
	public <T extends DataValue> List<T> listDataValues(Data<T> data, DataLocation dataLocation, Period period, Map<String, Object> params);

	public <T extends DataValue> Long countDataValues(String text, Data<T> data, DataLocation dataLocation, Period period)

##### RefreshValueService

The ```RefreshValueService``` will also refresh the dependencies when some other data elements are used in an expression. It will build a dependency tree and refresh them in the correct order. The ```ExpressionService``` will just calculate the current data element or calculation regardless of whether the dependencies have already been calculated or not. 

For example, here is how it would work for the following normalized data elements :

	normalized data element $3: $1 + $2
	normalized data element $2: $10 / $20
	calculation $30: $3

Refreshing the calculation ```$30``` would build the following dependency tree:

			$30
			 |
			 $3
			/  \
		   $1  $2
		      /  \
		     $10  $20

It would therefore refresh the data in the order ```$10 -> $20 -> $2 -> $1 -> $3 -> $30```. Below are the public methods:

	/* for NormalizedDataElement */
	public List<NormalizedDataElement> refreshNormalizedDataElement(NormalizedDataElement normalizedDataElement, Progress progress);
	
	public void refreshNormalizedDataElement(NormalizedDataElement dataElement, DataLocation dataLocation, Period period);
	
	/* for Calculation */
	public void refreshCalculation(Calculation<?> calculation, Progress progress);
	
	/* for all */
	public void refreshAll(final Progress progress);


[BSD 3-clause License]: http://www.w3.org/Consortium/Legal/2008/03-bsd-license.html
[CHAI]: http://www.clintonhealthaccess.org
[grails-chai-locations]: http://github.com/fterrier/grails-chai-locations
[DHSST]: http://www.dhsst.org
[JAQL]: http://code.google.com/p/jaql/