package study.querydsl.repository;


import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.MemberSearchCondition;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long memberId) {
        Member findMember = em.find(Member.class, memberId);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll_querydsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m",Member.class)
                .getResultList();
    }

    public List<Member> findByUsername(String name) {
        return em.createQuery("select m from Member m where m.username = :name",Member.class)
                .setParameter("name",name)
                .getResultList();
    }

    public List<Member> findByUsername_querydsl(String name) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(name))
                .fetch();
    }
    public List<MemberTeamDto> searchByBulider(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(condition.getUserName())) {
            builder.and(member.username.eq(condition.getUserName()));
        }
        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }
        return queryFactory
                .select(new QMemberTeamDto(member.id.as("memberId"),
                        member.username.as("userName"),
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team , team)
                .where(builder)
                .fetch();
    }
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(member.id.as("memberId"),
                        member.username.as("userName"),
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team , team)
                .where(usernameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetch();
    }
    //재사용 가능
    public List<Member> searchMember(MemberSearchCondition condition) {
        return queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team , team)
                .where(usernameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeLoe(),condition.getAgeGoe()))
                        //isValide 공통적인 null체크, 필수값 등
                .fetch();
    }

    private BooleanExpression ageBetween(int ageLoe, int ageGoe) {
        return ageGoe(ageGoe).and((ageLoe(ageLoe)));
    }

    private BooleanExpression usernameEq(String userName) {
        return hasText(userName) ? member.username.eq(userName) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

}


















