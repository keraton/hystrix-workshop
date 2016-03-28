package ws.ns.hystrix

import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey

/**
 * Created by apimentel on 3/27/16.
 */
class CommandWithFallback extends HystrixCommand<String> {

  private final String name

  public CommandWithFallback(String name) {
    super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
    this.name = name
  }
  @Override
  protected String run() throws Exception {
    throw new RuntimeException("Expected exception")
  }

  @Override
  protected String getFallback() {
    return "Hello "+name+"!"
  }
}
