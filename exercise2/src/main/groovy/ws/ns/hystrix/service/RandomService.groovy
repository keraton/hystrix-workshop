package ws.ns.hystrix.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ws.ns.hystrix.data.RandomDomain
import ws.ns.hystrix.data.RandomRepository

import javax.transaction.Transactional

/**
 * Created by apimentel on 3/29/16.
 */
@Service
@Transactional
class RandomService {
  @Autowired
  RandomRepository randomRepository

  public RandomDomain generate(Long id){
    RandomDomain randomDomain = randomRepository.getOne(id)
    randomDomain.randomNumber = new Random().nextInt(1000)
    randomDomain.randomString = UUID.randomUUID().toString()
    return randomRepository.save(randomDomain)
  }

  public String getString(Long id){
    randomRepository.getOne(id)?.randomString
  }
}
