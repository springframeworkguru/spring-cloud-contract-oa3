# Spring Cloud Contract OpenAPI 3.0 Contract Converter

[![Gitter chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/spring-cloud-contract-oa3/Lobby)

[![CircleCI](https://circleci.com/gh/springframeworkguru/spring-cloud-contract-oa3.svg?style=svg)](https://circleci.com/gh/springframeworkguru/spring-cloud-contract-oa3)

![QualityGate](https://sonarcloud.io/api/project_badges/measure?project=guru.springframework%3Aspring-cloud-contract-oa3&metric=alert_status)
## OpenAPI 3.0 Converter

The [OpenAPI Specification (OAS)](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md) defines a
standard, language-agnostic interface to RESTful APIs which allows both humans and computers to discover and understand 
the capabilities of the service without access to source code, documentation, or through network traffic inspection. 
When properly defined, a consumer can understand and interact with the remote service with a minimal amount of 
implementation logic.

An OpenAPI definition can then be used by documentation generation tools to display the API, code generation tools to
generate servers and clients in various programming languages, testing tools, and many other use cases.

## Example Project
A complete working example project using Open API 3.0 to define contracts for Spring Cloud Contract is available 
[here on GitHub](https://github.com/springframeworkguru/sccoa3-fraud-example).

This project is a copy of the fraud API example commonly used in the standalone examples. The above example implements 
the same producer, client, and contracts (defined in YAML) from the [standalone YAML example](https://github.com/springframeworkguru/sccoa3-fraud-example). 

## Usage
### Maven
To enable this plugin, you will need to add the OA3 converter jar to your Spring Boot project as follows.

1. Configure your project to use [Spring Cloud Contract](https://cloud.spring.io/spring-cloud-static/spring-cloud-contract/2.1.0.RELEASE/single/spring-cloud-contract.html#maven-project).

2. Add to your maven dependencies: 
    ```xml
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-contract-verifier</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    ```
3. The artifact also needs to be added to the Maven Plugin:

```xml
    <plugin>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-contract-maven-plugin</artifactId>
        <version>${spring-cloud-contract.version}</version>
        <extensions>true</extensions>
        <configuration>
            <packageWithBaseClasses>com.example.fraud</packageWithBaseClasses>
        </configuration>
        <dependencies>
        <!--needed to include oa3 converter-->
        <dependency>
            <groupId>guru.springframework</groupId>
            <artifactId>spring-cloud-contract-oa3</artifactId>
            <version>2.0.1</version>
            </dependency>
        </dependencies>
    </plugin>
```

##  Defining Contracts in OpenAPI

Natively, OpenAPI does a great job of describing an API in a holistic manner.

OpenAPI, however, does not define API interactions. Within the native OpenAPI specification, it is not possible to
define request / response pairs. To define a contract, you need to define the API and the specific details of a
request, and the expected response.

The Open API Specification defines a number of extension points in the API. These extension points may be used to
define details about request / response pairs.

Complete details of OpenAPI 3.x extensions can be
found [here](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#specificationExtensions).

In general, most OpenAPI schema objects may be extended using objects using a property with starts with 'x-'. The extension
property is an object, which provides the necessary flexibility to define interactions.

The below snippet shows the definition of two contracts by extending the
[Operation Object](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#operationObject) of the OA3 specification.


```yaml
paths:
    /fraudcheck:
        put:
            summary: Perform Fraud Check
            x-contracts:
            - contractId: 1
              name: Should Mark Client as Fraud
              priority: 1
            - contractId: 2
              name: Should Not Mark Client as Fraud
```

The OA3 extension objects are used to define request / response pairs. *While* the OA3 objects are used to define
the API itself. Where ever possible, the _DRY Principle_ is followed (Don't Repeat Yourself).

For example:

* *Path*: Source - OA3
* *HTTP Method*: Source - OA3
* *Parameter Value for Interaction*: Source - OA3 Extension
* *Request Body for Interaction*: Source - OA3 Extension

`x-contracts` -  This is the root extension object used to define contracts. This object will always expect a list of objects. Each object in
the list will have a `contractId` property.

The `x-contracts` object may be applied to:

* [Operation Object](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#operationObject) - Used to define
individual contacts, and header level information for contracts.

* [Parameter Object](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#parameterObject) - Define Parameter
 (path, query, header, cookie) Values for interactions.

* [Request Body](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#requestBodyObject) - Define the request
body for interaction.

* [Response Object](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#responseObject) - Define
expected response for given interaction.

### OA3 Extensions for Spring Cloud Contract
Under the covers, the converter is converting from the OA3 object format, to the `YamlContract` object of Spring Cloud Contract.
This is then converted to a `Contract` object using the same converter used by Spring Cloud Contract for it's 
YAML DSL.

The YAML DSL of Spring Cloud Contract is very robust. Please review the capabilities of the YAML DSL in the official 
[Spring Cloud Contract documentation](https://cloud.spring.io/spring-cloud-contract/multi/multi__contract_dsl.html#_common_top_level_elements).

As much as practical, the object properties and names follow the YAML DSL of Spring Cloud Contract. 
#### Operation Object Extension

```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "x-contracts": {
      "type": "array",
      "items": [
        {
          "type": "object",
          "properties": {
            "contractId": {
              "type": "integer"
            },
            "name": {
              "type": "string"
            },
            "description": {
              "type": "string"
            },
            "label": {
              "type": "string"
            },
            "priority": {
              "type": "integer"
            },
            "ignored": {
              "type": "boolean"
            },
            "contractPath": {
              "type": "string"
            }
          },
          "required": [
            "contractId",
            "name",
            "description",
            "label",
            "priority",
            "ignored",
            "contractPath"
          ]
        },
        {
          "type": "object",
          "properties": {
            "contractId": {
              "type": "integer"
            },
            "name": {
              "type": "string"
            },
            "description": {
              "type": "string"
            },
            "label": {
              "type": "string"
            },
            "priority": {
              "type": "integer"
            },
            "contractPath": {
              "type": "string"
            }
          },
          "required": [
            "contractId",
            "description"
          ]
        }
      ]
    }
  },
  "required": [
    "x-contracts"
  ]
}
```
#### Parameter Object Extension
Note: Query Parameters maybe defined on the Parameter object, or within the parameter element of the Request Body extension.
```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "x-contracts": {
      "type": "array",
      "items": [
        {
          "type": "object",
          "properties": {
            "contractId": {
              "type": "integer"
            },
            "value": {
              "type": "string"
            },
            "matchers": {
              "type": "array",
              "items": [
                {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string"
                    },
                    "value": {
                      "type": "string"
                    }
                  },
                  "required": []
                }
              ]
            }
          },
          "required": [
            "contractId",
            "value"
          ]
        }
      ]
    }
  },
  "required": [
    "x-contracts"
  ]
}
```
#### Request Body Extension
```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "x-contracts": {
      "type": "array",
      "items": [
        {
          "type": "object",
          "properties": {
            "contractId": {
              "type": "integer"
            },
            "request": {
              "type": "object",
              "properties": {
                "queryParameters": {
                  "type": "array",
                  "items": [
                    {
                      "type": "object",
                      "properties": {
                        "key": {
                          "type": "string"
                        },
                        "value": {
                          "type": "integer"
                        }
                      },
                      "required": [
                        "key",
                        "value"
                      ]
                    }
                  ]
                }
              },
              "required": []
            },
            "headers": {
              "type": "object",
              "properties": {
                "Header-key": {
                  "type": "string"
                }
              },
              "required": [
                "Header-key"
              ]
            },
            "body": {
              "type": "object"
            },
            "multipart": {
              "type": "object",
                "named": {
                  "type": "array",
                  "items": [
                    {
                      "type": "object",
                      "properties": {
                        "paramName": {
                          "type": "string"
                        },
                        "fileName": {
                          "type": "string"
                        },
                        "fileContent": {
                          "type": "string"
                        }
                      },
                      "required": [
                        "paramName",
                        "fileName",
                        "fileContent"
                      ]
                    }
                  ]
                }
              },
              "required": [
                "params",
                "named"
              ]
            },
            "matchers": {
              "type": "object",
              "properties": {
                "headers": {
                  "type": "array",
                  "items": [
                    {
                      "type": "object",
                      "properties": {
                        "key": {
                          "type": "string"
                        },
                        "regex": {
                          "type": "string"
                        },
                        "predefined": {
                          "type": "string"
                        },
                        "command": {
                          "type": "string"
                        },
                        "type": {
                          "type": "string"
                        }
                      },
                      "required": []
                    }
                  ]
                },
                "body": {
                  "type": "array",
                  "items": [
                    {
                      "type": "object",
                      "properties": {
                        "path": {
                          "type": "string"
                        },
                        "type": {
                          "type": "string"
                        },
                        "predefined": {
                          "type": "string"
                        }
                      },
                      "required": []
                    },
                    {
                      "type": "object",
                      "properties": {
                        "path": {
                          "type": "string"
                        },
                        "type": {
                          "type": "string"
                        },
                        "predefined": {
                          "type": "string"
                        }
                      },
                      "required": []
                    },
                    {
                      "type": "object",
                      "properties": {
                        "path": {
                          "type": "string"
                        },
                        "type": {
                          "type": "string"
                        },
                        "predefined": {
                          "type": "string"
                        },
                        "value": {
                          "type": "string"
                        },
                        "minOccurrence": {
                          "type": "integer"
                        },
                        "maxOccurrence": {
                          "type": "integer"
                        },
                        "regexType": {
                          "type": "string"
                        }
                      },
                      "required": []
                    }
                  ]
                },
                "queryParameters": {
                  "type": "array",
                  "items": [
                    {
                      "type": "object",
                      "properties": {
                        "key": {
                          "type": "string"
                        },
                        "type": {
                          "type": "string"
                        },
                        "value": {
                          "type": "string"
                        }
                      },
                      "required": []
                    }
                  ]
                },
                "cookies": {
                  "type": "array",
                  "items": [
                    {
                      "type": "object",
                      "properties": {
                        "key": {
                          "type": "string"
                        },
                        "regex": {
                          "type": "string"
                        },
                        "predefined": {
                          "type": "string"
                        },
                        "command": {
                          "type": "string"
                        },
                        "type": {
                          "type": "string"
                        }
                      },
                      "required": [
                        "key",
                        "regex",
                        "predefined",
                        "command",
                        "type"
                      ]
                    }
                  ]
                },
                "multipart": {
                  "type": "object",
                  "properties": {
                    "params": {
                      "type": "array",
                      "items": [
                        {
                          "type": "object",
                          "properties": {
                            "key": {
                              "type": "string"
                            },
                            "regex": {
                              "type": "string"
                            },
                            "predefined": {
                              "type": "string"
                            },
                            "command": {
                              "type": "string"
                            },
                            "type": {
                              "type": "string"
                            }
                          },
                          "required": []
                        }
                      ]
                    },
                    "named": {
                      "type": "array",
                      "items": [
                        {
                          "type": "object",
                          "properties": {
                            "paramName": {
                              "type": "string"
                            },
                            "fileName": {
                              "type": "object",
                              "properties": {
                                "regex": {
                                  "type": "string"
                                },
                                "perfefined": {
                                  "type": "string"
                                }
                              },
                              "required": []
                            },
                            "fileContent": {
                              "type": "object",
                              "properties": {
                                "regex": {
                                  "type": "string"
                                },
                                "perfefined": {
                                  "type": "string"
                                }
                              },
                              "required": [ ]
                            },
                            "contentType": {
                              "type": "object",
                              "properties": {
                                "regex": {
                                  "type": "string"
                                },
                                "perfefined": {
                                  "type": "string"
                                }
                              },
                              "required": [ ]
                            }
                          },
                          "required": []
                        }
                      ]
                    }
                  },
                  "required": [
                    "params",
                    "named"
                  ]
                }
              },
              "required": []
            }
          },
          "required": []
        }
      ]
    }
  },
  "required": [
    "x-contracts"
  ]
}
```

#### Response Object Extension
```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "x-contracts": {
      "type": "array",
      "items": [
        {
          "type": "object",
          "properties": {
            "contractId": {
              "type": "integer"
            },
            "headers": {
              "type": "object",
              "properties": {
                "HeaderKey": {
                  "type": "string"
                }
              },
              "required": [
                "HeaderKey"
              ]
            },
            "body": {
              "type": "object"
            },
            "cookies": {
              "type": "object",
              "properties": {
                "key": {
                  "type": "string"
                }
              },
              "required": [
                "key"
              ]
            },
            "assyc": {
              "type": "boolean"
            },
            "fixedDelayMilliseconds": {
              "type": "integer"
            },
            "matchers": {
              "type": "object",
              "properties": {
                "headers": {
                  "type": "array",
                  "items": [
                    {
                      "type": "object",
                      "properties": {
                        "key": {
                          "type": "string"
                        },
                        "regex": {
                          "type": "string"
                        },
                        "command": {
                          "type": "string"
                        },
                        "predefined": {
                          "type": "string"
                        },
                        "regexType": {
                          "type": "string"
                        }
                      },
                      "required": []
                    }
                  ]
                },
                "body": {
                  "type": "array",
                  "items": [
                    {
                      "type": "object",
                      "properties": {
                        "path": {
                          "type": "string"
                        },
                        "type": {
                          "type": "string"
                        },
                        "predefined": {
                          "type": "string"
                        },
                        "value": {
                          "type": "string"
                        },
                        "minOccurrence": {
                          "type": "integer"
                        },
                        "maxOccurrence": {
                          "type": "integer"
                        },
                        "regexType": {
                          "type": "string"
                        }
                      },
                      "required": []
                    }
                  ]
                },
                "cookies": {
                  "type": "object",
                  "properties": {
                    "key": {
                      "type": "string"
                    },
                    "regex": {
                      "type": "string"
                    },
                    "command": {
                      "type": "string"
                    },
                    "predefined": {
                      "type": "string"
                    },
                    "regexType": {
                      "type": "string"
                    }
                  },
                  "required": []
                }
              },
              "required": []
            }
          },
          "required": [
            "contractId"]
        }
      ]
    }
  },
  "required": [
    "x-contracts"
  ]
}
```

### Example Contract Definition

Consider the following example:

```yaml
openapi: 3.0.0
info:
    description: Spring Cloud Contract Verifier Http Server OA3 Sample
    version: "1.0.0"
    title: Fraud Service API
paths:
    /fraudcheck:
        put:
            summary: Perform Fraud Check
            x-contracts:
            - contractId: 1
              name: Should Mark Client as Fraud
              priority: 1
            - contractId: 2
              name: Should Not Mark Client as Fraud
            requestBody:
                content:
                    application/json:
                        schema:
                            type: object
                            properties:
                                "client.id":
                                    type: integer
                                loanAmount:
                                    type: integer
                x-contracts:
                - contractId: 1
                  body:
                      "client.id": 1234567890
                      loanAmount: 99999
                  matchers:
                      body:
                      - path: $.['client.id']
                        type: by_regex
                        value: "[0-9]{10}"
                - contractId: 2
                  body:
                      "client.id": 1234567890
                      loanAmount: 123.123
                  matchers:
                      body:
                      - path: $.['client.id']
                        type: by_regex
                        value: "[0-9]{10}"
            responses:
                '200':
                    description: created ok
                    content:
                        application/json:
                            schema:
                                type: object
                                properties:
                                    fraudCheckStatus:
                                        type: string
                                    "rejection.reason":
                                        type: string
                    x-contracts:
                    - contractId: 1
                      body:
                          fraudCheckStatus: "FRAUD"
                          "rejection.reason": "Amount too high"
                      headers:
                          Content-Type: application/json;charset=UTF-8
                    - contractId: 2
                      body:
                          fraudCheckStatus: "OK"
                          "rejection.reason": null
                      headers:
                          Content-Type: application/json;charset=UTF-8
                      matchers:
                          body:
                          - path: $.['rejection.reason']
                            type: by_command
                            value: assertThatRejectionReasonIsNull($it)
    /frauds:
        get:
            x-contracts:
            - contractId: 3
              name: should return all frauds - should count all frauds
            responses:
                '200':
                    description: okay
                    content:
                        application/json:
                            schema:
                                type: object
                                properties:
                                    count:
                                        type: integer
                    x-contracts:
                    - contractId: 3
                      body:
                          count: 200
    /drunks:
        get:
            x-contracts:
            - contractId: 6
              name: drunk frauds
            responses:
                '200':
                    description: okay
                    content:
                        application/json:
                            schema:
                                type: object
                                properties:
                                    count:
                                        type: integer
                    x-contracts:
                    - contractId: 6
                      body:
                          count: 100
```

#### Define Contract Headers

Two Contracts are defined in the Operation Object:

```yaml
        put:
            summary: Perform Fraud Check
            x-contracts:
            - contractId: 1
              name: Should Mark Client as Fraud
              priority: 1
            - contractId: 2
              name: Should Not Mark Client as Fraud
```

#### Define Expected Request for Contacts

In the Request Body Object, the details for the expected request for each contract are given:

```yaml
            requestBody:
                content:
                    application/json:
                        schema:
                            type: object
                            properties:
                                "client.id":
                                    type: integer
                                loanAmount:
                                    type: integer
                x-contracts:
                - contractId: 1
                  body:
                      "client.id": 1234567890
                      loanAmount: 99999
                  matchers:
                      body:
                      - path: $.['client.id']
                        type: by_regex
                        value: "[0-9]{10}"
                - contractId: 2
                  body:
                      "client.id": 1234567890
                      loanAmount: 123.123
                  matchers:
                      body:
                      - path: $.['client.id']
                        type: by_regex
                        value: "[0-9]{10}"
```

*Note:* Notice how `x-contracts` is a list, with two objects, each of which has a `contractId` property.
The `contractId` property is matched to the `contractId` property in other sections of the document.

#### Define Expected Responses for Each Contract

The expected response for each contract, is defined on the
https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#responseObject[Response Object].

In this example, two responses are defined for the HTTP status of 200.

```yaml
            responses:
                '200':
                    description: created ok
                    content:
                        application/json:
                            schema:
                                type: object
                                properties:
                                    fraudCheckStatus:
                                        type: string
                                    "rejection.reason":
                                        type: string
                    x-contracts:
                    - contractId: 1
                      body:
                          fraudCheckStatus: "FRAUD"
                          "rejection.reason": "Amount too high"
                      headers:
                          Content-Type: application/json;charset=UTF-8
                    - contractId: 2
                      body:
                          fraudCheckStatus: "OK"
                          "rejection.reason": null
                      headers:
                          Content-Type: application/json;charset=UTF-8
                      matchers:
                          body:
                          - path: $.['rejection.reason']
                            type: by_command
                            value: assertThatRejectionReasonIsNull($it)
```

### Advanced Example

Following is a more advanced example showing how to incorporate query parameters, cookies, header values, and
more detailed response properties.

```yaml
openapi: "3.0.0"
info:
  version: 1.0.0
  title: SCC
paths:
  /foo:
    put:
        x-contracts:
            - contractId: 1
              description: Some description
              name: some name
              priority: 8
              ignored: true
        parameters:
          - name: a
            in: query
            schema:
                type: string
            x-contracts:
                - contractId: 1
                  value: b
          - name: b
            in: query
            schema:
                type: string
            x-contracts:
                - contractId: 1
                  value: c
          - name: foo
            in: header
            schema:
                type: string
            x-contracts:
                - contractId: 1
                  value: bar
          - name: fooReq
            in: header
            schema:
                type: string
            x-contracts:
                - contractId: 1
                  value: baz
          - name: foo
            in: cookie
            schema:
              type: string
            x-contracts:
                - contractId: 1
                  value: bar
          - name: fooReq
            in: cookie
            schema:
              type: string
            x-contracts:
                - contractId: 1
                  value: baz
        requestBody:
            content:
                application/json:
                    schema:
                      properties:
                        foo:
                          type: string
            x-contracts:
                - contractId: 1
                  body:
                    foo: bar
                  matchers:
                      body:
                        - path: $.foo
                          type: by_regex
                          value: bar
                      headers:
                        - key: foo
                          regex: bar
        responses:
            '200':
                description: the response
                content:
                    application/json:
                        schema:
                            properties:
                              foo:
                                type: string
                x-contracts:
                    - contractId: 1
                      headers:
                        foo2: bar
                        foo3: foo33
                        fooRes: baz
                      body:
                        foo2: bar
                        foo3: baz
                        nullValue: null
                      matchers:
                        body:
                          - path: $.foo2
                            type: by_regex
                            value: bar
                          - path: $.foo3
                            type: by_command
                            value: executeMe($it)
                          - path: $.nullValue
                            type: by_null
                            value: null
                        headers:
                          - key: foo2
                            regex: bar
                          - key: foo3
                            command: andMeToo($it)
                        cookies:
                          - key: foo2
                            regex: bar
                          - key: foo3
                            predefined:
```

### OA3 YAML Syntax
The [YAML DSL for Spring Cloud Contract](https://cloud.spring.io/spring-cloud-contract/multi/multi__contract_dsl.html#contract-matchers) defines a number of advanced features (regx, matchers, json path, etc).
These features should work with the OA3 DSL by using the same YAML syntax.

# License

The Spring Cloud Contract OpenAPI 3.0 Contract Converter is released under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
