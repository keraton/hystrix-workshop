package ws.ns.hystrix.commands

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ws.ns.hystrix.service.RandomService
import ws.ns.hystrix.data.RandomDomain

/**
 * Created by apimentel on 3/29/16.
 */
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
