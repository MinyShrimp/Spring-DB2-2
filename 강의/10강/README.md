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

## 단일 트랜잭션

## 전파 커밋

## 전파 롤백

## 복구 REQUIRED

## 복구 REQUIRES_NEW