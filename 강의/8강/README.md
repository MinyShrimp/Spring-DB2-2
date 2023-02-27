# 스프링 트랜잭션 이해

## 프로젝트 생성

* https://start.spring.io/
* 프로젝트 선택
    * Project: Gradle - Groovy
    * Language: Java 17
    * Spring Boot: 3.0.3
* Project Metadata
    * Group: hello
    * Artifact: spring-db2-2
    * Packaging: Jar
* Dependencies
    * Spring Data JPA, H2 Database, Lombok

#### build.gradle

```gradle
dependencies {
    // ...
    // 테스트에서 Lombok 사용
    testCompileOnly 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'
}
```

## 트랜잭션 적용 확인

@Transactional 을 통해 선언적 트랜잭션 방식을 사용하면 단순히 애노테이션 하나로 트랜잭션을 적용할 수 있다.
그런데 이 기능은 트랜잭션 관련 코드가 눈에 보이지 않고, AOP를 기반으로 동작하기 때문에, 실제 트랜잭션이 적용되고 있는지 아닌지를 확인하기가 어렵다.

### 확인 방법

#### TxApplyBasicTest

```java
@Slf4j
@SpringBootTest
public class TxApplyBasicTest {

    @Autowired
    BasicService basicService;

    @Test
    void proxyCheck() {
        log.info("aop class = {}", basicService.getClass());
        Assertions.assertThat(AopUtils.isAopProxy(basicService)).isTrue();
    }

    @Test
    void txCheck() {
        basicService.tx();
        basicService.nonTx();
    }

    @TestConfiguration
    static class TxApplyBasicConfig {
        @Bean
        BasicService basicService() {
            return new BasicService();
        }
    }

    @Slf4j
    static class BasicService {
        @Transactional
        public void tx() {
            log.info("call tx");
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active = {}", txActive);
        }

        public void nonTx() {
            log.info("call nonTx");
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active = {}", txActive);
        }
    }
}
```

#### proxyCheck

```
aop class = class hello.springdb22.apply.TxApplyBasicTest$BasicService$$SpringCGLIB$$0
```

* `AopUtils.isAopProxy()`
* 선언적 트랜잭션 방식에서 스프링 트랜잭션은 AOP를 기반으로 동작한다.
    * `@Transactional` 을 메서드나 클래스에 붙이면 해당 객체는 트랜잭션 AOP 적용의 대상이 되고,
      결과적으로 실제 객체 대신에 트랜잭션을 처리해주는 프록시 객체가 스프링 빈에 등록된다.
    * 그리고 주입을 받을 때도 실제 객체 대신에 프록시 객체가 주입된다.
* 클래스 이름을 출력해보면 `basicService$$EnhancerBySpringCGLIB...` 라고 프록시 클래스의 이름이 출력되는 것을 확인할 수 있다.

### 스프링 컨테이너에 트랜잭션 프록시 등록

![img.png](img.png)

* `@Transactional` 애노테이션이 특정 클래스나 메서드에 하나라도 있으면 있으면 트랜잭션 AOP는 프록시를 만들어서 스프링 컨테이너에 등록한다.
    * 그리고 실제 `basicService` 객체 대신에 프록시인 `basicService$$CGLIB`를 스프링 빈에 등록한다.
    * 그리고 프록시는 내부에 실제 `basicService`를 참조하게 된다.
    * 여기서 핵심은 실제 객체 대신에 프록시가 스프링 컨테이너에 등록되었다는 점이다.
* 클라이언트인 `txBasicTest`는 스프링 컨테이너에 `@Autowired BasicService basicService`로 의존관계 주입을 요청한다.
    * 스프링 컨테이너에는 실제 객체 대신에 프록시가 스프링 빈으로 등록되어 있기 때문에 프록시를 주입한다.
    * 프록시는 `BasicService`를 상속해서 만들어지기 때문에 다형성을 활용할 수 있다.
* 따라서 `BasicService` 대신에 프록시인 `BasicService$$CGLIB`를 주입할 수 있다.

#### 동작 방식

![img_1.png](img_1.png)

### txTest

#### application.properties

```properties
# Transaction
logging.level.org.springframework.transaction.interceptor = trace
```

