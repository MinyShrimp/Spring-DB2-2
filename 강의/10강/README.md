# 스프링 트랜잭션 전파 2 - 활용

## 예제 프로젝트 시작

### 비즈니스 요구사항

* 회원을 등록하고 조회한다.
* 회원에 대한 변경 이력을 추적할 수 있도록 회원 데이터가 변경될 때 변경 이력을 DB LOG 테이블에 남겨야 한다.
    * 여기서는 예제를 단순화 하기 위해 회원 등록시에만 DB LOG 테이블에 남긴다.

### 예제

#### Member

```java
@Entity
@Getter @Setter
@NoArgsConstructor
public class Member {
    @Id
    @GeneratedValue
    private Long id;

    private String username;

    public Member(String username) {
        this.username = username;
    }
}
```

* JPA를 통해 관리하는 회원 엔티티이다.

#### MemberRepository

```java
@Slf4j
@Repository
@RequiredArgsConstructor
public class MemberRepository {
    private final EntityManager em;

    @Transactional
    public void save(Member member) {
        log.info("Member 저장");
        em.persist(member);
    }

    public Optional<Member> find(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList().stream().findAny();
    }
}
```

* JPA를 사용하는 회원 리포지토리이다. 저장과 조회 기능을 제공한다.

#### Log

```java
@Entity
@Getter @Setter
@NoArgsConstructor
public class Log {
    @Id
    @GeneratedValue
    private Long id;
    private String message;

    public Log(String message) {
        this.message = message;
    }
}
```

* JPA를 통해 관리하는 로그 엔티티이다.

#### LogRepository

```java
@Slf4j
@Repository
@RequiredArgsConstructor
public class LogRepository {
    private final EntityManager em;

    @Transactional
    public void save(Log logMessage) {
        log.info("Log 저장");
        em.persist(logMessage);

        if (logMessage.getMessage().contains("로그 예외")) {
            log.info("Log 저장시 예외 발생");
            throw new RuntimeException("예외 발생");
        }
    }

    public Optional<Log> find(String message) {
        return em.createQuery("select l from Log l where l.message = :message", Log.class)
                .setParameter("message", message)
                .getResultList().stream().findAny();
    }
}
```

* JPA를 사용하는 로그 리포지토리이다. 저장과 조회 기능을 제공한다.
* 중간에 예외 상황을 재현하기 위해 로그예외 라고 입력하는 경우 예외를 발생시킨다.

#### MemberService

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final LogRepository logRepository;

    public void joinV1(String username) {
        Member member = new Member(username);
        Log logMessage = new Log(username);

        log.info("== MemberRepository 호출 시작 ==");
        memberRepository.save(member);
        log.info("== MemberRepository 호출 종료 ==");

        log.info("== LogRepository 호출 시작 ==");
        logRepository.save(logMessage);
        log.info("== LogRepository 호출 종료 ==");
    }

    public void joinV2(String username) {
        Member member = new Member(username);
        Log logMessage = new Log(username);

        log.info("== MemberRepository 호출 시작 ==");
        memberRepository.save(member);
        log.info("== MemberRepository 호출 종료 ==");

        log.info("== LogRepository 호출 시작 ==");
        try {
            logRepository.save(logMessage);
        } catch (RuntimeException e) {
            log.info("Log 저장에 실패했습니다. logMessage = {}", logMessage.getMessage());
            log.info("정상 흐름 반환");
        }
        log.info("== LogRepository 호출 종료 ==");
    }
}
```

* 회원을 등록하면서 동시에 회원 등록에 대한 DB 로그도 함께 남긴다.
* `joinV1()`
    * 회원과 DB로그를 함께 남기는 비즈니스 로직이다.
    * 현재 별도의 트랜잭션은 설정하지 않는다.
* `joinV2()`
    * `joinV1()` 과 같은 기능을 수행한다.
    * DB로그 저장시 예외가 발생하면 예외를 복구한다.
    * 현재 별도의 트랜잭션은 설정하지 않는다.

#### MemberServiceTest

```java
@Slf4j
@SpringBootTest
class MemberServiceTest {
    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    LogRepository logRepository;

