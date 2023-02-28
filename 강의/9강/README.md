# 스프링 트랜잭션 전파 1 - 기본

## 커밋, 롤백

### BasicTxTest

```java
@Slf4j
@SpringBootTest
public class BasicTxTest {

    @Autowired
    PlatformTransactionManager txManager;

    @TestConfiguration
    static class Config {
        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
```

* `@TestConfiguration`
    * 해당 테스트에서 필요한 스프링 설정을 추가로 할 수 있다.
* `DataSourceTransactionManager`를 스프링 빈으로 등록했다.
    * 이후 트랜잭션 매니저인 `PlatformTransactionManager`를 주입 받으면 방금 등록한 `DataSourceTransactionManager`가 주입된다.

#### commit

```java
@Test
void commit() {
    log.info("트랜잭션 시작");
    TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

    log.info("트랜잭션 커밋 시작");
    txManager.commit(status);
    log.info("트랜잭션 커밋 완료");
}
```

* `txManager.getTransaction()`
    * 트랜잭션 매니저를 통해 트랜잭션을 시작(획득)한다.
* `new DefaultTransactionAttribute()`
    * 트랜잭션 매니저의 설정 중에서 기본값으로 설정한다.
* `txManager.commit(status)`
    * 트랜잭션을 커밋한다.

#### commit 결과

```
h.springdb22.propagation.BasicTxTest     : 트랜잭션 시작
o.s.j.d.DataSourceTransactionManager     : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
o.s.j.d.DataSourceTransactionManager     : Acquired Connection [HikariProxyConnection@35126588 wrapping conn0: url=jdbc:h2:mem:ac212226-6d1a-44aa-b1ee-99e7730db536 user=SA] for JDBC transaction
o.s.j.d.DataSourceTransactionManager     : Switching JDBC Connection [HikariProxyConnection@35126588 wrapping conn0: url=jdbc:h2:mem:ac212226-6d1a-44aa-b1ee-99e7730db536 user=SA] to manual commit

h.springdb22.propagation.BasicTxTest     : 트랜잭션 커밋 시작
o.s.j.d.DataSourceTransactionManager     : Initiating transaction commit
o.s.j.d.DataSourceTransactionManager     : Committing JDBC transaction on Connection [HikariProxyConnection@35126588 wrapping conn0: url=jdbc:h2:mem:ac212226-6d1a-44aa-b1ee-99e7730db536 user=SA]
o.s.j.d.DataSourceTransactionManager     : Releasing JDBC Connection [HikariProxyConnection@35126588 wrapping conn0: url=jdbc:h2:mem:ac212226-6d1a-44aa-b1ee-99e7730db536 user=SA] after transaction
h.springdb22.propagation.BasicTxTest     : 트랜잭션 커밋 완료
```

#### rollback

```java
@Test
void rollback() {
    log.info("트랜잭션 시작");
    TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

    log.info("트랜잭션 롤백 시작");
    txManager.rollback(status);
    log.info("트랜잭션 롤백 완료");
}
```

* `txManager.getTransaction()`
    * 트랜잭션 매니저를 통해 트랜잭션을 시작(획득)한다.
* `new DefaultTransactionAttribute()`
    * 트랜잭션 매니저의 설정 중에서 기본값으로 설정한다.
* `txManager.rollback(status)`
    * 트랜잭션을 롤백한다.

#### rollback 결과

```
h.springdb22.propagation.BasicTxTest     : 트랜잭션 시작
o.s.j.d.DataSourceTransactionManager     : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
o.s.j.d.DataSourceTransactionManager     : Acquired Connection [HikariProxyConnection@542467430 wrapping conn0: url=jdbc:h2:mem:ac212226-6d1a-44aa-b1ee-99e7730db536 user=SA] for JDBC transaction
o.s.j.d.DataSourceTransactionManager     : Switching JDBC Connection [HikariProxyConnection@542467430 wrapping conn0: url=jdbc:h2:mem:ac212226-6d1a-44aa-b1ee-99e7730db536 user=SA] to manual commit
h.springdb22.propagation.BasicTxTest     : 트랜잭션 롤백 시작
o.s.j.d.DataSourceTransactionManager     : Initiating transaction rollback
o.s.j.d.DataSourceTransactionManager     : Rolling back JDBC transaction on Connection [HikariProxyConnection@542467430 wrapping conn0: url=jdbc:h2:mem:ac212226-6d1a-44aa-b1ee-99e7730db536 user=SA]
o.s.j.d.DataSourceTransactionManager     : Releasing JDBC Connection [HikariProxyConnection@542467430 wrapping conn0: url=jdbc:h2:mem:ac212226-6d1a-44aa-b1ee-99e7730db536 user=SA] after transaction
h.springdb22.propagation.BasicTxTest     : 트랜잭션 롤백 완료
```

## 트랜잭션 두 번 사용

## 전파 기본

## 전파 예제

## 외부 롤백

## 내부 롤백

## REQUIRES_NEW

## 다양한 전파 옵션