이 로그를 추가하면 트랜잭션 프록시가 호출하는 트랜잭션의 시작과 종료를 명확하게 로그로 확인할 수 있다.

#### BasicService.tx

* 클라이언트가 `basicService.tx()`를 호출하면, 프록시의 `tx()`가 호출된다.
    * 여기서 프록시는 `tx()` 메서드가 트랜잭션을 사용할 수 있는지 확인해본다.
    * `tx()` 메서드에는 `@Transactional`이 붙어있으므로 트랜잭션 적용 대상이다.
* 따라서 트랜잭션을 시작한 다음에 실제 `basicService.tx()`를 호출한다.
* 그리고 실제 `basicService.tx()`의 호출이 끝나서 프록시로 제어가(리턴) 돌아오면
  프록시는 트랜잭션 로직을 커밋하거나 롤백해서 트랜잭션을 종료한다.

#### BasicService.nonTx

* 클라이언트가 `basicService.nonTx()`를 호출하면, 트랜잭션 프록시의 `nonTx()`가 호출된다.
    * 여기서 `nonTx()` 메서드가 트랜잭션을 사용할 수 있는지 확인해본다.
    * `nonTx()`에는 `@Transactional`이 없으므로 적용 대상이 아니다.
* 따라서 트랜잭션을 시작하지 않고, `basicService.nonTx()`를 호출하고 종료한다.

#### TransactionSynchronizationManager.isActualTransactionActive

* 현재 쓰레드에 트랜잭션이 적용되어 있는지 확인할 수 있는 기능이다.
  결과가 `true`면 트랜잭션이 적용되어 있는 것이다.
  트랜잭션의 적용 여부를 가장 확실하게 확인할 수 있다.

#### 로그

```
# tx() 호출
TransactionInterceptor        : Getting transaction for [hello.springdb22.apply.TxApplyBasicTest$BasicService.tx]
TxApplyBasicTest$BasicService : call tx
TxApplyBasicTest$BasicService : tx active = true
TransactionInterceptor        : Completing transaction for [hello.springdb22.apply.TxApplyBasicTest$BasicService.tx]

# nonTx() 호출
TxApplyBasicTest$BasicService : call nonTx
TxApplyBasicTest$BasicService : tx active = false
```

* 로그를 통해 `tx()` 호출시에는 `tx active=true`를 통해 트랜잭션이 적용된 것을 확인할 수 있다.
* `TransactionInterceptor` 로그를 통해 트랜잭션 프록시가 트랜잭션을 시작하고 완료한 내용을 확인할 수 있다.
* `nonTx()` 호출시에는 `tx active=false`를 통해 트랜잭션이 없는 것을 확인할 수 있다.

## 트랜잭션 적용 위치

스프링에서 우선순위는 항상 더 구체적이고 자세한 것이 높은 우선순위를 가진다.
이것만 기억하면 스프링에서 발생하는 대부분의 우선순위를 쉽게 기억할 수 있다.
그리고 더 구체적인 것이 더 높은 우선순위를 가지는 것은 상식적으로 자연스럽다.

* 클래스 애노테이션 < 메서드 애노테이션
* 부모 클래스 < 자식 클래스

### TxApplyLevelTest

```java
@SpringBootTest
public class TxApplyLevelTest {
    @Autowired
    LevelService service;

    @Test
    void orderTest() {
        service.write();
        service.read();
    }

    @TestConfiguration
    static class TxApplyLevelConfig {
        @Bean
        LevelService levelService() {
            return new LevelService();
        }
    }

    @Slf4j
    @Transactional(readOnly = true)
    static class LevelService {
        @Transactional(readOnly = false)
        public void write() {
            log.info("call write");
            printTxInfo();
        }

        public void read() {
            log.info("call read");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active = {}", txActive);

            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("tx readOnly = {}", readOnly);
        }
    }
}
```

스프링의 `@Transactional` 은 다음 두 가지 규칙이 있다.

#### 우선순위