    /**
     * MemberService    @Transactional:OFF
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON
     */
    @Test
    void outerTxOff_success() {
        // given
        String username = "outerTxOff_success";

        // when
        memberService.joinV1(username);

        // then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }
}
```

### 참고

* JPA의 구현체인 하이버네이트가 테이블을 자동으로 생성해준다.
* 메모리 DB이기 때문에 모든 테스트가 완료된 이후에 DB는 사라진다.
* 여기서는 각각의 테스트가 완료된 시점에 데이터를 삭제하지 않는다.
    * 따라서 username 은 테스트별로 각각 다르게 설정해야 한다.
    * 그렇지 않으면 다음 테스트에 영향을 준다.
    * 모든 테스트가 완료되어야 DB가 사라진다.

#### JPA와 데이터 변경

* JPA를 통한 모든 데이터 변경(등록, 수정, 삭제)에는 트랜잭션이 필요하다.
    * 조회는 트랜잭션 없이 가능하다.
    * 현재 코드에서 서비스 계층에 트랜잭션이 없기 때문에 리포지토리에 트랜잭션이 있다

## 커밋, 롤백

### 서비스 계층에 트랜잭션이 없을 때 - 커밋

#### 상황

* 서비스 계층에 트랜잭션이 없다.
* 회원, 로그 리포지토리가 각각 트랜잭션을 가지고 있다.
* 회원, 로그 리포지토리 둘다 커밋에 성공한다.

#### outerTxOff_success

```java
/**
 * MemberService    @Transactional:OFF
 * MemberRepository @Transactional:ON
 * LogRepository    @Transactional:ON
 */
@Test
void outerTxOff_success() {
    // given
    String username = "outerTxOff_success";

    // when
    memberService.joinV1(username);

    // then
    assertTrue(memberRepository.find(username).isPresent());
    assertTrue(logRepository.find(username).isPresent());
}
```

#### 결과 로그

```
# TEST.outerTxOff_success 시작
# MemberService.joinV1 호출
h.springdb22.propagation.MemberService   : == MemberRepository 호출 시작 ==

# MemberRepository.save 호출
# 트랜잭션 시작
o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [hello.springdb22.propagation.MemberRepository.save]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(1026471930<open>)] for JPA transaction
o.s.orm.jpa.JpaTransactionManager        : Exposing JPA transaction as JDBC [org.springframework.orm.jpa.vendor.HibernateJpaDialect$HibernateConnectionHandle@74a03bd5]
o.s.t.i.TransactionInterceptor           : Getting transaction for [hello.springdb22.propagation.MemberRepository.save]
h.s.propagation.MemberRepository         : Member 저장

# em.persist 호출
org.hibernate.SQL                        : select next value for member_seq

# MemberRepository.save 호출 종료
# 트랜잭션 마무리 작업 시작 - COMMIT
o.s.t.i.TransactionInterceptor           : Completing transaction for [hello.springdb22.propagation.MemberRepository.save]
o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
o.s.orm.jpa.JpaTransactionManager        : Committing JPA transaction on EntityManager [SessionImpl(1026471930<open>)]

# 실제 INSERT 문 보냄
org.hibernate.SQL                        : insert into member (username, id) values (?, ?)
org.hibernate.orm.jdbc.bind              : binding parameter [1] as [VARCHAR] - [outerTxOff_success]
org.hibernate.orm.jdbc.bind              : binding parameter [2] as [BIGINT] - [1]

# 트랜잭션 마무리 작업 종료
o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(1026471930<open>)] after transaction

# MemberService.joinV1 다시 돌아옴
h.springdb22.propagation.MemberService   : == MemberRepository 호출 종료 ==
h.springdb22.propagation.MemberService   : == LogRepository 호출 시작 ==

# LogRepository.save 호출
# 트랜잭션 시작
o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [hello.springdb22.propagation.LogRepository.save]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(1784425773<open>)] for JPA transaction
o.s.orm.jpa.JpaTransactionManager        : Exposing JPA transaction as JDBC [org.springframework.orm.jpa.vendor.HibernateJpaDialect$HibernateConnectionHandle@1dadd172]
o.s.t.i.TransactionInterceptor           : Getting transaction for [hello.springdb22.propagation.LogRepository.save]
h.springdb22.propagation.LogRepository   : Log 저장

# em.persist 호출
org.hibernate.SQL                        : select next value for log_seq

# LogRepository.save 호출 종료
# 트랜잭션 마무리 작업 시작 - COMMIT
o.s.t.i.TransactionInterceptor           : Completing transaction for [hello.springdb22.propagation.LogRepository.save]
o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
o.s.orm.jpa.JpaTransactionManager        : Committing JPA transaction on EntityManager [SessionImpl(1784425773<open>)]

# 실제 INSERT 문 보냄
org.hibernate.SQL                        : insert into log (message, id) values (?, ?)
org.hibernate.orm.jdbc.bind              : binding parameter [1] as [VARCHAR] - [outerTxOff_success]
org.hibernate.orm.jdbc.bind              : binding parameter [2] as [BIGINT] - [1]

