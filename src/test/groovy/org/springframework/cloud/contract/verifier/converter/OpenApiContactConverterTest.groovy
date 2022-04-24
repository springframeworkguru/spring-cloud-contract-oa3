package org.springframework.cloud.contract.verifier.converter

import org.springframework.cloud.contract.spec.Contract
import spock.lang.Specification

/**
 * Created by jt on 5/24/18.
 */
class OpenApiContactConverterTest extends Specification {

    static final URL CONTRACT_URL = OpenApiContactConverterTest.getResource('/yml/contract.yml')
    static final File CONTRACT_FILE = new File(CONTRACT_URL.toURI())

    static final URL CONTRACT_OA3_URL = OpenApiContactConverterTest.getResource('/openapi/contract_OA3.yml')
    static final File CONTRACT_OA3_FILE = new File(CONTRACT_OA3_URL.toURI())

    static final URL CONTRACT_OA3_URL_PATH = OpenApiContactConverterTest.getResource('/openapi/contract_OA3_contractPath.yml')
    static final File CONTRACT_OA3_FILE_PATH = new File(CONTRACT_OA3_URL_PATH.toURI())

    static final URL FRAUD_API_URL = OpenApiContactConverterTest.getResource('/openapi/openapi.yml')
    static final File FRAUD_API_FILE = new File(FRAUD_API_URL.toURI())

    static final URL PAYOR_API_URL = OpenApiContactConverterTest.getResource('/openapi/payor_example.yml')
    static final File PAYOR_API_FILE = new File(PAYOR_API_URL.toURI())

    static final URL VELO_API_URL = OpenApiContactConverterTest.getResource('/openapi/velooa3.yaml')
    static final File VELO_API_FILE = new File(VELO_API_URL.toURI())

    static final URL MATCHER_URL = OpenApiContactConverterTest.getResource('/yml/contract_matchers.yml')
    static final File MATCHER_FILE = new File(MATCHER_URL.toURI())

    static final URL MATCHER_URL_OA3 = OpenApiContactConverterTest.getResource('/openapi/contract_matchers.yml')
    static final File MATCHER_FILE_OA3 = new File(MATCHER_URL_OA3.toURI())

    OpenApiContractConverter contactConverter = new OpenApiContractConverter()
    YamlContractConverter yamlContractConverter = new YamlContractConverter()

    def 'IsAccepted True'() {
        given:
        File file = new File('src/test/resources/openapi/openapi_petstore.yml')

        expect:
        contactConverter.isAccepted(file)
    }

    def 'IsAccepted True 2'() {
        given:
        File file = new File('src/test/resources/openapi/openapi.yml')

        expect:
        contactConverter.isAccepted(file)
    }

    def 'IsAccepted False'() {
        given:
        File file = new File('foo')

        expect:
        !contactConverter.isAccepted(file)
    }

    def 'ConvertFrom - should not go boom'() {
        given:
        File file = new File('src/test/resources/openapi/openapi.yml')

        expect:
        contactConverter.convertFrom(file)
    }

    def 'Test Yaml Contract'() {
        when:
        Contract yamlContract = yamlContractConverter.convertFrom(CONTRACT_FILE).first()
        Collection<Contract> oa3Contract = contactConverter.convertFrom(CONTRACT_OA3_FILE)

        then:
        Contract openApiContract = oa3Contract.find { it.name == 'some name' }
        with(yamlContract.request) {
            it.url == openApiContract.request.url
            it.method == openApiContract.request.method
            it.cookies == openApiContract.request.cookies
            it.headers == openApiContract.request.headers
            it.body == openApiContract.request.body
            it.bodyMatchers == openApiContract.request.bodyMatchers
        }
        with(yamlContract.response) {
            it.status == openApiContract.response.status
            it.headers == openApiContract.response.headers
            it.bodyMatchers == openApiContract.response.bodyMatchers
            it.body == openApiContract.response.body
        }
        yamlContract == openApiContract
    }

    def 'Test OA3 Fraud Yml'() {
        given:
        Collection<Contract> oa3Contract = contactConverter.convertFrom(FRAUD_API_FILE)

        when:
        Contract contract = oa3Contract[0]

        then:
        contract
        oa3Contract.size() == 6
    }

    def 'Test parse of test path'() {
        when:
        Collection<Contract> oa3Contract = contactConverter.convertFrom(CONTRACT_OA3_FILE_PATH)

        then:
        oa3Contract[0]
        oa3Contract[0].request.url.clientValue == '/foo1'
    }

    def 'Test Parse of Payor example contracts'() {
        when:
        Collection<Contract> oa3Contract = contactConverter.convertFrom(PAYOR_API_FILE)

        then:
        oa3Contract[0]
    }

    def 'Test Parse of Velo Contracts'() {
        when:
        Collection<Contract> veloContracts = contactConverter.convertFrom(VELO_API_FILE)

        then:
        veloContracts[0]
        contactConverter.isAccepted(VELO_API_FILE)
    }

    def 'Test Parse of Matchers'() {
        given:
        Contract yamlContract = yamlContractConverter.convertFrom(MATCHER_FILE).first()
        Collection<Contract> matcherContracts = contactConverter.convertFrom(MATCHER_FILE_OA3)

        when:
        Contract openApiContract = matcherContracts[0]

        then:
        contactConverter.isAccepted(MATCHER_FILE_OA3)
        with(yamlContract.request) {
            it.urlPath.serverValue == openApiContract.request.url.serverValue
            it.urlPath.clientValue as String == openApiContract.request.url.clientValue as String
            it.method == openApiContract.request.method
            it.cookies == openApiContract.request.cookies
            it.headers == openApiContract.request.headers
            it.body == openApiContract.request.body  // has empty list, which does not convert
            it.bodyMatchers == openApiContract.request.bodyMatchers
        }
        with(yamlContract.response) {
            yamlContract.response.status == openApiContract.response.status
            yamlContract.response.headers == openApiContract.response.headers
            yamlContract.response.bodyMatchers == openApiContract.response.bodyMatchers
            yamlContract.response.body == openApiContract.response.body // has empty list, which does not convert
        }
    }
}
