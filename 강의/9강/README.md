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

### double_commit

```java
@Test
void double_commit() {
    log.info("트랜잭션 1 시작");
    TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionDefinition());

    log.info("트랜잭션 1 커밋");
    txManager.commit(tx1);

    log.info("트랜잭션 2 시작");
    TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionDefinition());

    log.info("트랜잭션 2 커밋");
    txManager.commit(tx2);
}
```

#### 결과 로그

```
h.springdb22.propagation.BasicTxTest     : 트랜잭션 1 시작
o.s.j.d.DataSourceTransactionManager     : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
o.s.j.d.DataSourceTransactionManager     : Acquired Connection [HikariProxyConnection@1772902226 wrapping conn0: url=jdbc:h2:mem:2c05b56e-60b9-48e8-8612-b3eaf3f4ef2b user=SA] for JDBC transaction
o.s.j.d.DataSourceTransactionManager     : Switching JDBC Connection [HikariProxyConnection@1772902226 wrapping conn0: url=jdbc:h2:mem:2c05b56e-60b9-48e8-8612-b3eaf3f4ef2b user=SA] to manual commit
h.springdb22.propagation.BasicTxTest     : 트랜잭션 1 커밋
o.s.j.d.DataSourceTransactionManager     : Initiating transaction commit
o.s.j.d.DataSourceTransactionManager     : Committing JDBC transaction on Connection [HikariProxyConnection@1772902226 wrapping conn0: url=jdbc:h2:mem:2c05b56e-60b9-48e8-8612-b3eaf3f4ef2b user=SA]
o.s.j.d.DataSourceTransactionManager     : Releasing JDBC Connection [HikariProxyConnection@1772902226 wrapping conn0: url=jdbc:h2:mem:2c05b56e-60b9-48e8-8612-b3eaf3f4ef2b user=SA] after transaction

h.springdb22.propagation.BasicTxTest     : 트랜잭션 2 시작
o.s.j.d.DataSourceTransactionManager     : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
o.s.j.d.DataSourceTransactionManager     : Acquired Connection [HikariProxyConnection@444211664 wrapping conn0: url=jdbc:h2:mem:2c05b56e-60b9-48e8-8612-b3eaf3f4ef2b user=SA] for JDBC transaction
o.s.j.d.DataSourceTransactionManager     : Switching JDBC Connection [HikariProxyConnection@444211664 wrapping conn0: url=jdbc:h2:mem:2c05b56e-60b9-48e8-8612-b3eaf3f4ef2b user=SA] to manual commit
h.springdb22.propagation.BasicTxTest     : 트랜잭션 2 커밋
o.s.j.d.DataSourceTransactionManager     : Initiating transaction commit
o.s.j.d.DataSourceTransactionManager     : Committing JDBC transaction on Connection [HikariProxyConnection@444211664 wrapping conn0: url=jdbc:h2:mem:2c05b56e-60b9-48e8-8612-b3eaf3f4ef2b user=SA]
o.s.j.d.DataSourceTransactionManager     : Releasing JDBC Connection [HikariProxyConnection@444211664 wrapping conn0: url=jdbc:h2:mem:2c05b56e-60b9-48e8-8612-b3eaf3f4ef2b user=SA] after transaction
```

#### 동작 방식

![img.png](img.png)

* 트랜잭션1: `Acquired Connection [HikariProxyConnection@1772902226 wrapping conn0]`
* 트랜잭션2: `Acquired Connection [HikariProxyConnection@444211664 wrapping conn0]`

결과적으로 `conn0`을 통해 커넥션이 재사용 된 것을 확인할 수 있고,
`HikariProxyConnection@1772902226`, `HikariProxyConnection@444211664`을 통해 각각 커넥션 풀에서 커넥션을 조회한 것을 확인할 수 있다.

![img_1.png](img_1.png)

* 트랜잭션이 각각 수행되면서 사용되는 DB 커넥션도 각각 다르다.
* 이 경우 트랜잭션을 각자 관리하기 때문에 전체 트랜잭션을 묶을 수 없다.
    * 예를 들어서 트랜잭션1이 커밋하고, 트랜잭션2가 롤백하는 경우
      트랜잭션1에서 저장한 데이터는 커밋되고, 트랜잭션2에서 저장한 데이터는 롤백된다.

### double_commit_rollback

```java
@Test
void double_commit_rollback() {
    log.info("트랜잭션 1 시작");
    TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionDefinition());
    log.info("트랜잭션 1 커밋");
    txManager.commit(tx1);

    log.info("트랜잭션 2 시작");
    TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionDefinition());
    log.info("트랜잭션 2 롤백");
    txManager.rollback(tx2);
}
```

#### 결과 로그

