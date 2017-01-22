# Hystrix Workshop

The purpose of this workshop is to review some of the features of Hystrix and implementation options.

The workshop is composed of 3 exercises, each one has the finished product in a different branch.

## Exercise 1 - Hystrix Basics

In the first exercise we will use the simplest approach to implement hystrix. We will create simple classes with that extend HystrixCommand and use a unit tests to verify it.

#### Setup

* Create an empty folder
* Init a maven project with the language of your preference
```
mvn -B archetype:generate \
  -DarchetypeGroupId=org.apache.maven.archetypes \
  -DgroupId=com.mycompany.app \
  -DartifactId=my-app
```
* Or you can use idea/eclipse/netbeans to create your maven project
* Add hystrix dependency
```
        <dependency>
            <groupId>com.netflix.hystrix</groupId>
            <artifactId>hystrix-core</artifactId>
            <version>1.5.1</version>
        </dependency>
```
* Add Junit dependency
```
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
            <scope>test</scope>
        </dependency>
```

* Test everything is going smooth 
```
mvn clean install
```

#### Hello World Command

This case show the basic construct that is used by Hystrix, the Hystrix Command. We need to override the run method with the business logic that we need to isolate.

* Add a CommandHelloWorld with the following code
```groovy
import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey

class CommandHelloWorld extends HystrixCommand<String> {

  private final String name;

  public CommandHelloWorld(String name) {
    super(HystrixCommandGroupKey.Factory.asKey('ExampleGroup'))
    this.name = name
  }

  @Override
  protected String run() {
    'Hello ' + name + '!'
  }
}
```
* Add a test case to verify the output is correct
```groovy

import spock.lang.Specification
import ws.ns.hystrix.CommandHelloWorld

class HystrixTest extends Specification {
  def 'should return hello world'() {
    setup:
      CommandHelloWorld commandHelloWorld = new CommandHelloWorld('Angel')
    when:
      String result = commandHelloWorld.execute()
    then:
      result == 'Hello Angel!'
  }
}
```

* Test it out

#### Command with fallback

One of the key concepts in hystrix is the fallback. The fallback is method that will be executed when and exception happens in the run method or the call reaches timeout.
To add this logic we will need to override the getFallback method.

* Add a CommandWithFallback class with the following code
```groovy
import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey

class CommandWithFallback extends HystrixCommand<String> {

  private final String name

  public CommandWithFallback(String name) {
    super(HystrixCommandGroupKey.Factory.asKey('ExampleGroup'))
    this.name = name
  }
  @Override
  protected String run() throws Exception {
    throw new RuntimeException('Expected exception')
  }

  @Override
  protected String getFallback() {
    return 'Hello '+name+'!'
  }
}
```
* Add a test case, the result should be the same but it comes from the fallback
```groovy
  def 'should use fallback'() {
    setup:
      CommandWithFallback commandWithFallback = new CommandWithFallback('Angel')
    when:
      String result = commandWithFallback.execute()
    then:
      result == 'Hello Angel!'
      commandWithFallback.isFailedExecution()
      commandWithFallback.isResponseFromFallback()
  }
```

* Test it out

#### Command with cache

Hystrix has built-in short-lived  request caching that allows de-duping of command executions for the same request.
To use this feature we need to override the getCacheKey method and make available a HystrixRequestContext which constains the state and manages request scope variables that share state among threads.

* Add a CommandWithCache class with the following code
```groovy

import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey

class CommandWithCache extends HystrixCommand<String> {

  private final String name

  public CommandWithCache(String name) {
    super(HystrixCommandGroupKey.Factory.asKey('ExampleGroup'))
    this.name = name
  }

  @Override
  protected String run() {
    'Hello ' + name + '!'
  }

  @Override
  protected String getCacheKey() {
    name
  }
}
```
* Add a test case, the result should be the same but second response should come from the cache
```groovy
  def 'should use cache'() {
    setup:
      CommandWithCache commandWithCacheA = new CommandWithCache('Angel')
      CommandWithCache commandWithCacheB = new CommandWithCache('Angel')
      HystrixRequestContext context = HystrixRequestContext.initializeContext()
    when:
      String resultA = commandWithCacheA.execute()
      String resultB = commandWithCacheB.execute()
    then:
      resultA == 'Hello Angel!'
      resultB == 'Hello Angel!'
      commandWithCacheA.isResponseFromCache() == false
      commandWithCacheB.isResponseFromCache() == true
    cleanup:
      context.shutdown()
  }
```
* Test it out

