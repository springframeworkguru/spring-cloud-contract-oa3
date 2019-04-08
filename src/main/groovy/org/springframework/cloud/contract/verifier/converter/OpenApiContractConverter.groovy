package org.springframework.cloud.contract.verifier.converter

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import groovy.util.logging.Slf4j
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.parser.OpenAPIV3Parser
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.contract.spec.Contract
import org.springframework.cloud.contract.spec.ContractConverter
import org.springframework.context.ApplicationContext


/**
 * Created by John Thompson on 5/24/18.
 */
@Slf4j
class OpenApiContractConverter implements ContractConverter<Collection<PathItem>> {

    public static final OpenApiContractConverter INSTANCE = new OpenApiContractConverter()
    public static final String SERVICE_NAME_KEY = "scc.enabled.servicenames"

    private YamlToContracts yamlToContracts = new YamlToContracts()

    @Autowired
    private ApplicationContext appContext

    @Override
    boolean isAccepted(File file) {

        try {
            def spec = new OpenAPIV3Parser().read(file.path)

            if (spec == null) {
                log.debug("Spec Not Found")
                throw new RuntimeException("Spec not found")
            }

            if (spec.paths.size() == 0) { // could toss NPE, which is also invalid spec
                log.debug("No Paths Found")
                throw new RuntimeException("No paths found")
            }

            def contractsFound = false
            //check spec for contracts
            spec.paths.each { k, v ->
                if (!contractsFound) {
                    v.readOperations().each { operation ->
                        if (operation.extensions) {
                            def contracts = operation.extensions."x-contracts"
                            if (contracts != null && contracts.size > 0) {
                                contractsFound = true
                            }
                        }
                    }
                }
            }

            return contractsFound
        } catch (Exception e) {
            log.error("Unexpected error in reading contract file")
            log.error(e.message)
            return false
        }
    }