# 트랜잭션 마무리 작업 종료
o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(1784425773<open>)] after transaction

# MemberService.joinV1 다시 돌아옴
h.springdb22.propagation.MemberService   : == LogRepository 호출 종료 ==
# MemberService.joinV1 종료

# TEST.outerTxOff_success 다시 돌아옴
# MemberRepository.find 호출
org.hibernate.SQL                        : select m1_0.id,m1_0.username from member m1_0 where m1_0.username=?
org.hibernate.orm.jdbc.bind              : binding parameter [1] as [VARCHAR] - [outerTxOff_success]

# LogRepository.find 호출
org.hibernate.SQL                        : select l1_0.id,l1_0.message from log l1_0 where l1_0.message=?
org.hibernate.orm.jdbc.bind              : binding parameter [1] as [VARCHAR] - [outerTxOff_success]

# TEST.outerTxOff_success 종료
```

#### 요청 흐름

![img.png](img.png)

* `MemberService`에서 `MemberRepository`를 호출한다.
    * `MemberRepository`에는 `@Transactional` 애노테이션이 있으므로 트랜잭션 AOP가 작동한다.
    * 여기서 트랜잭션 매니저를 통해 트랜잭션을 시작한다. 이렇게 시작한 트랜잭션을 트랜잭션 B라 하자.
        * 트랜잭션 매니저에 트랜잭션을 요청하면 데이터소스를 통해 커넥션 `con1`을 획득하고,
        * 해당 커넥션을 수동 커밋 모드로 변경해서 트랜잭션을 시작한다.
        * 그리고 트랜잭션 동기화 매니저를 통해 트랜잭션을 시작한 커넥션을 보관한다.
    * 트랜잭션 매니저의 호출 결과로 `status`를 반환한다. 여기서는 신규 트랜잭션 여부가 참이 된다.
* `MemberRepository`는 JPA를 통해 회원을 저장하는데, 이때 JPA는 트랜잭션이 시작된 `con1`을 사용해서 회원을 저장한다.
* `MemberRepository`가 정상 응답을 반환했기 때문에 트랜잭션 AOP는 트랜잭션 매니저에 커밋을 요청한다.
* 트랜잭션 매니저는 `con1`을 통해 물리 트랜잭션을 커밋한다.
    * 물론 이 시점에 앞서 설명한 신규 트랜잭션 여부, `rollbackOnly`여부를 모두 체크한다.

이렇게 해서 `MemberRepository`와 관련된 모든 데이터는 정상 커밋되고, 트랜잭션 B는 완전히 종료된다.
이후에 `LogRepository`를 통해 트랜잭션 C를 시작하고, 정상 커밋한다.
결과적으로 둘다 커밋되었으므로 `Member`, `Log` 모두 안전하게 저장된다.

#### @Transactional - REQUIRED

* 트랜잭션 전파의 기본 값은 `REQUIRED`이다. 따라서 다음 둘은 같다.
    * @Transactional(propagation = Propagation.REQUIRED)
    * @Transactional
* `REQUIRED`는 기존 트랜잭션이 없으면 새로운 트랜잭션을 만들고, 기존 트랜잭션이 있으면 참여한다.

### 서비스 계층에 트랜잭션이 없을 때 - 롤백

#### 상황

* 서비스 계층에 트랜잭션이 없다.
* 회원, 로그 리포지토리가 각각 트랜잭션을 가지고 있다.
* 회원 리포지토리는 정상 동작하지만 로그 리포지토리에서 예외가 발생한다.

#### outerTxOff_fail

```java
/**
 * MemberService    @Transactional:OFF
 * MemberRepository @Transactional:ON
 * LogRepository    @Transactional:ON
 */
@Test
void outerTxOff_fail() {
    // given
    String username = "로그 예외_outerTxOff_fail";

    // when
    assertThatThrownBy(() -> memberService.joinV1(username))
            .isInstanceOf(RuntimeException.class);

    // then: 완전히 롤백되지 않고, member 데이터가 남아서 저장된다.
    assertTrue(memberRepository.find(username).isPresent());
    assertTrue(logRepository.find(username).isEmpty());
}
```

* 사용자 이름에 로그예외 라는 단어가 포함되어 있으면 `LogRepository`에서 런타임 예외가 발생한다.
* 트랜잭션 AOP는 해당 런타임 예외를 확인하고 롤백 처리한다.

#### 결과 로그

```
#################################
# MemberRepository 호출 생략
# - 이전과 동일
#################################

