package ws.ns.hystrix.commands

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ws.ns.hystrix.service.RandomService
import ws.ns.hystrix.data.RandomDomain

/**
 * Created by apimentel on 3/30/16.
 */
@Component
class RandomDomainSelectCommand {
  @Autowired
  RandomService randomService
  @HystrixCommand(commandKey ="selectDomain" ,fallbackMethod = "fallback")
  public String get(Long id) {
    randomService.getString(id)
  }

  public String fallback(Long id) {
    return new RandomDomain()
  }
}
