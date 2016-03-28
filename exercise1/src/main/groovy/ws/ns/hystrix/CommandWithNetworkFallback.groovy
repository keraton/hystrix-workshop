package ws.ns.hystrix

import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey
import com.netflix.hystrix.HystrixCommandKey
import com.netflix.hystrix.HystrixThreadPoolKey
import redis.clients.jedis.Jedis

/**
 * Created by apimentel on 3/27/16.
 */
class CommandWithNetworkFallback extends HystrixCommand<String> {
  private final String name
  private final boolean fail

  public CommandWithNetworkFallback(String name, boolean fail) {
    super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("RemoteServiceX"))
      .andCommandKey(HystrixCommandKey.Factory.asKey("GetValueCommand")))
    this.name = name
    this.fail = fail
  }

  @Override
  protected String run() {
    if(fail){
      throw new RuntimeException("Failed")
    }
    String result = "Hello " + name + "!"
    new Jedis("localhost").set(name, result)
    result
  }

  @Override
  protected String getFallback() {
    new NetworkedFallback(name).execute()
  }

  private class NetworkedFallback extends HystrixCommand<String>{
    private final String name
    public NetworkedFallback(String name){
      super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("RemoteServiceX"))
        .andCommandKey(HystrixCommandKey.Factory.asKey("GetValueFallbackCommand"))
        .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("RemoteServiceXFallback")))
      this.name = name
    }
    @Override
    protected String run() throws Exception {
      Jedis jedis = new Jedis('localhost')
      jedis.get(name)
    }
  }
}
