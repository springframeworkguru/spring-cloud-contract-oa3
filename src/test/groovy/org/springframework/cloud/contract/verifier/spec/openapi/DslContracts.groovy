package org.springframework.cloud.contract.verifier.spec.openapi

import org.springframework.cloud.contract.spec.Contract

/**
 * Created by jt on 5/30/18.
 */
class DslContracts {
    static Contract shouldMarkClientAsFraud(){
        return Contract.make {
            name = "1 - Should Mark Client As Fraud"
            request {
                method 'PUT'
                url '/fraudcheck'
                body([
                       "client.id": $(regex('[0-9]{10}')),
                       loanAmount: 99999
                ])
                headers {
                    contentType('application/json')
                }
            }
            response {
                status OK()
                body([
                       fraudCheckStatus: "FRAUD",
                       "rejection.reason": "Amount too high"
                ])
                headers {
                    contentType('application/json')
                }
            }
        }
    }

    static Contract shouldMarkClientasNotFraud() {
        return Contract.make {
            request {
                method 'PUT'
                url '/fraudcheck'
                body("""
						{
						"client.id":"${value(consumer(regex('[0-9]{10}')), producer('1234567890'))}",
						"loanAmount":123.123
						}
					"""
                )
                headers {
                    contentType("application/json")
                }

            }
            response {
                status OK()
                body(
                        fraudCheckStatus: "OK",
                        "rejection.reason": $(consumer(null), producer(execute('assertThatRejectionReasonIsNull($it)')))
                )
                headers {
                    contentType("application/json")
                }
            }

        }
    }

    static shouldCountAllFrauds(){
        return Contract.make {
            request {
                name "should count all frauds"
                method GET()
                url '/frauds'
            }
            response {
                status OK()
                body([
                        count: 200
                ])
                headers {
                    contentType("application/json")
                }
            }
        }
    }

    static shouldCountAllDrunks() {
        Contract.make {
            request {
                method GET()
                url '/drunks'
            }
            response {
                status OK()
                body([
                        count: 100
                ])
                headers {
                    contentType("application/json")
                }
            }
        }
    }

    static shouldReturnACookie(){
        Contract.make {
            request {
                method GET()
                url '/frauds/name'
                cookies {
                    cookie("name", "foo")
                    cookie(name2: "bar")
                }
            }
            response {
                status 200
                body("foo bar")
            }
        }
    }

    static shouldReturnAFraudForTheName() {
        Contract.make {
            // highest priority
            priority(1)
            request {
                method PUT()
                url '/frauds/name'
                body([
                        name: "fraud"
                ])
                headers {
                    contentType("application/json")
                }
            }
            response {
                status OK()
                body([
                        result: "Sorry ${fromRequest().body('$.name')} but you're a fraud"
                ])
                headers {
                    header(contentType(), "${fromRequest().header(contentType())};charset=UTF-8")
                }
            }
        }
    }

    static shouldReturnNonFraudForTheName() {
        Contract.make {
            request {
                method PUT()
                url '/frauds/name'
                body([
                        name: $(anyAlphaUnicode())
                ])
                headers {
                    contentType("application/json")
                }
            }
            response {
                status OK()
                body([
                        result: "Don't worry ${fromRequest().body('$.name')} you're not a fraud"
                ])
                headers {
                    header(contentType(), "${fromRequest().header(contentType())};charset=UTF-8")
                }
            }
        }
    }
}
