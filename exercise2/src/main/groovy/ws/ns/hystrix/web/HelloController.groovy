package ws.ns.hystrix.web

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import ws.ns.hystrix.commands.RandomDomainSelectCommand
import ws.ns.hystrix.commands.RandomDomainUpdateCommand
import ws.ns.hystrix.data.RandomDomain

/**
 * Created by apimentel on 3/27/16.
 */
@Controller
public class HelloController {

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
}