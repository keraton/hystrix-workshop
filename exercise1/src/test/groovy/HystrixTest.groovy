/*
 * This Spock specification was auto generated by running 'gradle init --type groovy-library'
 * by 'apimentel' at '3/27/16 12:43 AM' with Gradle 2.7
 *
 * @author apimentel, @date 3/27/16 12:43 AM
 */


import spock.lang.Specification
import ws.ns.hystrix.CommandHelloWorld

class HystrixTest extends Specification {
  def "should return hello world"() {
    setup:
      CommandHelloWorld commandHelloWorld = new CommandHelloWorld("Angel")
    when:
      String result = commandHelloWorld.execute();
    then:
      result == "Hello Angel!"
  }

}