# MemberService.joinV2 되돌아옴
h.springdb22.propagation.MemberService   : == LogRepository 호출 시작 ==

# LogRepository 호출 시작
# 트랜잭션 시작
o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [hello.springdb22.propagation.LogRepository.save]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(1478396265<open>)] for JPA transaction
o.s.orm.jpa.JpaTransactionManager        : Exposing JPA transaction as JDBC [org.springframework.orm.jpa.vendor.HibernateJpaDialect$HibernateConnectionHandle@767b9d66]
o.s.t.i.TransactionInterceptor           : Getting transaction for [hello.springdb22.propagation.LogRepository.save]
h.springdb22.propagation.LogRepository   : Log 저장

# em.persist 호출
org.hibernate.SQL                        : select next value for log_seq

# RuntimeException 발생
h.springdb22.propagation.LogRepository   : Log 저장시 예외 발생

# LogRepository 호출 종료
# 트랜잭션 종료 준비 - ROLLBACK
o.s.t.i.TransactionInterceptor           : Completing transaction for [hello.springdb22.propagation.LogRepository.save] after exception: java.lang.RuntimeException: 예외 발생
o.s.orm.jpa.JpaTransactionManager        : Initiating transaction rollback
o.s.orm.jpa.JpaTransactionManager        : Rolling back JPA transaction on EntityManager [SessionImpl(1478396265<open>)]

# 트랜잭션 종료
o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(1478396265<open>)] after transaction

# Test.outerTxOff_fail 되돌아옴
# MemberRepository.find 호출
org.hibernate.SQL                        : select m1_0.id,m1_0.username from member m1_0 where m1_0.username=?
org.hibernate.orm.jdbc.bind              : binding parameter [1] as [VARCHAR] - [로그 예외_outerTxOff_fail]

# LogRepository.find 호출
org.hibernate.SQL                        : select l1_0.id,l1_0.message from log l1_0 where l1_0.message=?
org.hibernate.orm.jdbc.bind              : binding parameter [1] as [VARCHAR] - [로그 예외_outerTxOff_fail]
```

#### 요청 흐름

![img_1.png](img_1.png)

* `MemberService`에서 `MemberRepository`를 호출하는 부분은 앞서 설명한 내용과 같다.
* 트랜잭션이 정상 커밋되고, 회원 데이터도 DB에 정상 반영된다.
* `MemberService`에서 `LogRepository`를 호출하는데, "로그 예외" 라는 이름을 전달한다.
* 이 과정에서 새로운 트랜잭션 C가 만들어진다.

#### LogRepository 응답 로직

1. `LogRepository`는 트랜잭션 C와 관련된 `con2`를 사용한다.
2. 로그예외 라는 이름을 전달해서 `LogRepository`에 런타임 예외가 발생한다.
3. `LogRepository`는 해당 예외를 밖으로 던진다. 이 경우 트랜잭션 AOP가 예외를 받게된다.
4. 런타임 예외가 발생해서 트랜잭션 AOP는 트랜잭션 매니저에 롤백을 호출한다.
5. 트랜잭션 매니저는 신규 트랜잭션이므로 물리 롤백을 호출한다.

#### 결과

* 이 경우 회원은 저장되지만, 회원 이력 로그는 롤백된다.
    * 따라서 **데이터 정합성**에 문제가 발생할 수 있다.
* 둘을 하나의 트랜잭션으로 묶어서 처리해보자.

## 단일 트랜잭션

### 예제

#### MemberService, Repositories

```java
@Transactional
public void joinV1(String username) { ... }

// @Transactional
public void save(Log logMessage) { ... }

// @Transactional
public void save(Member member) { ... }
```

* `MemberService`에만 `@Transactional` 코드를 추가하자.
* `MemberRepository`, `LogRepository`의 `@Transactional` 코드를 제거하자.

#### singleTx

```java
/**
 * MemberService    @Transactional:ON
 * MemberRepository @Transactional:OFF
 * LogRepository    @Transactional:OFF
 */
@Test
void singleTx() {
    // given
    String username = "singleTx";

    // when
    memberService.joinV1(username);

    // then: 모든 데이터가 정상 저장된다.
    assertTrue(memberRepository.find(username).isPresent());
    assertTrue(logRepository.find(username).isEmpty());
}
```

#### 결과 로그

```
# MemberService.joinV1 호출
# 트랜잭션 시작
o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [hello.springdb22.propagation.MemberService.joinV1]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(1659772041<open>)] for JPA transaction
o.s.orm.jpa.JpaTransactionManager        : Exposing JPA transaction as JDBC [org.springframework.orm.jpa.vendor.HibernateJpaDialect$HibernateConnectionHandle@c335b9]
o.s.t.i.TransactionInterceptor           : Getting transaction for [hello.springdb22.propagation.MemberService.joinV1]