트랜잭션을 사용할 때는 다양한 옵션을 사용할 수 있다.
그런데 어떤 경우에는 옵션을 주고, 어떤 경우에는 옵션을 주지 않으면 어떤 것이 선택될까?
예를 들어서 읽기 전용 트랜잭션 옵션을 사용하는 경우와 아닌 경우를 비교해보자.
(읽기 전용 옵션에 대한 자세한 내용은 뒤에서 다룬다. 여기서는 적용 순서에 집중하자.)

* `LevelService`의 타입에 `@Transactional(readOnly = true)`이 붙어있다.
* `write()`
    * 해당 메서드에 `@Transactional(readOnly = false)`이 붙어있다.
    * 이렇게 되면 타입에 있는 `@Transactional(readOnly = true)`와
      해당 메서드에 있는 `@Transactional(readOnly = false)` 둘 중 하나를 적용해야 한다.
    * 클래스 보다는 메서드가 더 구체적이므로 메서드에 있는
      `@Transactional(readOnly = false)` 옵션을 사용한 트랜잭션이 적용된다.

#### 클래스에 적용하면 메서드는 자동 적용

* `read()`
* 해당 메서드에 `@Transactional`이 없다. 이 경우 더 상위인 클래스를 확인한다.
    * 클래스에 `@Transactional(readOnly = true)`이 적용되어 있다.
    * 따라서 트랜잭션이 적용되고 `readOnly = true` 옵션을 사용하게 된다.
* 참고로 `readOnly = false` 는 기본 옵션이기 때문에 보통 생략한다.

#### TransactionSynchronizationManager.isCurrentTransactionReadOnly

현재 트랜잭션에 적용된 `readOnly` 옵션의 값을 반환한다.

#### 실행 결과

```
# write()
TransactionInterceptor     : Getting transaction for [hello.springdb22.apply.TxLevelTest$LevelService.write]
TxLevelTest$LevelService   : call write
TxLevelTest$LevelService   : tx active = true
TxLevelTest$LevelService   : tx readOnly = false
TransactionInterceptor     : Completing transaction for [hello.springdb22.apply.TxLevelTest$LevelService.write]

# read()
TransactionInterceptor     : Getting transaction for [hello.springdb22.apply.TxLevelTest$LevelService.read]
TxLevelTest$LevelService   : call read
TxLevelTest$LevelService   : tx active = true
TxLevelTest$LevelService   : tx readOnly = true
TransactionInterceptor     : Completing transaction for [hello.springdb22.apply.TxLevelTest$LevelService.read]
```

### 인터페이스에 @Transactional 적용

인터페이스에도 `@Transactional`을 적용할 수 있다. 이 경우 다음 순서로 적용된다.
구체적인 것이 더 높은 우선순위를 가진다고 생각하면 바로 이해가 될 것이다.

1. 클래스의 메서드 (우선순위가 가장 높다.)
2. 클래스의 타입
3. 인터페이스의 메서드
4. 인터페이스의 타입 (우선순위가 가장 낮다.)

그런데 인터페이스에 `@Transactional`을 사용하는 것은 스프링 공식 메뉴얼에서 **권장하지 않는 방법**이다.
AOP를 적용하는 방식에 따라서 인터페이스에 애노테이션을 두면 AOP가 적용이 되지 않는 경우도 있기 때문이다.

> 참고<br>
> 스프링은 인터페이스에 `@Transactional`을 사용하는 방식을 스프링 5.0에서 많은 부분 개선했다.
> 과거에는 구체 클래스를 기반으로 프록시를 생성하는 CGLIB 방식을 사용하면 인터페이스에 있는 `@Transactional`을 인식하지 못했다.
> 스프링 5.0 부터는 이 부분을 개선해서 인터페이스에 있는 `@Transactional`도 인식한다.
> 하지만 다른 AOP 방식에서 또 적용되지 않을 수 있으므로 공식 메뉴얼의 가이드대로 가급적 구체 클래스에 `@Transactional`을 사용하자.

## 트랜잭션 AOP 주의 사항 - 프로시 내부 호출 1

## 트랜잭션 AOP 주의 사항 - 프로시 내부 호출 2

## 트랜잭션 AOP 주의 사항 - 초기화 시점

## 트랜잭션 옵션 소개

## 예외와 트랜잭션 커밋, 롤백 - 기본

## 예외와 트랜잭션 커밋, 롤백 - 활용