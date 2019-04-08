package org.springframework.cloud.contract.verifier.converter

import spock.lang.Specification


/**
 * Created by jt on 2019-04-02.
 */
class ServiceNameTest extends Specification {

    URL petstoreUrl = OpenApiContactConverterTest.getResource("/openapi/openapi_petstore.yml")
    File petstoreFile = new File(petstoreUrl.toURI())

    OpenApiContractConverter contactConverter

    void setup() {
        contactConverter = new OpenApiContractConverter()
    }

    void cleanup() {
        System.properties.remove(OpenApiContractConverter.SERVICE_NAME_KEY)
    }

    def "Test Contract Not Set"(){
        when:
        def enabled = contactConverter.checkServiceEnabled(null)

        then:
        enabled
    }

    def "Test Contract Set, System Param Not Set"(){
        when:
        def enabled = contactConverter.checkServiceEnabled("FOO")

        then:
        enabled
    }

    def "Test Contract Set, Should Match one value"() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, "ServiceA")

        when:
        def enabled = contactConverter.checkServiceEnabled("ServiceA")

        then:
        enabled
    }

    def "Test Contract Set, Should NOT Match one value"() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, "ServiceA")

        when:
        def enabled = contactConverter.checkServiceEnabled("ServiceB")

        then:
        !enabled
    }

    def "Test Contract Set, Should Match List of Values"() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, "ServiceA,ServiceB")

        when:
        def enabled = contactConverter.checkServiceEnabled("ServiceB")

        then:
        enabled
    }

    def "Test Contract Set, Should Match List of Values with spaces"() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, "ServiceA,  ServiceB")

        when:
        def enabled = contactConverter.checkServiceEnabled("ServiceB")

        then:
        enabled
    }

    def "testNoSystemparam"() {
        when:
        def contracts = contactConverter.convertFrom(petstoreFile)

        then:
        contracts
        contracts.size() == 3
    }

    def "test Service A"() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, "serviceA")

        when:
        def contracts = contactConverter.convertFrom(petstoreFile)

        then:
        contracts
        contracts.size() == 1
    }

    def "test Service A and B"() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, "serviceA, serviceB")

        when:
        def contracts = contactConverter.convertFrom(petstoreFile)

        then:
        contracts
        contracts.size() == 2
    }

    def "test Service A and C"() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, "serviceA, serviceC")

        when:
        def contracts = contactConverter.convertFrom(petstoreFile)

        then:
        contracts
        contracts.size() == 2
    }

    def "test Service A, B and C"() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, "serviceA,serviceB, serviceC")

        when:
        def contracts = contactConverter.convertFrom(petstoreFile)

        then:
        contracts
        contracts.size() == 3
    }
}
