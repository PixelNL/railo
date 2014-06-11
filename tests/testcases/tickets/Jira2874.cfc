<cfcomponent output="false" extends="org.railo.cfml.test.RailoTestCase">
	
	<cffunction name="testWhitespaceAfterArgs" output="false">
		<cfset x = () ->5+1 />
		<cfset assertEquals(expected:6, actual: x()) />
	</cffunction>

	<cffunction name="testNoWhitespaceAfterArgs" output="false">
		<cfset x = ()->5+1 />
		<cfset assertEquals(expected:6, actual: x()) />
	</cffunction>

	<cffunction name="testArgsWork" output="false" >
		<cfset x = (x,y)->x + y />
		<cfset assertEquals(expected:6, actual: x(1,5)) />
	</cffunction>
		
	<cffunction name="testWhitespaceAfterLambdaOperator">
		<cfset x = (x)-> x * x />
		<cfset assertEquals(expected:4, actual: x(2)) />
	</cffunction>
	
	<cffunction name="testCFScript">
		<cfscript>
			local.x = (x)->x*x;
			assertEquals(expected:9, actual: local.x(3));
		</cfscript>
	</cffunction>

	<cffunction name="testEnclosedVars">
		<cfset local.y = 5 />
		<cfset local.lambda = (x) -> x+y />
		<cfset assertEquals(expected:7, actual:local.lambda(2)) />
	</cffunction>	

	<cffunction name="otherFunction" test="false" access="private">
		<cfargument name="x">
		<cfreturn 5+x />
	</cffunction>

	<cffunction name="testCallAnotherFunction">
		<cfset local.lambda = (x) -> otherFunction(x) />
		<cfset assertEquals(expected:8, actual:local.lambda(3)) />
	</cffunction>	
	
	<cffunction name="testLambdaBegetsLambda">
		<cfset local.ll = (x) -> (y,z) -> x+y+z  />
		<cfset local.lambda = local.ll(5) />
		<cfset assertEquals(expected:10, actual:local.lambda(3,2)) />
	</cffunction>

	<cffunction name="testScopes">
		<cfset local.ll = createLambda()/>
		<cfdump var="#1#">
		<cfdump var="ee">
		<cfdump var="#ll#">
		<cfdump var="#function (){}#">
		<cfdump var="#createLambda#">
		<cfset x=getBuiltInFunction('len')>
		<cfdump var="#x#">
		<cfdump var="#x("Susi")#">
		<cfset assertEquals(expected:'null', actual:ll()) />
	</cffunction> 

	<cffunction name="createLambda" access="private" hint="hello hint">
		<cfset local.a="a">
		<cfset local.ll = () -> isNull(a)?'null':a  />
		<cfreturn ll>
	</cffunction>
 
	<!--- <cffunction name="testBlock" access="private">
		<cfscript>
			local.lf=(numeric n) -> {
				local.rtn=n*n;
				return rtn+1;
			};
			assertEquals(expected:1, actual:lf(2));
		</cfscript>


		<cfset assertEquals(expected:10, actual:local.lambda(3,2)) />
	</cffunction>--->




</cfcomponent>