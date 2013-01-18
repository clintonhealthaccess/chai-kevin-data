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

There are 2 types of calculations, **sums** and **modes**. Sums simply take 

values stored by data location type too 


Data types
---




Class hierarchy
---



JAQL
---



Refresh and stuff
---



[BSD 3-clause License]: http://www.w3.org/Consortium/Legal/2008/03-bsd-license.html
[CHAI]: http://www.clintonhealthaccess.org
[grails-chai-locations]: http://github.com/fterrier/grails-chai-locations
[DHSST]: http://www.dhsst.org
[JAQL]: http://code.google.com/p/jaql/