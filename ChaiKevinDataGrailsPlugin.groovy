import org.chai.kevin.ExpressionService
import org.chai.kevin.JaqlService
import org.chai.kevin.RefreshValueService
import org.chai.kevin.JaqlService
import org.chai.location.LocationService;
import org.chai.kevin.data.DataService;
import org.chai.kevin.exports.CalculationExportService;
import org.chai.kevin.exports.DataElementExportService;
import org.chai.kevin.value.ExpressionService
import org.chai.kevin.value.RefreshValueService
import org.chai.kevin.value.ValueService
import org.hibernate.SessionFactory;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean

class ChaiKevinDataGrailsPlugin {
    
    def version = "0.1.5-CHAI"
    def grailsVersion = "2.1 > *"
    def dependsOn = [:]
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Chai Kevin Data Plugin" // Headline display name of the plugin
    def author = "François Terrier"
    def authorEmail = "fterrier@clintonhealthaccess.org"
    def description = '''\
Data plugin for the Kevin project
'''

    // URL to the plugin's documentation
    def documentation = "http://github.com/fterrier/grails-chai-kevin-data"
	def license = "BSD3"
	def organization = [ name: "Clinton Health Access Initiative", url: "http://www.clintonhealthaccess.org" ]
	def developers = [ 
		[ name: "Jean Kahigiso", email: "jkahigiso@clintonhealthaccess.org" ],
		[ name: "Sue Lister", email: "slister@clintonhealthaccess.org" ]
	]

	def issueManagement = [ system: "JIRA", url: "http://github.com/fterrier/grails-chai-kevin-data/issues" ]
	def scm = [ url: "http://github.com/fterrier/grails-chai-kevin-data" ]

	def doWithSpring = {
		
		jaqlService(JaqlService) { bean ->
			bean.singleton = true
		}

		refreshValueService(RefreshValueService) {
			expressionService = ref("expressionService")
			valueService = ref("valueService")
			locationService = ref("locationService")
			sessionFactory = ref("sessionFactory")
			dataService = ref("dataService")
			periodService = ref("periodService")
			transactionManager = ref("transactionManager")
		}

		valueService(ValueService) {
			sessionFactory = ref("sessionFactory")
		}

		expressionService(ExpressionService) {
			dataService = ref("dataService")
			locationService = ref("locationService")
			valueService = ref("valueService")
			periodService = ref("periodService")
			jaqlService = ref("jaqlService")
			sessionFactory = ref("sessionFactory")
		}

		dataElementExportService(DataElementExportService){
			locationService = ref("locationService")
			valueService = ref("valueService")
			sessionFactory = ref("sessionFactory")
		}

		calculationExportService(CalculationExportService){
			locationService = ref("locationService")
			valueService = ref("valueService")
			sessionFactory = ref("sessionFactory")
		}

		// override the spring cache manager to use the same as hibernate
		springcacheCacheManager(EhCacheManagerFactoryBean) {
			shared = true
			cacheManagerName = "Springcache Plugin Cache Manager"
		}
		
	}

}