#### Command with remote fallback

In the implementation of the fall back method there are several patterns that should be used:
* Fail Fast: no fallback, the exception will propagate.
* Fail Silent: return null, an empty List and empty Map or other such responses.
* Fail Static: return a default static method (a constant).
* Fallback Stubbed: return a compound object with with stubbed values from headers, cookies, or defaults.
* Fallback Cache via Network: use a cache middleware to retrieve a stale version of the data. 

The last option requires care because the fallback dependes on an external resource and should be isolated in another command.
For this exercise we will implement the last one. In this case we will use redis as the external cache.

* Add the dependencies required for the external cache.
```
compile 'redis.clients:jedis:2.8.1'
```
* Add a CommandWithNetworkFallback class with the following code
```groovy
import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey
import com.netflix.hystrix.HystrixCommandKey
import com.netflix.hystrix.HystrixThreadPoolKey
import redis.clients.jedis.Jedis

class CommandWithNetworkFallback extends HystrixCommand<String> {
  private final String name
  private final boolean fail

  public CommandWithNetworkFallback(String name, boolean fail) {
    super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey('RemoteServiceX'))
      .andCommandKey(HystrixCommandKey.Factory.asKey('GetValueCommand')))
    this.name = name
    this.fail = fail
  }

  @Override
  protected String run() {
    if(fail){
      throw new RuntimeException('Failed')
    }
    String result = 'Hello ' + name + '!'
    new Jedis('localhost').set(name, result)
    result
  }

  @Override
  protected String getFallback() {
    new NetworkedFallback(name).execute()
  }

  private class NetworkedFallback extends HystrixCommand<String>{
    private final String name
    public NetworkedFallback(String name){
      super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey('RemoteServiceX'))
        .andCommandKey(HystrixCommandKey.Factory.asKey('GetValueFallbackCommand'))
        .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey('RemoteServiceXFallback')))
      this.name = name
    }
    @Override
    protected String run() throws Exception {
      new Jedis('localhost').get(name)
    }
  }
}
```
* Add a test case, the responses should be the same but one will come from redis
```groovy
  def "should use remote cache"(){
    setup:
      CommandWithNetworkFallback commandWithNetworkFallbackA = new CommandWithNetworkFallback("Angel", false)
      CommandWithNetworkFallback commandWithNetworkFallbackB = new CommandWithNetworkFallback("Angel", true)
    when:
      String resultA = commandWithNetworkFallbackA.execute()
      String resultB = commandWithNetworkFallbackB.execute()
    then:
      resultA == "Hello Angel!";
      resultB == "Hello Angel!";
  }
```

## Exercise 2 - Spring boot + Hystrix

In this exercise we will create create a simple spring boot web app and add hystrix support for the data base communication.
In the last exercise we created hystrix commands by hand, but there's another alternative using annotations. In this exercise we will use annotation approach.

#### Setup

* Create a new folder
* Init a gradle project with the language of your preference
```
gradle init --type groovy-library    
```
* Add the idea plugin in the build.gradle file it you will be using Idea
```
apply plugin: 'idea'
```
* Add dependencies, the build.gradle file should look like this
```groovy
buildscript {
    ext {
        springBootVersion = '1.3.3.RELEASE'
    }
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
    }
}
apply plugin: 'groovy'
apply plugin: 'idea'
apply plugin: 'spring-boot'

repositories {
    mavenCentral()
    maven { url "https://repo.spring.io/snapshot" }
    maven { url "https://repo.spring.io/milestone" }
    jcenter()
}

dependencies {
    compile 'org.codehaus.groovy:groovy-all:2.4.4'
    compile("org.springframework.boot:spring-boot-starter-web") {
        exclude module: "spring-boot-starter-tomcat"
    }
    compile("org.springframework.boot:spring-boot-starter-jetty")
    compile("org.springframework.boot:spring-boot-starter-actuator")
    compile("org.springframework.boot:spring-boot-starter-data-jpa")
    compile 'org.springframework.cloud:spring-cloud-starter-hystrix'
    compile('mysql:mysql-connector-java:5.1.6')
    testCompile 'org.spockframework:spock-core:1.0-groovy-2.4'
    testCompile 'junit:junit:4.12'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:Angel.SR6"
    }
}
```
* Test everything is going smooth (and download half internet)
```
gradle clean build
```

#### Create domain layer

* Create a Domain object with 3 fields Id, randomString, randomNumber
```groovy
package ws.ns.hystrix.data

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class RandomDomain {
  @Id
  Long id

  String randomString

  Long randomNumber
}
```

