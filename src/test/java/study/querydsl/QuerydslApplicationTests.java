package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

	@Autowired
	EntityManager em;

	@Test

	void contextLoads() {

		testEntity testEntity = new testEntity();
		em.persist(testEntity);

		JPAQueryFactory query = new JPAQueryFactory(em);
		QtestEntity qtest = QtestEntity.testEntity;

		testEntity result = query
				.selectFrom(qtest)
				.fetchOne();

		assertThat(result).isEqualTo(testEntity);
		assertThat(result.getId()).isEqualTo(testEntity.getId());

	}

}