```
h.springdb22.propagation.BasicTxTest     : 트랜잭션 1 시작
o.s.j.d.DataSourceTransactionManager     : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
o.s.j.d.DataSourceTransactionManager     : Acquired Connection [HikariProxyConnection@1772902226 wrapping conn0: url=jdbc:h2:mem:ece222f5-6dae-4e4e-9e4b-e3a15534f998 user=SA] for JDBC transaction
o.s.j.d.DataSourceTransactionManager     : Switching JDBC Connection [HikariProxyConnection@1772902226 wrapping conn0: url=jdbc:h2:mem:ece222f5-6dae-4e4e-9e4b-e3a15534f998 user=SA] to manual commit
h.springdb22.propagation.BasicTxTest     : 트랜잭션 1 커밋
o.s.j.d.DataSourceTransactionManager     : Initiating transaction commit
o.s.j.d.DataSourceTransactionManager     : Committing JDBC transaction on Connection [HikariProxyConnection@1772902226 wrapping conn0: url=jdbc:h2:mem:ece222f5-6dae-4e4e-9e4b-e3a15534f998 user=SA]
o.s.j.d.DataSourceTransactionManager     : Releasing JDBC Connection [HikariProxyConnection@1772902226 wrapping conn0: url=jdbc:h2:mem:ece222f5-6dae-4e4e-9e4b-e3a15534f998 user=SA] after transaction

h.springdb22.propagation.BasicTxTest     : 트랜잭션 2 시작
o.s.j.d.DataSourceTransactionManager     : Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
o.s.j.d.DataSourceTransactionManager     : Acquired Connection [HikariProxyConnection@444211664 wrapping conn0: url=jdbc:h2:mem:ece222f5-6dae-4e4e-9e4b-e3a15534f998 user=SA] for JDBC transaction
o.s.j.d.DataSourceTransactionManager     : Switching JDBC Connection [HikariProxyConnection@444211664 wrapping conn0: url=jdbc:h2:mem:ece222f5-6dae-4e4e-9e4b-e3a15534f998 user=SA] to manual commit
h.springdb22.propagation.BasicTxTest     : 트랜잭션 2 롤백
o.s.j.d.DataSourceTransactionManager     : Initiating transaction rollback
o.s.j.d.DataSourceTransactionManager     : Rolling back JDBC transaction on Connection [HikariProxyConnection@444211664 wrapping conn0: url=jdbc:h2:mem:ece222f5-6dae-4e4e-9e4b-e3a15534f998 user=SA]
o.s.j.d.DataSourceTransactionManager     : Releasing JDBC Connection [HikariProxyConnection@444211664 wrapping conn0: url=jdbc:h2:mem:ece222f5-6dae-4e4e-9e4b-e3a15534f998 user=SA] after transaction
```

#### 동작 방식

![img_2.png](img_2.png)

## 전파 기본

### 트랜잭션 전파

* 트랜잭션을 각각 사용하는 것이 아니라, 트랜잭션이 이미 진행중인데, 여기에 추가로 트랜잭션을 수행하면 어떻게 될까?
* 기존 트랜잭션과 별도의 트랜잭션을 진행해야 할까? 아니면 기존 트랜잭션을 그대로 이어 받아서 트랜잭션을 수행해야 할까?
* 이런 경우 어떻게 동작할지 결정하는 것을 트랜잭션 전파(propagation)라 한다.

### 물리 트랜잭션, 논리 트랜잭션

![img_5.png](img_5.png)

* 스프링은 이해를 돕기 위해 **논리 트랜잭션**과 **물리 트랜잭션**이라는 개념을 나눈다.
* 논리 트랜잭션들은 하나의 물리 트랜잭션으로 묶인다.
* **물리 트랜잭션**은 우리가 이해하는 실제 데이터베이스에 적용되는 트랜잭션을 뜻한다.
    * 실제 커넥션을 통해서 트랜잭션을 시작(`setAutoCommit(false)`)하고, 실제 커넥션을 통해서 커밋, 롤백하는 단위이다.
* **논리 트랜잭션**은 트랜잭션 매니저를 통해 트랜잭션을 사용하는 단위이다.
    * 이러한 논리 트랜잭션 개념은 트랜잭션이 진행되는 중에 내부에 추가로 트랜잭션을 사용하는 경우에 나타난다.
* 단순히 트랜잭션이 하나인 경우 둘을 구분하지는 않는다.

#### 원칙

* 모든 논리 트랜잭션이 커밋되어야 물리 트랜잭션이 커밋된다.
* 하나의 논리 트랜잭션이라도 롤백되면 물리 트랜잭션은 롤백된다.

### 기본 옵션 - REQUIRED

#### 외부 트랜잭션이 수행중인데, 내부 트랜잭션이 추가로 수행됨.

![img_3.png](img_3.png)

* 외부 트랜잭션이 수행중이고, 아직 끝나지 않았는데, 내부 트랜잭션이 수행된다.
* 외부 트랜잭션이라고 이름 붙인 것은 둘 중 상대적으로 밖에 있기 때문에 외부 트랜잭션이라 한다.
    * 처음 시작된 트랜잭션으로 이해하면 된다.
* 내부 트랜잭션은 외부에 트랜잭션이 수행되고 있는 도중에 호출되기 때문에 마치 내부에 있는 것 처럼 보여서 내부 트랜잭션이라 한다.

![img_4.png](img_4.png)

* 스프링은 이 경우 외부 트랜잭션과 내부 트랜잭션을 묶어서 하나의 트랜잭션을 만들어준다.
* 내부 트랜잭션이 외부 트랜잭션에 **참여**하는 것이다.

#### 1, 2 모두 커밋

![img_6.png](img_6.png)

* 모든 논리 트랜잭션이 커밋 되었으므로 물리 트랜잭션도 커밋된다.

#### 둘 중에 하나만 커밋, 하나는 롤백

![img_7.png](img_7.png)

![img_8.png](img_8.png)

* 논리 트랜잭션이 롤백 되었으므로 물리 트랜잭션은 **모두** 롤백된다.

## 전파 예제

## 외부 롤백

## 내부 롤백

## REQUIRES_NEW

## 다양한 전파 옵션