* Create a Repository for the domain class
```groovy
package ws.ns.hystrix.data

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
public interface RandomRepository extends JpaRepository<RandomDomain, Long> {
}
```

* Add spring boot main class and save a random domain on start
```groovy
package ws.ns.hystrix

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import ws.ns.hystrix.data.RandomDomain
import ws.ns.hystrix.data.RandomRepository

@SpringBootApplication
class Library {
    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(Library, args)
        RandomRepository randomRepository = ctx.getBean(RandomRepository)
        randomRepository.save(new RandomDomain(id: 1L, randomNumber: new Random().nextInt(1000), randomString: UUID.randomUUID().toString()))
    }
}
```
* Add jdbc configurations and management port to the application.yml
```yml
management.port: 8081
spring.jpa.generate-ddl: true
spring.jpa.hibernate.ddl-auto: create-drop
spring.datasource.url: jdbc:mysql://localhost/hystrix
spring.datasource.username: hystrix
spring.datasource.password: hystrix
spring.datasource.driver-class-name: com.mysql.jdbc.Driver
spring.datasource.testWhileIdle: true
spring.datasource.timeBetweenEvictionRunsMillis: 15000
spring.datasource.validationQuery: SELECT 1
```
* Run your add and verify everything is going smooth
```
gradle clean bootRun
```

#### Create a Service layer

* Add a service class that updates a record with new random values and gets the random string by id
```groovy
package ws.ns.hystrix.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ws.ns.hystrix.data.RandomDomain
import ws.ns.hystrix.data.RandomRepository
import javax.transaction.Transactional

@Service
@Transactional
class RandomService {
  @Autowired
  RandomRepository randomRepository

  public RandomDomain generate(Long id){
    RandomDomain randomDomain = randomRepository.getOne(id)
    randomDomain.randomNumber = new Random().nextInt(1000)
    randomDomain.randomString = UUID.randomUUID().toString()
    randomRepository.save(randomDomain)
  }

  public String getString(Long id){
    randomRepository.getOne(id)?.randomString
  }
}
```

#### Add http endpoints
* Add a controller with two endpoints one for each service layer
```groovy
package ws.ns.hystrix.web

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import ws.ns.hystrix.data.RandomDomain
import ws.ns.hystrix.service.RandomService

@Controller
public class HelloController {

  @Autowired
  RandomService randomService

  @RequestMapping(path = "hello", method = RequestMethod.GET)
  @ResponseBody
  public String get() {
    randomService.getString(1L)
  }
  @RequestMapping(path = "hello", method = RequestMethod.POST)
  @ResponseBody
  public RandomDomain post() {
    randomService.generate(1L)
  }
}
```

* Run your application and verify the endpoints generate the correct response http://localhost:8080/hello

#### Add hystrix support

As mentioned before we will be using javanica annotations to create the command for this exercise. 

* Create two classes one for each method of the service and implement the fallback method as you wish.
```groovy
package ws.ns.hystrix.commands

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ws.ns.hystrix.service.RandomService
import ws.ns.hystrix.data.RandomDomain

@Component
class RandomDomainSelectCommand {
  @Autowired
  RandomService randomService
  @HystrixCommand(commandKey ="selectDomain" ,fallbackMethod = "fallback")
  public String get(Long id) {
    randomService.getString(id)
  }

  public String fallback(Long id) {
    new RandomDomain()
  }
}
```

```groovy
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ws.ns.hystrix.service.RandomService
import ws.ns.hystrix.data.RandomDomain

@Component
class RandomDomainUpdateCommand {
  @Autowired
  RandomService randomService
  @HystrixCommand(commandKey = "updateDomain",fallbackMethod = "fallback")
  public RandomDomain generateDomain(Long id) {
    randomService.generate(id)
  }

  public RandomDomain fallback(Long id) {
    return new RandomDomain()
  }
}
```

* Change service calls with Command execution in the controller

```groovy
  @Autowired
  RandomDomainUpdateCommand randomRepositoryCommand
  @Autowired
  RandomDomainSelectCommand domainSelectCommand

  @RequestMapping(path = "hello", method = RequestMethod.GET)
  @ResponseBody
  public String get() {
    domainSelectCommand.get(1L)
  }
  @RequestMapping(path = "hello", method = RequestMethod.POST)
  @ResponseBody
  public RandomDomain post() {
    randomRepositoryCommand.generateDomain(1L)
  }
```

