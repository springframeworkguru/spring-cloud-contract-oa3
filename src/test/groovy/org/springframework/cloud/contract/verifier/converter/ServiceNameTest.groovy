package org.springframework.cloud.contract.verifier.converter

import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

/**
 * Created by jt on 2019-04-02.
 */
class ServiceNameTest extends Specification {

    static final URL PETSTORE_URL = OpenApiContactConverterTest.getResource('/openapi/openapi_petstore.yml')
    static final File PETSTORE_FILE = new File(PETSTORE_URL.toURI())

    OpenApiContractConverter contactConverter = new OpenApiContractConverter()

    def 'Test Contract Not Set'() {
        expect:
        contactConverter.checkServiceEnabled(null)
    }

    def 'Test Contract Set, System Param Not Set'() {
        expect:
        contactConverter.checkServiceEnabled('FOO')
    }

    @RestoreSystemProperties
    def 'Test Contract Set, Should Match one value'() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, 'ServiceA')

        expect:
        contactConverter.checkServiceEnabled('ServiceA')
    }

    @RestoreSystemProperties
    def 'Test Contract Set, Should NOT Match one value'() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, 'ServiceA')

        expect:
        !contactConverter.checkServiceEnabled('ServiceB')
    }

    @RestoreSystemProperties
    def 'Test Contract Set, Should Match List of Values'() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, 'ServiceA,ServiceB')

        expect:
        contactConverter.checkServiceEnabled('ServiceB')
    }

    @RestoreSystemProperties
    def 'Test Contract Set, Should Match List of Values with spaces'() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, 'ServiceA,  ServiceB')

        expect:
        contactConverter.checkServiceEnabled('ServiceB')
    }

    def 'Test no system parameters'() {
        expect:
        def contracts = contactConverter.convertFrom(PETSTORE_FILE)
        contracts.size() == 3
    }

    @RestoreSystemProperties
    def 'test Service A'() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, 'serviceA')

        expect:
        def contracts = contactConverter.convertFrom(PETSTORE_FILE)
        contracts.size() == 1
    }

    @RestoreSystemProperties
    def 'test Service A and B'() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, 'serviceA, serviceB')

        expect:
        def contracts = contactConverter.convertFrom(PETSTORE_FILE)
        contracts.size() == 2
    }

    @RestoreSystemProperties
    def 'test Service A and C'() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, 'serviceA, serviceC')

        expect:
        def contracts = contactConverter.convertFrom(PETSTORE_FILE)
        contracts.size() == 2
    }

    @RestoreSystemProperties
    def 'test Service A, B and C'() {
        given:
        System.properties.setProperty(OpenApiContractConverter.SERVICE_NAME_KEY, 'serviceA,serviceB, serviceC')

        expect:
        def contracts = contactConverter.convertFrom(PETSTORE_FILE)
        contracts.size() == 3
    }
}
