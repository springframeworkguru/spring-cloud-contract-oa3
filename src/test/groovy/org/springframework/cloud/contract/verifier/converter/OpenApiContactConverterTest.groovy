package org.springframework.cloud.contract.verifier.converter

import org.springframework.cloud.contract.spec.Contract
import spock.lang.Specification

/**
 * Created by jt on 5/24/18.
 */
class OpenApiContactConverterTest extends Specification {

    URL contractUrl = OpenApiContactConverterTest.getResource("/yml/contract.yml")
    File contractFile = new File(contractUrl.toURI())
    URL contractOA3Url = OpenApiContactConverterTest.getResource("/openapi/contract_OA3.yml")
    File contractOA3File = new File(contractOA3Url.toURI())

    URL contractOA3UrlPath = OpenApiContactConverterTest.getResource("/openapi/contract_OA3_contractPath.yml")
    File contractOA3FilePath = new File(contractOA3UrlPath.toURI())

    URL fruadApiUrl = OpenApiContactConverterTest.getResource("/openapi/openapi.yml")
    File fraudApiFile = new File(fruadApiUrl.toURI())

    URL payorApiUrl = OpenApiContactConverterTest.getResource("/openapi/payor_example.yml")
    File payorApiFile = new File(payorApiUrl.toURI())

    URL veloApiUrl = OpenApiContactConverterTest.getResource("/openapi/velooa3.yaml")
    File veloApiFile = new File(veloApiUrl.toURI())

    URL matcherUrl = OpenApiContactConverterTest.getResource("/yml/contract_matchers.yml")
    File matcherFile = new File(matcherUrl.toURI())

    URL matcherUrlOA3 = OpenApiContactConverterTest.getResource("/openapi/contract_matchers.yml")
    File matcherFileOA3 = new File(matcherUrlOA3.toURI())

    OpenApiContractConverter contactConverter
    YamlContractConverter yamlContractConverter

    void setup() {
        contactConverter = new OpenApiContractConverter()
        yamlContractConverter = new YamlContractConverter()
    }

    def "IsAccepted True"() {
        given:
        File file = new File('src/test/resources/openapi/openapi_petstore.yml')
        when:

        def result = contactConverter.isAccepted(file)

        then:
        result
    }

    def "IsAccepted True 2"() {
        given:
        File file = new File('src/test/resources/openapi/openapi.yml')
        when:

        def result = contactConverter.isAccepted(file)

        then:
        result
    }

    def "IsAccepted False"() {
        given:
        File file = new File('foo')
        when:

        def result = contactConverter.isAccepted(file)

        then:
        !result

    }

    def "ConvertFrom - should not go boom"() {
        given:
        File file = new File('src/test/resources/openapi/openapi.yml')
        when:

        def result = contactConverter.convertFrom(file)

        println result

        then:
        result != null
    }


    def "Test Yaml Contract"() {
        given:
        Contract yamlContract = yamlContractConverter.convertFrom(contractFile).first()
        Collection<Contract> oa3Contract = contactConverter.convertFrom(contractOA3File)

        when:

        Contract openApiContract = oa3Contract.find { it.name.equalsIgnoreCase("some name") }

        then:
        openApiContract
        yamlContract.request.url == openApiContract.request.url
        yamlContract.request.method == openApiContract.request.method
        yamlContract.request.cookies == openApiContract.request.cookies
        yamlContract.request.headers == openApiContract.request.headers
        yamlContract.request.body == openApiContract.request.body
        yamlContract.request.bodyMatchers == openApiContract.request.bodyMatchers
        yamlContract.response.status == openApiContract.response.status
        yamlContract.response.headers == openApiContract.response.headers
        yamlContract.response.bodyMatchers == openApiContract.response.bodyMatchers
        yamlContract.response.body == openApiContract.response.body
        yamlContract == openApiContract

    }

    def "test OA3 Fraud Yml"() {
        given:
        Collection<Contract> oa3Contract = contactConverter.convertFrom(fraudApiFile)

        when:
        Contract contract = oa3Contract.getAt(0)

        then:
        contract
        oa3Contract.size() == 6

    }

    def "Test parse of test path"() {
        given:
        Collection<Contract> oa3Contract = contactConverter.convertFrom(contractOA3FilePath)

        when:
        Contract contract = oa3Contract.getAt(0)

        then:
        contract
        contract.getRequest().url.clientValue.equals("/foo1")
    }

    def "Test Parse of Payor example contracts"() {

        given:
        Collection<Contract> oa3Contract = contactConverter.convertFrom(payorApiFile)

        when:
        Contract contract = oa3Contract.getAt(0)

        then:
        contract
    }

    def "Test Parse of Velo Contracts"() {

        given:
        Collection<Contract> veloContracts = contactConverter.convertFrom(veloApiFile)

        when:
        //Contract contract = oa3Contract.getAt(0)
        Contract veloContract = veloContracts.getAt(0)

        then:
        //contract
        contactConverter.isAccepted(veloApiFile)
    }

    def "Test Parse of Matchers"() {

        given:
        Contract yamlContract = yamlContractConverter.convertFrom(matcherFile).first()
        Collection<Contract> matcherContracts = contactConverter.convertFrom(matcherFileOA3)

        when:

        Contract openApiContract = matcherContracts.getAt(0)

        then:
        //contract
        contactConverter.isAccepted(matcherFileOA3)
       // yamlContract.request.url == openApiContract.request.url
        yamlContract.request.method == openApiContract.request.method
        yamlContract.request.cookies == openApiContract.request.cookies
        yamlContract.request.headers == openApiContract.request.headers
        yamlContract.request.body == openApiContract.request.body  // has empty list, which does not convert
        yamlContract.request.bodyMatchers == openApiContract.request.bodyMatchers
        yamlContract.response.status == openApiContract.response.status
        yamlContract.response.headers == openApiContract.response.headers
        yamlContract.response.bodyMatchers == openApiContract.response.bodyMatchers
        yamlContract.response.body == openApiContract.response.body // has empty list, which does not convert

    }

}