* Enable Hystrix on the main class
```groovy
@SpringBootApplication
@EnableCircuitBreaker
class Library {...}
```
* Run the app and verify the endpoints work as before

#### Add hystrix dashboard

* Add hystrix dashboard dependency
```
compile 'org.springframework.cloud:spring-cloud-starter-hystrix-dashboard'
```
* Enable the dasboard on the main class
```groovy
@SpringBootApplication
@EnableCircuitBreaker
@EnableHystrixDashboard
class Library {...}
```
* Run the application
```
gradle clean bootRun
```
* Test one of the endpoints http://localhost:8080/hello
* Verify that the stream of metrics is being generated on http://localhost:8081/hystrix.stream
* Enter to the hystrix dashboard http://localhost:8080/hystrix
* Monitor the stream generated by the current app

#### Let's add some load

Try to add some load to the endpoints and see how the dashboard behaves. We will be using wrk for this load test.
You can download it with brew install wrk

* To send POST request with wrk we need to add a little lua script like this:
```
wrk.method = "POST"
```
* start a 5 minute test with the following command 
```
wrk -t10 -d360s -c10 -spost.lua  http://localhost:8080/hello
```
* start another test for the GET endpoint
```
wrk -t10 -d360s -c10  http://localhost:8080/hello
```
* See how the dashboard changes in values and colors.
* Try taking down your database in the middle of the test.
* Try bringing it back and see what happens.

## Exercise 3 - Turbine

With exercise #2 we monitor the execution of the commands in a our app, but it has the limition of only monitor the stream generated by one app.
To monitor more than one app we need an aggregator of metrics, for hystrix it's called Turbine. We will use the previous example
and aggregate the stream of metrics into a turbine app through a AMQP server.

#### Create turbine app

* Setup and account on https://www.cloudamqp.com/
* Create a new gradle project in a new folder
```
gradle init --type groovy-library    
```
* Add the dependencies for turbine
```groovy
buildscript {
    ext {
        springBootVersion = '1.3.3.RELEASE'
    }
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
    }
}
apply plugin: 'groovy'
apply plugin: 'idea'
apply plugin: 'spring-boot'

repositories {
    mavenCentral()
    maven { url "https://repo.spring.io/snapshot" }
    maven { url "https://repo.spring.io/milestone" }
    jcenter()

}
dependencies {
    compile 'org.codehaus.groovy:groovy-all:2.4.4'
    compile("org.springframework.cloud:spring-cloud-starter-turbine-amqp")
    testCompile 'org.spockframework:spock-core:1.0-groovy-2.4'
    testCompile 'junit:junit:4.12'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:Angel.SR6"
    }
}
```
* Create spring main class
```groovy
package ws.ns.hystrix

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.turbine.amqp.EnableTurbineAmqp
import org.springframework.context.ApplicationContext
@SpringBootApplication
@EnableTurbineAmqp
class Library {
    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(Library, args)
    }
}
```
* Add amqp configuration specific to your cloudamqp account to application.yml
```yml
spring.rabbitmq.host: fox.rmq.cloudamqp.com
spring.rabbitmq.virtual-host: ***
spring.rabbitmq.port: 5672
spring.rabbitmq.username: ***
spring.rabbitmq.password: ***
```
* Run the app
```
gradle clean bootRun
```

#### Add support for amqp on the existing web app

* Add hystrix amqp dependency to exercise2
```
compile 'org.springframework.cloud:spring-cloud-netflix-hystrix-amqp'
```
* Setup the application name in the file bootstrap.yml
```yml
spring:
  application:
    name: Angel
```
* Add amqp configuration to application.yml
```yml
spring.rabbitmq.host: fox.rmq.cloudamqp.com
spring.rabbitmq.virtual-host: ***
spring.rabbitmq.port: 5672
spring.rabbitmq.username: ***
spring.rabbitmq.password: ***
```
* Run the app and test the endpoints
* Verify that the stream of metrics is being generated on http://localhost:8989/
* Enter to the hystrix dashboard http://localhost:8080/hystrix
* Monitor the stream generated by the current app

#### Let's gather all the data together

* Change the configuration of the web app to have the values from AMQP
 ```yml
spring.rabbitmq.host: fox.rmq.cloudamqp.com
spring.rabbitmq.virtual-host: qghlygtg
spring.rabbitmq.port: 5672
spring.rabbitmq.username: qghlygtg
spring.rabbitmq.password: F_VLCcr8BPMmVZRLJGu_H3QCsOX1_bSr

 ```
* Run your app and add some load.
* Watch all the data been showed in the dashboard.