    @Override
    Collection<Contract> convertFrom(File file) {
        Collection<Contract>  sccContracts = []

        def spec = new OpenAPIV3Parser().read(file.path)

        spec?.paths?.each { path, pathItem ->
            pathItem.readOperations().each { operation ->
                if (operation?.extensions?."x-contracts") {
                    operation.extensions."x-contracts".each { openApiContract ->

                        if(checkServiceEnabled(openApiContract.serviceName)) {

                            YamlContract yamlContract = new YamlContract()
                            yamlContract.name = openApiContract?.name
                            yamlContract.description = openApiContract?.description
                            yamlContract.priority = openApiContract?.priority
                            yamlContract.label = openApiContract?.label

                            def contractId = openApiContract.contractId

                            if (openApiContract.ignored != null) {
                                yamlContract.ignored = openApiContract.ignored
                            } else {
                                yamlContract.ignored = false
                            }

                            def contractPath = (StringUtils.isEmpty(openApiContract.contractPath)) ? path : openApiContract.contractPath

                            yamlContract.request = new YamlContract.Request()
                            yamlContract.request.url = contractPath

                            if (pathItem?.get?.is(operation)) {
                                yamlContract.request.method = "GET"
                            } else if (pathItem?.put?.is(operation)) {
                                yamlContract.request?.method = "PUT"
                            } else if (pathItem?.post?.is(operation)) {
                                yamlContract.request.method = "POST"
                            } else if (pathItem?.delete?.is(operation)) {
                                yamlContract.request?.method = "DELETE"
                            } else if (pathItem?.patch?.is(operation)) {
                                yamlContract?.request?.method = "PATCH"
                            }

                            if (openApiContract?.request?.queryParameters) {
                                openApiContract?.request?.queryParameters.each { queryParameter ->
                                    yamlContract.request.queryParameters.put(queryParameter.key, queryParameter.value)

                                    if (queryParameter.matchers) {
                                        queryParameter?.matchers?.each { contractMatcher ->
                                            YamlContract.QueryParameterMatcher queryParameterMatcher = new YamlContract.QueryParameterMatcher()
                                            queryParameterMatcher.key = queryParameter.name
                                            queryParameterMatcher.value = contractMatcher.value
                                            queryParameterMatcher.type = getMatchingTypeFromString(contractMatcher.type)
                                            yamlContract.request.matchers.queryParameters.add(queryParameterMatcher)
                                        }
                                    }
                                }
                            }

                            operation?.parameters?.each { openApiParam ->
                                openApiParam?.extensions?."x-contracts".each { contractParam ->
                                    if (contractParam.contractId == contractId) {
                                        if (openApiParam.in == 'path' || openApiParam.in == 'query') {
                                            yamlContract.request.queryParameters.put(openApiParam.name, contractParam.value)

                                            if (contractParam.matchers) {
                                                contractParam?.matchers?.each { contractMatcher ->
                                                    YamlContract.QueryParameterMatcher queryParameterMatcher = new YamlContract.QueryParameterMatcher()
                                                    queryParameterMatcher.key = openApiParam.name
                                                    queryParameterMatcher.value = contractMatcher.value
                                                    queryParameterMatcher.type = getMatchingTypeFromString(contractMatcher.type)
                                                    yamlContract.request.matchers.queryParameters.add(queryParameterMatcher)
                                                }
                                            }
                                        }
                                        if (openApiParam.in == 'header') {
                                            yamlContract.request.headers.put(openApiParam.name, contractParam.value)

                                            if (contractParam.matchers) {
                                                contractParam?.matchers?.each { headerMatcher ->
                                                    YamlContract.HeadersMatcher headersMatcher = new YamlContract.HeadersMatcher()
                                                    headersMatcher.key = openApiParam.name
                                                    headersMatcher.regex = headerMatcher.regex
                                                    headersMatcher.predefined = getPredefinedRegexFromString(headerMatcher.predefined)
                                                    headersMatcher.command = headerMatcher.command
                                                    headersMatcher.regexType = getRegexTypeFromString(headerMatcher.regexType)
                                                    yamlContract.request.matchers.headers.add(headerMatcher)
                                                }
                                            }
                                        }

                                        if (openApiParam.in == 'cookie') {
                                            yamlContract.request.cookies.put(openApiParam.name, contractParam.value)
                                        }
                                    }
                                }
                            }

                            if (operation?.requestBody?.extensions?."x-contracts") {
                                operation?.requestBody?.extensions?."x-contracts"?.each { contractBody ->
                                    if (contractBody?.contractId == contractId) {

                                        contractBody?.headers?.each { k, v ->
                                            yamlContract.request.headers.put(k, v)
                                        }

                                        if (contractBody?.cookies) {
                                            contractBody?.cookies?.each { cookieVal ->
                                                yamlContract.request.cookies.put(cookieVal.key, cookieVal.value)
                                            }
                                        }

                                        yamlContract.request.body = contractBody?.body
                                        yamlContract.request.bodyFromFile = contractBody?.bodyFromFile
                                        yamlContract.request.bodyFromFileAsBytes = contractBody?.bodyFromFileAsBytes

                                        if (contractBody.multipart) {
                                            yamlContract.request.multipart = new YamlContract.Multipart()
                                            yamlContract.request.matchers.multipart = new YamlContract.MultipartStubMatcher()

                                            contractBody.multipart?.params?.each { val ->
                                                try {
                                                    yamlContract.request.multipart.params.put(val.key, val.value)
                                                } catch (Exception e) {
                                                    log.error("Error processing multipart params", e)
                                                }
                                            }

                                            contractBody.multipart?.named.each { contractNamed ->
                                                YamlContract.Named named = new YamlContract.Named()
                                                named.fileContent = contractNamed.fileContent
                                                named.fileName = contractNamed.fileName
                                                named.paramName = contractNamed.paramName
                                                yamlContract.request.multipart.named.add(named)
                                            }
                                        }

                                        if (contractBody.matchers?.url) {
                                            YamlContract.KeyValueMatcher keyValueMatcher = new YamlContract.KeyValueMatcher()
                                            keyValueMatcher.key = contractBody.matchers?.url?.key
                                            keyValueMatcher.regex = contractBody.matchers?.url?.regex
                                            keyValueMatcher.predefined = contractBody.matchers?.url?.predefined
                                            keyValueMatcher.command = contractBody.matchers?.url?.command
                                            keyValueMatcher.regexType = contractBody.matchers?.url?.regexType

                                            yamlContract.request.matchers.url = keyValueMatcher
                                        }

                                        contractBody.matchers?.queryParameters?.each { matcher ->
                                            YamlContract.QueryParameterMatcher queryParameterMatcher = new YamlContract.QueryParameterMatcher()
                                            queryParameterMatcher.key = matcher.key
                                            queryParameterMatcher.value = matcher.value
                                            queryParameterMatcher.type = getMatchingTypeFromString(matcher?.type)

                                            yamlContract.request.matchers.queryParameters.add(queryParameterMatcher)
                                        }

                                        contractBody.matchers?.body?.each { bodyMatcher ->
                                            YamlContract.BodyStubMatcher bodyStubMatcher = new YamlContract.BodyStubMatcher()
                                            bodyStubMatcher.path = bodyMatcher?.path
                                            bodyStubMatcher.value = bodyMatcher.value

                                            try {
                                                if (StringUtils.isNotEmpty(bodyMatcher.type)) {
                                                    bodyStubMatcher.type = YamlContract.StubMatcherType.valueOf(bodyMatcher.type)
                                                }

                                                bodyStubMatcher.predefined = getPredefinedRegexFromString(bodyMatcher.predefined)
                                                bodyStubMatcher.minOccurrence = bodyMatcher?.minOccurrence
                                                bodyStubMatcher.maxOccurrence = bodyMatcher?.maxOccurrence
                                                bodyStubMatcher.regexType = getRegexTypeFromString(bodyMatcher.regexType)

                                            } catch (Exception e) {
                                                log.error("Error parsing body matcher in request", e)
                                            }

                                            yamlContract.request.matchers.body.add(bodyStubMatcher)
                                        }

                                        contractBody.matchers?.headers?.each { matcher ->
                                            yamlContract.request.matchers.headers.add(buildKeyValueMatcher(matcher))
                                        }

                                        contractBody.matchers?.cookies?.each { matcher ->
                                            yamlContract.request.matchers.cookies.add(buildKeyValueMatcher(matcher))
                                        }

                                        contractBody.matchers?.multipart?.each { matcher ->
                                            matcher.params?.each { param ->
                                                yamlContract.request.matchers.multipart.params.add(buildKeyValueMatcher(param))
                                            }

                                            matcher?.named?.each { namedParam ->
                                                YamlContract.MultipartNamedStubMatcher stubMatcher = new YamlContract.MultipartNamedStubMatcher()
                                                stubMatcher.paramName = namedParam.paramName
                                                try {

                                                    if (StringUtils.isNotEmpty(namedParam?.fileName?.reqex)) {
                                                        stubMatcher.fileName = new YamlContract.ValueMatcher(
                                                                regex: namedParam?.fileName?.reqex,
                                                                predefined: getPredefinedRegexFromString(matcher?.fileName?.predefined))
                                                    }

                                                    if (StringUtils.isNotEmpty(namedParam?.fileContent?.reqex)) {
                                                        stubMatcher.fileContent = new YamlContract.ValueMatcher(
                                                                regex: namedParam?.fileContent?.reqex,
                                                                predefined: getPredefinedRegexFromString(matcher?.fileContent?.predefined))
                                                    }

                                                    if (StringUtils.isNotEmpty(namedParam?.contentType?.reqex)) {
                                                        stubMatcher.contentType = new YamlContract.ValueMatcher(
                                                                regex: namedParam?.contentType?.reqex,
                                                                predefined: getPredefinedRegexFromString(matcher?.contentType?.predefined))
                                                    }
                                                } catch (Exception e) {
                                                    log.error("Error parsging multipart matcher in request", e)
                                                }

                                                yamlContract.request.matchers.multipart.named.add(stubMatcher)
                                            }
                                        }
                                    } //  contract request
                                }
                            }

                            // process responses
                            if (operation?.responses) {
                                operation.responses.each { openApiResponse ->
                                    if (openApiResponse?.value?.extensions?.'x-contracts') {
                                        openApiResponse?.value?.extensions?.'x-contracts'?.each { responseContract ->
                                            if (responseContract.contractId == contractId) {
                                                yamlContract.response = new YamlContract.Response()

                                                def httpResponse = openApiResponse.key.replaceAll("[^a-zA-Z0-9 ]+", "")

                                                yamlContract.response.status = Integer.valueOf(httpResponse)

                                                yamlContract.response.body = responseContract?.body
                                                yamlContract.response.bodyFromFile = responseContract?.bodyFromFile
                                                yamlContract.response.bodyFromFileAsBytes = responseContract?.bodyFromFileAsBytes

                                                responseContract.headers?.each { responseHeader ->
                                                    yamlContract.response.headers.put(responseHeader.key, responseHeader.value)
                                                }
                                                //matchers
                                                responseContract?.matchers?.body?.each { matcher ->
                                                    YamlContract.BodyTestMatcher bodyTestMatcher = new YamlContract.BodyTestMatcher()
                                                    bodyTestMatcher.path = matcher.path
                                                    bodyTestMatcher.value = matcher.value

                                                    try {
                                                        if (StringUtils.isNotEmpty(matcher.type)) {
                                                            bodyTestMatcher.type = YamlContract.TestMatcherType.valueOf(matcher.type)
                                                        }
                                                        bodyTestMatcher.minOccurrence = matcher?.minOccurrence
                                                        bodyTestMatcher.maxOccurrence = matcher?.maxOccurrence
                                                        bodyTestMatcher.predefined = getPredefinedRegexFromString(matcher.predefined)
                                                        bodyTestMatcher.regexType = getRegexTypeFromString(matcher.regexType)

                                                    } catch (Exception e) {
                                                        log.error("Error parsing body matcher in response", e)
                                                    }

                                                    yamlContract.response.matchers.body.add(bodyTestMatcher)
                                                }

                                                responseContract?.matchers?.headers?.each { headerMatcher ->
                                                    YamlContract.TestHeaderMatcher testHeaderMatcher = new YamlContract.TestHeaderMatcher()
                                                    testHeaderMatcher.key = headerMatcher.key
                                                    testHeaderMatcher.regex = headerMatcher.regex
                                                    testHeaderMatcher.command = headerMatcher.command
                                                    testHeaderMatcher.predefined = getPredefinedRegexFromString(headerMatcher.predefined)
                                                    testHeaderMatcher.regexType = getRegexTypeFromString(headerMatcher.regexType)

                                                    yamlContract.response.matchers.headers.add(testHeaderMatcher)
                                                }

                                                responseContract?.matchers?.cookies?.each { matcher ->
                                                    YamlContract.TestCookieMatcher testCookieMatcher = new YamlContract.TestCookieMatcher()
                                                    testCookieMatcher.key = matcher.key
                                                    testCookieMatcher.regex = matcher.regex
                                                    testCookieMatcher.command = matcher.command
                                                    testCookieMatcher.predefined = getPredefinedRegexFromString(matcher.predefined)
                                                    testCookieMatcher.regexType = getRegexTypeFromString(matcher.regexType)

                                                    yamlContract.response.matchers.cookies.add(testCookieMatcher)
                                                }

                                                try {
                                                    yamlContract.response.async = responseContract.async

                                                    if (StringUtils.isNotEmpty(responseContract.fixedDelayMilliseconds)
                                                            && StringUtils.isNumeric(responseContract.fixedDelayMilliseconds)) {
                                                        yamlContract.response.fixedDelayMilliseconds = responseContract.fixedDelayMilliseconds
                                                    }
                                                } catch (Exception e) {
                                                    log.error("Error with setting aysnc property or fixedDelayMilliseconds", e)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (!yamlContract.response) {
                                println "Warning: Response Object is null. Verify Response Object on contract and for proper contract Ids"
                                yamlContract.response = new YamlContract.Response()
                                // prevents NPE in contract conversion
                            }

                            File tempFile = File.createTempFile("sccoa3", ".yml")

                            ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.SPLIT_LINES).enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE))
                            mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS)
                            mapper.writeValue(tempFile, yamlContract)

                            log.debug(tempFile.getAbsolutePath())

                            sccContracts.addAll(yamlToContracts.convertFrom(tempFile))
                        } else {
                            YamlContract ignored = new YamlContract()
                            ignored.name = "Ignored Contract"
                            ignored.ignored = true
                            ignored.request = new YamlContract.Request()
                            ignored.request.url = "/ignored"
                            ignored.request.method = "GET"
                            ignored.response = new YamlContract.Response()
                          //  sccContracts.add(ignored)
                        }
                    }
                }
            }
        }

        return sccContracts
    }

    YamlContract.KeyValueMatcher buildKeyValueMatcher(def matcher) {
        YamlContract.KeyValueMatcher keyValueMatcher = new YamlContract.KeyValueMatcher()
        keyValueMatcher.key = matcher?.key
        keyValueMatcher.regex = matcher?.regex
        keyValueMatcher.command = matcher?.command
        keyValueMatcher.predefined = getPredefinedRegexFromString(matcher?.predefined)
        keyValueMatcher.regexType = getRegexTypeFromString(matcher?.regexType)

        return keyValueMatcher
    }

    YamlContract.PredefinedRegex getPredefinedRegexFromString(String val){
        if (StringUtils.isNotBlank(val)) {
            try {
                return YamlContract.PredefinedRegex.valueOf(val)
            } catch (Exception e) {
                log.error("Error parsing PredefinedRegex", e)
            }
        }
        return null
    }

    YamlContract.RegexType getRegexTypeFromString(String val) {
        if (StringUtils.isNotBlank(val)) {
            try {
                return YamlContract.RegexType.valueOf(val)
            } catch (Exception e) {
                log.error("Error parsing RegexType", e)
            }
        }
        return null
    }

    YamlContract.MatchingType getMatchingTypeFromString(String val) {
        if (StringUtils.isNotBlank(val)) {
            try {
                return YamlContract.MatchingType.valueOf(val)
            } catch (Exception e) {
                log.error("Error parsing MatchingType", e)
            }
        }
        return null
    }

    @Override
    Collection<PathItem> convertTo(Collection<Contract> contract) {
        throw new RuntimeException("Not Implemented")
    }

    boolean checkServiceEnabled(String serviceName){

        //if not set on contract or sys env, return true
        if(!serviceName) {
            log.debug("Service Name Not Set on Contract, returning true")
            return true
        }

        String[] propValues = StringUtils.split(System.getProperty(SERVICE_NAME_KEY), ',')?.collect {StringUtils.trim(it)}

        if (!propValues) {
            log.debug("System Property - " + SERVICE_NAME_KEY + " - Not set, returning true ")
            return true
        }

        return propValues.contains(serviceName)

    }
}
