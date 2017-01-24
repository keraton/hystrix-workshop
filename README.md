# Hystrix Workshop

The purpose of this workshop is to review some of the features of Hystrix and implementation options.

The workshop is composed of 3 exercises, each one has the finished product in a different branch.

## Exercise 1 - Hystrix Basics

In the first exercise we will use the simplest approach to implement hystrix. We will create simple classes with that extend HystrixCommand and use a unit tests to verify it.

#### Setup

* Create an empty folder
* Init a maven project with IDE of your preference
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
* Add AssertJ dependency
```
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>1.2.0</version>
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
```java
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

public class CommandHelloWorld extends HystrixCommand<String> {

    private final String name;

    public CommandHelloWorld(String name) {
        super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
        this.name = name;
    }

    @Override
    protected String run() {
        return "Hello " + name + "!";
    }
}
```
* Add a test case to verify the output is correct
```java
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandHelloWorldTest {

    @Test
    public void should () {
        // Given
        CommandHelloWorld commandHelloWorld = new CommandHelloWorld("Angel");

        // When
        String result = commandHelloWorld.execute();

        // Then
        assertThat(result).isEqualTo("Hello Angel!");
    }

}
```
* Test it out

#### Command with fallback

One of the key concepts in hystrix is the fallback. The fallback is method that will be executed when and exception happens in the run method or the call reaches timeout.
To add this logic we will need to override the getFallback method.

* Add a CommandWithFallback class with the following code
```java
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

public class CommandWithFallback extends HystrixCommand<String> {

    private final String name;

    public CommandWithFallback (String name) {
        super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
        this.name = name;
    }

    @Override
    protected String run() throws Exception {
        throw new RuntimeException("Expected Exception");
    }

    @Override
    protected String getFallback() {
        return "Hello " + name + "!";
    }
}
```
* Add a test case, the result should be the same but it comes from the fallback
```java
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandWithFallbackTest {

    @Test
    public void should () {
        // Given
        CommandWithFallback commandWithFallback = new CommandWithFallback("Angel");

        // When
        String result = commandWithFallback.execute();

        // Then
        assertThat(result).isEqualTo("Hello Angel!");
        assertThat(commandWithFallback.isFailedExecution());
        assertThat(commandWithFallback.isResponseFromFallback());
    }

}
```

* Test it out

#### Command with cache

Hystrix has built-in short-lived  request caching that allows de-duping of command executions for the same request.
To use this feature we need to override the getCacheKey method and make available a HystrixRequestContext which constains the state and manages request scope variables that share state among threads.

* Add a CommandWithCache class with the following code
```java

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

public class CommandWithCache extends HystrixCommand<String> {

    private final String name;

    public CommandWithCache(String name) {
        super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
        this.name = name;
    }

    @Override
    protected String run() throws Exception {
        return "Hello " + name + "!";
    }

    @Override
    protected String getCacheKey() {
        return name;
    }
}

