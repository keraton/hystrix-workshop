package ws.ns.hystrix

import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey

/**
 * Created by apimentel on 3/27/16.
 */
class CommandHelloWorld extends HystrixCommand<String> {

  private final String name;

  public CommandHelloWorld(String name) {
    super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
    this.name = name
  }

  @Override
  protected String run() {
    "Hello " + name + "!";
  }
}