# em.persist(member) 호출
h.springdb22.propagation.MemberService   : == MemberRepository 호출 시작 ==
h.s.propagation.MemberRepository         : Member 저장
org.hibernate.SQL                        : select next value for member_seq
h.springdb22.propagation.MemberService   : == MemberRepository 호출 종료 ==

# em.persist(logMessage) 호출
h.springdb22.propagation.MemberService   : == LogRepository 호출 시작 ==
h.springdb22.propagation.LogRepository   : Log 저장
org.hibernate.SQL                        : select next value for log_seq
h.springdb22.propagation.MemberService   : == LogRepository 호출 종료 ==

# MemberService.joinV1 호출 종료
# 트랜잭션 종료 준비 - COMMIT
o.s.t.i.TransactionInterceptor           : Completing transaction for [hello.springdb22.propagation.MemberService.joinV1]
o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
o.s.orm.jpa.JpaTransactionManager        : Committing JPA transaction on EntityManager [SessionImpl(1659772041<open>)]

# INSERT Member 
org.hibernate.SQL                        : insert into member (username, id) values (?, ?)
org.hibernate.orm.jdbc.bind              : binding parameter [1] as [VARCHAR] - [singleTx]
org.hibernate.orm.jdbc.bind              : binding parameter [2] as [BIGINT] - [1]

# INSERT Log
org.hibernate.SQL                        : insert into log (message, id) values (?, ?)
org.hibernate.orm.jdbc.bind              : binding parameter [1] as [VARCHAR] - [singleTx]
org.hibernate.orm.jdbc.bind              : binding parameter [2] as [BIGINT] - [1]

# 트랜잭션 종료
o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(1659772041<open>)] after transaction
```

### 흐릉 정리

![img_2.png](img_2.png)

* 이렇게 하면 `MemberService`를 시작할 때 부터 종료할 때 까지의 모든 로직을 하나의 트랜잭션으로 묶을 수 있다.
    * 물론 `MemberService`가 `MemberRepository`, `LogRepository`를 호출하므로 이 로직들은 같은 트랜잭션을 사용한다.
* `MemberService`만 트랜잭션을 처리하기 때문에 앞서 배운 복잡한 것을 고민할 필요가 없다.
    * 논리 트랜잭션, 물리 트랜잭션, 외부 트랜잭션, 내부 트랜잭션, `rollbackOnly`, 신규 트랜잭션, 트랜잭션 전파
* 아주 단순하고 깔끔하게 트랜잭션을 묶을 수 있다.

![img_3.png](img_3.png)

* `@Transactional`이 `MemberService`에만 붙어있기 때문에 여기에만 트랜잭션 AOP가 적용된다.
    * `MemberRepository`, `LogRepository`는 트랜잭션 AOP가 적용되지 않는다.
* `MemberService`의 시작부터 끝까지, 관련 로직은 해당 트랜잭션이 생성한 커넥션을 사용하게 된다.
    * `MemberService`가 호출하는 `MemberRepository`, `LogRepository`도 같은 커넥션을 사용하면서 자연스럽게 트랜잭션 범위에 포함된다.

### 각각 트랜잭션이 필요한 상황

![img_4.png](img_4.png)

#### 트랜잭션 적용 범위

![img_5.png](img_5.png)

* 클라이언트 A는 `MemberService`부터 `MemberRepository`, `LogRepository`를 모두 하나의 트랜잭션으로 묶고 싶다.
* 클라이언트 B는 `MemberRepository`만 호출하고 여기에만 트랜잭션을 사용하고 싶다.
* 클라이언트 C는 `LogRepository`만 호출하고 여기에만 트랜잭션을 사용하고 싶다.

* 클라이언트 A만 생각하면 `MemberService`에 트랜잭션 코드를 남기고,
  `MemberRepository`, `LogRepository`의 트랜잭션 코드를 제거하면 앞서 배운 것 처럼 깔끔하게 하나의 트랜잭션을 적용할 수 있다.
* 하지만 이렇게 되면 클라이언트 B, C가 호출하는 `MemberRepository`, `LogRepository`에는 트랜잭션을 적용할 수 없다.

## 전파 커밋

## 전파 롤백

## 복구 REQUIRED

## 복구 REQUIRES_NEW