```
* Add a test case, the result should be the same but second response should come from the cache
```java
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandWithCacheTest {

    @Test
    public void should () {
        // Given
        CommandWithCache commandWithCache1 = new CommandWithCache("Angel");
        CommandWithCache commandWithCache2 = new CommandWithCache("Angel");

        // When
        HystrixRequestContext.initializeContext();

        String commandWithCache1Result = commandWithCache1.execute();
        String commandWithCache2Result = commandWithCache2.execute();

        // Then
        assertThat(commandWithCache1Result).isEqualTo("Hello Angel!");
        assertThat(commandWithCache2Result).isEqualTo("Hello Angel!");
        assertThat(commandWithCache1.isResponseFromCache()).isFalse();
        assertThat(commandWithCache2.isResponseFromCache()).isTrue();
    }

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

* Install Redis client in local

See in the redis.io
https://redis.io/

* Add the dependencies required for the external cache.
```
        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
            <version>2.8.1</version>
        </dependency>
```
* Add a CommandWithNetworkFallback class with the following code
```java
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

public class CommandWithNetworkFallback extends HystrixCommand<String> {
    private final String name;
    private final boolean fail;

    public CommandWithNetworkFallback(String name, boolean fail) {
        super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("RemoteServiceX"))
                .andCommandKey(HystrixCommandKey.Factory.asKey("GetValueCommand")));
        this.name = name;
        this.fail = fail;
    }

    @Override
    protected String run() {
        if(fail){
            throw new RuntimeException("Failed");
        }
        String result = "Hello " + name + "!";
        new Jedis("localhost").set(name, result);
        return result;
    }

    @Override
    protected String getFallback() {
        return new NetworkedFallback(name).execute();
    }

    private class NetworkedFallback extends HystrixCommand<String>{
        private final String name;
        public NetworkedFallback(String name){
            super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("RemoteServiceX"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("GetValueFallbackCommand"))
                    .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("RemoteServiceXFallback")));
            this.name = name;
        }
        @Override
        protected String run() throws Exception {
            return new Jedis("localhost").get(name);
        }
    }
}
```
* Add a test case, the responses should be the same but one will come from redis
```java
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandWithNetworkFallbackTest {

    @Test
    public void should () {
        // Given
        CommandWithNetworkFallback commandWithNetworkFallbackA = new CommandWithNetworkFallback("Angel", false);
        CommandWithNetworkFallback commandWithNetworkFallbackB = new CommandWithNetworkFallback("Angel", true);

        // When
        String resultA = commandWithNetworkFallbackA.execute();
        String resultB = commandWithNetworkFallbackB.execute();

        // Then
        assertThat(resultA).isEqualTo("Hello Angel!");
        assertThat(resultB).isEqualTo("Hello Angel!");

    }

}
```

## Exercise 2 - Spring boot + Hystrix

In this exercise we will create create three simple spring boot web app and add hystrix support for the communication.
In the last exercise we created hystrix commands by hand, but there's another alternative using annotations. In this exercise we will use annotation approach.

#### Test scenario

Two servers have producer - consumer relationship between of them. And one server act as a hystrix dashboard

#### Producer server

We will create a RandomServer which return a random value to its client

* Create a new folder
* Init a maven project with IDE of your preference
* Add dependencies, the pom.xml file should look like this
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.keratonjava</groupId>
    <artifactId>hystrix-spring-boot-tutorial</artifactId>
    <version>1.0-SNAPSHOT</version>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>1.4.3.RELEASE</version>
    </parent>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>Camden.SR4</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

    </dependencies>

    <properties>
        <java.version>1.8</java.version>
    </properties>


    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

    
</project>

```
* Test everything is going smooth (and download half internet)
```
mvn clean install
```

#### Create service and controller

* Create a service and controller that return a simple random value
```java
package com.keratonjava.random;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@SpringBootApplication
public class RandomApplication  {

    @RequestMapping("/random")
    public String get() {
        return UUID.randomUUID().toString();
    }

    public static void main(String[] args) {
        SpringApplication.run(RandomApplication.class, args);
    }

}
```

* Run it with IDEA or using maven command
```sh
mvn spring-boot:run
```

* Test it in http://localhost:8080/random


#### Create a Consumer Server

We will create a calculator that use the random value from the Random server.

* Create a new folder
* Init a maven project with IDE of your preference
* Add dependencies, the pom.xml file should look like this
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.keratonjava.tuto</groupId>
    <artifactId>hystrix-spring-boot-tutorial-client</artifactId>
    <version>1.0-SNAPSHOT</version>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>1.4.3.RELEASE</version>
    </parent>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>Camden.SR4</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-hystrix</artifactId>
        </dependency>

    </dependencies>

    <properties>
        <java.version>1.8</java.version>
    </properties>


    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

#### Create the Calculator service
* Add a service that use the random value
```java
package com.keratonjava.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Service
public class CalculatorService {

    private final RestTemplate restTemplate;

    @Autowired
    public CalculatorService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String calculateRandom() {
        URI uri = URI.create("http://localhost:8080/random");
        return this.restTemplate.getForObject(uri, String.class);
    }

}
```

#### Create the Calculator Controller
* Add the calculate end point that use the Calculator service

```java
package com.keratonjava.client;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CalculatorController {

    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @RequestMapping("calculate")
    public String calculateRandom() {
        return calculatorService.calculateRandom();
    }
}

```

#### Create the Calculator Application
* Add spring-boot application main class
```java
package com.keratonjava.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@EnableHystrix
@SpringBootApplication
public class CalculatorApplication {

    @Bean
    public RestTemplate rest(RestTemplateBuilder builder) {
        return builder.build();
    }

    public static void main(String[] args) {
        SpringApplication.run(CalculatorApplication.class, args);
    }

}


```
* Add an application.properties

```properties
server.port=8090
```

* Run your application and verify the endpoints generate the correct response http://localhost:8090/calculate
* You will see that the calculate will return exactly the same value from the RandomServer

#### Add hystrix support

As mentioned before we will be using javanica annotations to create the command for this exercise. 

* In the CalculatorService add the HystrixCommand Annotation
```java
package com.keratonjava.client;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Service
public class CalculatorService {

    private final RestTemplate restTemplate;

    @Autowired
    public CalculatorService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @HystrixCommand(fallbackMethod = "fakeRandom")
    public String calculateRandom() {
        URI uri = URI.create("http://localhost:8080/random");
        return this.restTemplate.getForObject(uri, String.class);
    }

    public String fakeRandom() {
        return "fake-2e8d-4565-a4a7-9cb5bb8a4c42";
    }
}

```
* In the Spring-boot main class, add @EnableHystrix annotation
```java
package com.keratonjava.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@EnableHystrix
@SpringBootApplication
public class CalculatorApplication {

    @Bean
    public RestTemplate rest(RestTemplateBuilder builder) {
        return builder.build();
    }

    public static void main(String[] args) {
        SpringApplication.run(CalculatorApplication.class, args);
    }

}

```
* Run the app and verify the endpoints work as before
* Try to stop the RandomService to see what happen

#### Add hystrix dashboard

* Create a new spring-boot project
* Add a pom.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.keratonjava</groupId>
    <artifactId>hystrix-spring-boot-tutorial-dashboard</artifactId>
    <version>1.0-SNAPSHOT</version>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>1.4.3.RELEASE</version>
    </parent>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>Camden.SR4</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-hystrix-dashboard</artifactId>
        </dependency>

    </dependencies>

    <properties>
        <java.version>1.8</java.version>
    </properties>


    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>

```
* Enable the dasboard on the main class
```java
package com.keratonjava.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;

@SpringBootApplication
@EnableHystrixDashboard
public class DashboardApplication {

    public static void main(String...args) {
        SpringApplication.run(DashboardApplication.class, args);
    }
}

```
* Add an application.properties
```
server.port=8070
```
* Test one of the endpoints http://localhost:8090/calculate
* Verify that the stream of metrics is being generated on http://localhost:8090/hystrix.stream
* Enter to the hystrix dashboard http://localhost:8070/hystrix
* Monitor the stream generated by the current app

#### Let's add some load

Try to add some load to the endpoints and see how the dashboard behaves. We will be using wrk for this load test.
You can download it with brew install wrk


* start a 5 minute test with the following command 
```
wrk -t10 -d360s -c10  http://localhost:8090/calculate
```
* See how the dashboard changes in values and colors.
* Try taking down your Random application in the middle of the test.
* Try bringing it back and see what happens.

## Exercise 3 - Turbine

With exercise #2 we monitor the execution of the commands in a our app, but it has the limition of only monitor the stream generated by one app.
To monitor more than one app we need an aggregator of metrics, for hystrix it's called Turbine. We will use the previous example
and aggregate the stream of metrics into a turbine app through a AMQP server.

#### Create turbine app

* Setup and account on https://www.cloudamqp.com/
* Create a new maven project in a new folder
* Add the dependencies for turbine
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.keratonjava</groupId>
    <artifactId>hystrix-spring-boot-tutorial-turbine</artifactId>
    <version>1.0-SNAPSHOT</version>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>1.4.3.RELEASE</version>
    </parent>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>Camden.SR4</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-turbine-stream</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
        </dependency>

    </dependencies>

    <properties>
        <java.version>1.8</java.version>
    </properties>


    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```
* Create spring main class

```java
package com.keratonjava.turbine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.turbine.stream.EnableTurbineStream;

@SpringBootApplication
@EnableTurbineStream
public class TurbineApplication {

    public static void main(String... args) {
        SpringApplication.run(TurbineApplication.class, args);
    }
}

```
* Add amqp configuration specific to your cloudamqp account to application.properties
```properties
server.port=8060
spring.rabbitmq.host=cat.rmq.cloudamqp.com
spring.rabbitmq.virtual-host=***
spring.rabbitmq.port=5672
spring.rabbitmq.username=***
spring.rabbitmq.password=***
```
* Run the app


#### Add support for stream on the existing web app

* Add hystrix stream dependency to CalculatorServer
```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-netflix-hystrix-amqp</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
        </dependency>
```
* Setup the application name in the file bootstrap.yml
```xml
spring:
  application:
    name: Your_name
```
* Add amqp configuration to application.properties
```properties
server.port=8060
spring.rabbitmq.host=cat.rmq.cloudamqp.com
spring.rabbitmq.virtual-host=***
spring.rabbitmq.port=5672
spring.rabbitmq.username=***
spring.rabbitmq.password=***
```
* Run the app and test the endpoints
* Verify that the stream of metrics is being generated on http://localhost:8060/
* Enter to the hystrix dashboard http://localhost:8070/hystrix
* Monitor the stream generated by the current app

#### Let's gather all the data together

* Change the configuration of the web app to have the values from AMQP
 ```properties
server.port=8060
spring.rabbitmq.host=cat.rmq.cloudamqp.com
spring.rabbitmq.virtual-host=jbgfqbkf
spring.rabbitmq.port=5672
spring.rabbitmq.username=jbgfqbkf
spring.rabbitmq.password=sJuGRIlADtaDIcMrSIBbEdemo8w7e9lR

 ```
* Run your app and add some load.
* Watch all the data been showed in the dashboard.
