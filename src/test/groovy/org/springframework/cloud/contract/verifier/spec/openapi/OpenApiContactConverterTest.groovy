package org.springframework.cloud.contract.verifier.spec.openapi

import org.springframework.cloud.contract.spec.Contract
import org.springframework.cloud.contract.verifier.converter.YamlContractConverter
import spock.lang.Specification

/**
 * Created by jt on 5/24/18.
 */
class OpenApiContactConverterTest extends Specification {

    URL contractUrl = OpenApiContactConverterTest.getResource("/yml/contract.yml")
    File contractFile = new File(contractUrl.toURI())
    URL contractOA3Url = OpenApiContactConverterTest.getResource("/yml/contract_OA3.yml")
    File contractOA3File = new File(contractOA3Url.toURI())

    URL fruadApiUrl = OpenApiContactConverterTest.getResource("/openapi/openapi.yml")
    File fraudApiFile = new File(fruadApiUrl.toURI())

    OpenApiContractConverter contactConverter
    YamlContractConverter yamlContractConverter

    void setup() {
        contactConverter = new OpenApiContractConverter()
        yamlContractConverter = new YamlContractConverter()
    }

    def "IsAccepted True"() {
        given:
        File file = new File('src/test/resources/openapi/openapi_petstore.yaml')
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
        File file = new File('src/test/resources/openapi/openapi.yaml')
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
}
