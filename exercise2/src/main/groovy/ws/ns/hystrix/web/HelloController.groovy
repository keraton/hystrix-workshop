package ws.ns.hystrix.web

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import ws.ns.hystrix.data.RandomDomain
import ws.ns.hystrix.service.RandomService

/**
 * Created by apimentel on 3/27/16.
 */
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