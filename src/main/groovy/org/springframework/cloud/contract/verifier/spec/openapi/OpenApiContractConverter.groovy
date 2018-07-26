package org.springframework.cloud.contract.verifier.spec.openapi

import groovy.util.logging.Slf4j
import io.swagger.oas.models.PathItem
import io.swagger.oas.models.media.MediaType
import io.swagger.parser.v3.OpenAPIV3Parser
import org.springframework.cloud.contract.spec.Contract
import org.springframework.cloud.contract.spec.ContractConverter
import org.springframework.cloud.contract.spec.internal.DslProperty
import org.springframework.cloud.contract.spec.internal.ExecutionProperty
import org.springframework.cloud.contract.spec.internal.MatchingTypeValue
import org.springframework.cloud.contract.spec.internal.RegexPatterns
import org.springframework.cloud.contract.verifier.converter.YamlContract

import java.util.regex.Pattern

import static org.apache.commons.lang3.StringUtils.isNumeric

/**
 * Created by John Thompson on 5/24/18.
 */
@Slf4j
class OpenApiContractConverter implements ContractConverter<Collection<PathItem>> {

    public static final OpenApiContractConverter INSTANCE = new OpenApiContractConverter()

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
                            if (contracts.size > 0) {
                                contractsFound = true
                            }
                        }
                    }
                }
            }

            return contractsFound
        } catch (Exception e) {
            log.debug(e.message)
            return false
        }
    }

    @Override
    Collection<Contract> convertFrom(File file) {
        def sccContracts = []

        def spec = new OpenAPIV3Parser().read(file.path)

        spec?.paths?.each { path, pathItem ->
            pathItem.readOperations().each { operation ->
                if (operation?.extensions?."x-contracts") {
                    operation.extensions."x-contracts".each { openApiContract ->

                        def contractId = openApiContract.contractId

                        sccContracts.add(
                                Contract.make {
                                    name = openApiContract?.name
                                    description = openApiContract?.description
                                    priority = openApiContract?.priority

                                    if(openApiContract.ignored) {
                                        ignored = openApiContract.ignored
                                    } else {
                                        ignored = false
                                    }

                                    request {
                                        if (pathItem?.get?.is(operation)) {
                                            method("GET")
                                        } else if (pathItem?.put.is(operation)) {
                                            method("PUT")
                                        } else if (pathItem?.post.is(operation)) {
                                            method("POST")
                                        } else if (pathItem?.delete.is(operation)) {
                                            method("DELETE")
                                        } else if (pathItem?.patch.is(operation)) {
                                            method("PATCH")
                                        }
                                        if (operation?.parameters) {
                                            url(path) {
                                                queryParameters {
                                                    operation?.parameters?.each { openApiParam ->
                                                        openApiParam?.extensions?."x-contracts".each { contractParam ->
                                                            if (contractParam.contractId == contractId) {
                                                                if (openApiParam.in == 'path' || openApiParam.in == 'query') {
                                                                    parameter(openApiParam.name, contractParam.default)
                                                                }
                                                                if (openApiParam.in == 'header') {
                                                                    headers {
                                                                        header(openApiParam.name, contractParam.default)
                                                                    }
                                                                }
                                                                if (openApiParam.in == 'cookie') {
                                                                    cookies {
                                                                        cookie(openApiParam.name, contractParam.default)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            url(path)
                                        }
                                        headers {
                                            if (openApiContract?.requestHeaders) {
                                                openApiContract?.requestHeaders?.each { xheader ->
                                                    xheader.each { k, v ->
                                                        header(k, v)
                                                    }
                                                }
                                            }

                                            if(operation?.requestBody?.content){
                                                LinkedHashMap<String, MediaType> content = operation?.requestBody?.content

                                                //todo - not sure if there is a use case for more than one map entry here
                                                header("Content-Type", content.entrySet().getAt(0).key)
                                            }
                                        }

                                        if (operation?.requestBody?.extensions?."x-contracts") {
                                            operation?.requestBody?.extensions?."x-contracts"?.each { contractBody ->
                                                if (contractBody.contractId == contractId) {
                                                    body(contractBody.body)
                                                    bodyMatchers {
                                                        contractBody.matchers?.body?.each { matcher ->
                                                            MatchingTypeValue value = null
                                                            switch (matcher.type) {
                                                                case 'by_date':
                                                                    value = byDate()
                                                                    break
                                                                case 'by_time':
                                                                    value = byTime()
                                                                    break
                                                                case 'by_timestamp':
                                                                    value = byTimestamp()
                                                                    break
                                                                case 'by_regex':
                                                                    String regex = matcher.value
                                                                    if (matcher.predefined) {
                                                                        YamlContract.PredefinedRegex  pdRegx = YamlContract.PredefinedRegex.valueOf(matcher.predefined)
                                                                        regex = predefinedToPattern(pdRegx).pattern()
                                                                    }
                                                                    value = byRegex(regex)
                                                                    break
                                                                case 'by_equality':
                                                                    value = byEquality()
                                                                    break
                                                            }
                                                            jsonPath(matcher.path, value)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    response {
                                        if (operation?.responses) {
                                            operation.responses.each { openApiResponse ->
                                                if (openApiResponse?.value?.extensions?.'x-contracts') {
                                                    openApiResponse?.value?.extensions?.'x-contracts'?.each { responseContract ->
                                                        if (responseContract.contractId == contractId) {

                                                                def httpResponse = openApiResponse.key.replaceAll("[^a-zA-Z0-9 ]+","")

                                                                if (isNumeric(httpResponse)) {
                                                                    status(httpResponse as Integer)
                                                                }

                                                                def contentTypeSB = new StringBuffer()

                                                                openApiResponse.getValue()?.content?.keySet()?.each { val ->
                                                                    contentTypeSB.append(val)
                                                                    contentTypeSB.append(';')
                                                                }

                                                                headers {

                                                                    responseContract.headers.each { String headerKey, Object headerValue ->
                                                                        def matcher = responseContract?.matchers?.headers?.find { it.key == headerKey }
                                                                        if (headerValue instanceof List) {
                                                                            ((List) headerValue).each {
                                                                                Object serverValue = serverValue(it, matcher, headerKey)
                                                                                header(headerKey, new DslProperty(it, serverValue))
                                                                            }
                                                                        } else {
                                                                            Object serverValue = serverValue(headerValue, matcher, headerKey)
                                                                            header(headerKey, new DslProperty(headerValue, serverValue))
                                                                        }
                                                                    }
                                                                }

                                                                if(responseContract.cookies){
                                                                    cookies {
                                                                        responseContract.cookies.each { responseCookie ->
                                                                            def matcher =responseContract.matchers.cookies.find { it.key == responseCookie.key }
                                                                            Object serverValue = serverCookieValue(responseCookie.value, matcher, responseCookie.key)

                                                                            cookie(responseCookie.key, new DslProperty(responseCookie.value, serverValue))
                                                                        }
                                                                    }
                                                                }

                                                                if (responseContract.body) body(responseContract.body)
                                                                if (responseContract.bodyFromFile) body(file(responseContract.bodyFromFile))
                                                                if (responseContract.async) async()

                                                                bodyMatchers {
                                                                    responseContract.matchers?.body?.each { matcher ->
                                                                        MatchingTypeValue value = null
                                                                        switch (matcher.type) {
                                                                            case 'by_date':
                                                                                value = byDate()
                                                                                break
                                                                            case 'by_time':
                                                                                value = byTime()
                                                                                break
                                                                            case 'by_timestamp':
                                                                                value = byTimestamp()
                                                                                break
                                                                            case 'by_regex':
                                                                                String regex = matcher.value
                                                                                if (matcher.predefined) {
                                                                                    regex = predefinedToPattern(matcher.predefined).pattern()
                                                                                }
                                                                                value = byRegex(regex)
                                                                                break
                                                                            case 'by_equality':
                                                                                value = byEquality()
                                                                                break
                                                                            case 'by_type':
                                                                                value = byType() {
                                                                                    if (matcher.minOccurrence != null) minOccurrence(matcher.minOccurrence)
                                                                                    if (matcher.maxOccurrence != null) maxOccurrence(matcher.maxOccurrence)
                                                                                }
                                                                                break
                                                                            case 'by_command':
                                                                                value = byCommand(matcher.value)
                                                                                break
                                                                            case 'by_null':
                                                                                value = byNull()
                                                                                break
                                                                        }
                                                                        jsonPath(matcher.path, value)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                })
                    }
                }
            }
        }

        return sccContracts
    }

    //todo - extend from yaml converter?
    protected Pattern predefinedToPattern(YamlContract.PredefinedRegex predefinedRegex) {
        RegexPatterns patterns = new RegexPatterns()
        switch (predefinedRegex) {
            case YamlContract.PredefinedRegex.only_alpha_unicode:
                return patterns.onlyAlphaUnicode()
            case YamlContract.PredefinedRegex.number:
                return patterns.number()
            case YamlContract.PredefinedRegex.any_double:
                return patterns.aDouble()
            case YamlContract.PredefinedRegex.any_boolean:
                return patterns.anyBoolean()
            case YamlContract.PredefinedRegex.ip_address:
                return patterns.ipAddress()
            case YamlContract.PredefinedRegex.hostname:
                return patterns.hostname()
            case YamlContract.PredefinedRegex.email:
                return patterns.email()
            case YamlContract.PredefinedRegex.url:
                return patterns.url()
            case YamlContract.PredefinedRegex.uuid:
                return patterns.uuid()
            case YamlContract.PredefinedRegex.iso_date:
                return patterns.isoDate()
            case YamlContract.PredefinedRegex.iso_date_time:
                return patterns.isoDateTime()
            case YamlContract.PredefinedRegex.iso_time:
                return patterns.isoTime()
            case YamlContract.PredefinedRegex.iso_8601_with_offset:
                return patterns.iso8601WithOffset()
            case YamlContract.PredefinedRegex.non_empty:
                return patterns.nonEmpty()
            case YamlContract.PredefinedRegex.non_blank:
                return patterns.nonBlank()
        }
    }

    //todo - extend from yaml converter?
    protected Object serverValue(Object value, def matcher, String key) {
        Object serverValue = value
        if (matcher?.regex) {
            serverValue = Pattern.compile(matcher.regex)
            Pattern pattern = (Pattern) serverValue
            assertPatternMatched(pattern, value, key)
        } else if (matcher?.predefined) {
            Pattern pattern = predefinedToPattern(matcher.predefined)
            serverValue = pattern
            assertPatternMatched(pattern, value, key)
        } else if (matcher?.command) {
            serverValue = new ExecutionProperty(matcher.command)
        }
        return serverValue
    }

    //todo - extend from yaml converter?
    protected Object serverCookieValue(Object value, def matcher, String key) {
        Object serverValue = value
        if (matcher?.regex) {
            serverValue = Pattern.compile(matcher.regex)
            Pattern pattern = (Pattern) serverValue
            assertPatternMatched(pattern, value, key)
        } else if (matcher?.predefined) {
            Pattern pattern = predefinedToPattern(matcher.predefined)
            serverValue = pattern
            assertPatternMatched(pattern, value, key)
        }
        return serverValue
    }

    //todo - extend from yaml converter?
    protected Object clientValue(Object value, YamlContract.KeyValueMatcher matcher, String key) {
        Object clientValue = value
        if (matcher?.regex) {
            clientValue = Pattern.compile(matcher.regex)
            Pattern pattern = (Pattern) clientValue
            assertPatternMatched(pattern, value, key)
        } else if (matcher?.predefined) {
            Pattern pattern = predefinedToPattern(matcher.predefined)
            clientValue = pattern
            assertPatternMatched(pattern, value, key)
        }
        return clientValue
    }

    //todo - extend from yaml converter?
    private void assertPatternMatched(Pattern pattern, value, String key) {
        boolean matches = pattern.matcher(value.toString()).matches()
        if (!matches) throw new IllegalStateException("Broken headers! A header with " +
                "key [${key}] with value [${value}] is not matched by regex [${pattern.pattern()}]")
    }
    @Override
    Collection<PathItem> convertTo(Collection<Contract> contract) {

        throw new RuntimeException("Not Implemented")

    }
}
