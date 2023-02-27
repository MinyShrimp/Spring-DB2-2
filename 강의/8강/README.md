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

## 스프링 트랜잭션 소개

## 트랜잭션 적용 확인

## 트랜잭션 적용 위치

## 트랜잭션 AOP 주의 사항 - 프로시 내부 호출 1

## 트랜잭션 AOP 주의 사항 - 프로시 내부 호출 2

## 트랜잭션 AOP 주의 사항 - 초기화 시점

## 트랜잭션 옵션 소개

## 예외와 트랜잭션 커밋, 롤백 - 기본

## 예외와 트랜잭션 커밋, 롤백 - 활용