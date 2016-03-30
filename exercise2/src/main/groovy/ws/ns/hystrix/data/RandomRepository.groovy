package ws.ns.hystrix.data

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Created by apimentel on 3/29/16.
 */
@Repository
public interface RandomRepository extends JpaRepository<RandomDomain, Long> {
}
