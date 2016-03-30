package ws.ns.hystrix.data

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

/**
 * Created by apimentel on 3/29/16.
 */
@Entity
class RandomDomain {
  @Id
  Long id

  String randomString

  Long randomNumber
}
