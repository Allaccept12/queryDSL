package study.querydsl.repository;


import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.MemberSearchCondition;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
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

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> result = queryFactory
                .select(new QMemberTeamDto(member.id.as("memberId"),
                        member.username.as("userName"),
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())//한번 조회할때 몇개까지 조회
                .fetchResults();// 컨텐츠와 카운트 쿼리 두번 날림
        List<MemberTeamDto> content = result.getResults();
        long count = result.getTotal();
        return new PageImpl<>(content,pageable,count);

    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {

        //둘을 쪼갠 이유는 최적화 가능성 때문임, 카운트를 먼저 세고, 혹시 카운트가 없으면 content쿼리를 진행하지 않는다던지
        //혹은 컨텐트 쿼리는 복잡한데 카운트 쿼리는 쉬울때 카운트 쿼리를 빼서 최적화 시킨다던지
        //단, 데이터 몇개 없으면 fetchResult는 이제 곧 없어지기때문에 사용하면 안된다.
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(member.id.as("memberId"),
                        member.username.as("userName"),
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())//한번 조회할때 몇개까지 조회
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()));


        return PageableExecutionUtils.getPage(content,pageable,countQuery::fetchOne);
        //return new PageImpl<>(content,pageable,count);

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
