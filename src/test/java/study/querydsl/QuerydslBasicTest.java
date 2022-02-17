package study.querydsl;


import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;



    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1",10,teamA);
        Member member2 = new Member("member2",20,teamA);

        Member member3 = new Member("member3",30,teamB);
        Member member4 = new Member("member4",40,teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() throws Exception {
        //given
        //member1을 찾자
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDSL() throws Exception {
        //given
        //when
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() throws Exception {
        //given
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();
        //when
        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
        //then
    }
    @Test
    public void searchAndParam() throws Exception {
        //given
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.in(10,20)
                )
                .fetchOne();
        //when
        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
        //then
    }

    @Test
    public void resultFetchTest() throws Exception {
        //given
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member member1 = queryFactory
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        QueryResults<Member> memberQueryResults = queryFactory
                .selectFrom(member)
                .fetchResults();
        memberQueryResults.getTotal();
        List<Member> content = memberQueryResults.getResults();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     *  회원 정렬 순서
     *  1. 회원 나이 내림차순
     *  2. 회원 이름 올림차순
     *  3단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     * @throws Exception
     */
    @Test
    public void 정렬() throws Exception {
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    @Test
    public void 페이징() throws Exception {

        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }
    @Test
    public void 전체페이징() throws Exception {

        QueryResults<Member> QueryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        assertThat(QueryResults.getTotal()).isEqualTo(4);
        assertThat(QueryResults.getLimit()).isEqualTo(2);
        assertThat(QueryResults.getOffset()).isEqualTo(1);
        assertThat(QueryResults.getResults().size()).isEqualTo(2);

    }

    @Test
    public void 집합() throws Exception {
        List<Tuple> findMembers = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();
        Tuple tuple = findMembers.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     * @throws Exception
     */

    @Test
    public void 그룹() throws Exception {
        //given
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamA.get(team.name)).isEqualTo("teamB");
        assertThat(teamA.get(member.age.avg())).isEqualTo(35);
    }


    /**
     * 팀 A에 소속된 모든 회원
     * @throws Exception
     */
    @Test
    public void join() throws Exception {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("member1","member2");
    }

    /**
     * 세타 조인
     * 연관관계 없는 필드로 조인
     * 모든 필드를 묶어서 검사
     * 회원 이름이 팀이름과 같은 회원 조회
     * @throws Exception
     */
    @Test
    public void theta_join() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        //when
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL : select m,t from Member m left join m.team t on t.name = 'teamA'
     * @throws Exception
     */
    @Test
    public void 조인_온_filtering() throws Exception {
        //given
        List<Tuple> reuslt = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team)
//                .join(member.team, team).on(team.name.eq("teamA"))
                .where(team.name.eq("teamA")) //필터링  이너일때는 on과 동일
                .fetch();
        for (Tuple tuple : reuslt) {
            System.out.println("tuple = " + tuple);
        }
    }


    /***
     * 연관관계 없는 엔티티 외부 조인
     * 회원의 이름이 팀이름과 같은 대상 외부 조인
     * @throws Exception
     */
    @Test
    public void join_on_no_relation() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        //when
        List<Tuple> tuple = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
//                .where(member.username.eq(team.name))
                .fetch();
        //then
        for (Tuple tuple1 : tuple) {
            System.out.println("tuple1 = " + tuple1);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetch_join_no() throws Exception {
        //given
        em.flush();
        em.clear();
        //when
        Member member1 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
        //then
    }

    @Test
    public void fetch_join() throws Exception {
        //given
        em.flush();
        em.clear();
        //when
        Member member1 = queryFactory
                .selectFrom(member)
                .join(member.team,team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());
        //then
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     * @throws Exception
     */
    @Test
    public void subQuery() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        //when
        assertThat(result).extracting("age")
                .containsExactly(40);

        //then
    }


    /**
     * 나이가 가장 평균 이상인 회원 조회
     * @throws Exception
     */
    @Test
    public void subQueryGoe() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        //when
        assertThat(result).extracting("age")
                .containsExactly(30,40);

        //then
    }
    /**
     * 나이가 10살 초과
     * @throws Exception
     */
    @Test
    public void subQueryIn() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();
        //when
        assertThat(result).extracting("age")
                .containsExactly(20,30,40);
        //then
    }


    @Test
    public void selectSubQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");
        //given
        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase() throws Exception {
        //given
        List<String> fetch = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() throws Exception {
        //given
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() throws Exception {
        //given
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() throws Exception {
        //given
        //username_age
        List<String> fetch1 = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        for (String s : fetch1) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void projection_one() throws Exception {
        //given
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void projection_tuple() throws Exception {
        //given
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple.get(member.username));
            System.out.println("tuple = " + tuple.get(member.age));
        }
        //튜플은 repository안에서만 사용 service로 던질떄는 Dto 로 변경 시켜서 던지자
    }

    @Test
    public void findDtoByJpql() throws Exception {
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username,m.age) from Member m", MemberDto.class)
                .getResultList();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoBySetter() throws Exception {
        //given
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    @Test
    public void findDtoByField() throws Exception {
        //given
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    @Test
    public void findDtoByConstructor() throws Exception {
        //given
        //타입이 맞아야한다 (생성자위치)
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (UserDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUsetDto() throws Exception {
        QMember subMember = new QMember("memberSub");
        //given
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        //서브쿼리
                        ExpressionUtils.as(JPAExpressions
                                .select(subMember.age.max())
                                .from(subMember), "age")))
                .from(member)
                .fetch();
        for (UserDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void queryProjection() throws Exception {
        //given
        List<MemberDto> fetch = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

    }


    @Test
    public void 동적_쿼리_BooleanBulider() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember1(usernameParam,ageParam);
        assertThat(result.size()).isEqualTo(1);

        //then
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        // 파라미터의 값이 널 ? 쿼리가 동적으로 바뀌어야함
        // 검색 조건

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();

    }

    @Test
    public void 동적_쿼리_Whereparam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = null;
        List<Member> result = searchMember2(usernameParam,ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
//                .where(allEq(usernameCond,ageCond))
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
        //where 는 null 이되면 무시된다. 구ㅡ래서 동적쿼리 가능

    }
    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;

    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    //예) 광고 상태 isValid, 날짜가 In : isServiceAble

//    private BooleanExpression isServiceAble(String usernameCond, Integer ageCond) {
//        return isValid(usernameCond).and(In(ageCond));
//    }
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    public void bulkUpdate() throws Exception {
        //given
        //member 1 =10 > 비회원
        //member 2 =20 > 비회원
        //멤버가 28살 이하일때는 다 비회원
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
        //쿼리가 진행되도 영속성 컨텍스트에는 그대로임
        // 그렇지만 DB에는 변동되어있음
        // 영속성 컨텍스트가 우선권을 가지기 때문에 clear 를 해줘야함
        em.flush();
        em.clear();
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member member1 : result) {
            System.out.println("member1 = " + member1.getUsername());
        }
    }

    @Test
    public void bulkAdd() throws Exception {
        //given
        queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    public void bulkDelete() throws Exception {
        //given
        long execute = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    public void sqlFuntion() throws Exception {
          //given
        queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('replace', {0}, {1}, {2})",
                                member.username,"member","m")
                )
                .from(member)
                .fetch();
    }

    @Test
    public void sqlFunction2() throws Exception {
        //given
        queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower',{0}",member.username))
//                )
                .fetch();
    }




}
