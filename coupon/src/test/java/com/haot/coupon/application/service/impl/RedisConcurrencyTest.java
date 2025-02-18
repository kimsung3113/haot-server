package com.haot.coupon.application.service.impl;

import com.haot.coupon.application.dto.request.coupons.CouponCustomerCreateRequest;
import com.haot.coupon.application.kafka.CouponErrorProducer;
import com.haot.coupon.application.kafka.CouponIssueProducer;
import com.haot.coupon.application.service.CouponService;
import com.haot.coupon.common.exceptions.CustomCouponException;
import com.haot.coupon.domain.model.Coupon;
import com.haot.coupon.domain.model.CouponEvent;
import com.haot.coupon.infrastructure.repository.CouponEventRepository;
import com.haot.coupon.infrastructure.repository.CouponRepository;
import com.haot.coupon.utils.TestEntityFixture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@Testcontainers
@SpringBootTest
@Slf4j(topic = "RedisConcurrencyTest")
public class RedisConcurrencyTest {

    private static final int REDIS_PORT = 6379;
    private static final Network NETWORK = Network.newNetwork();

    @Container
    public static GenericContainer<?> redisContainer = new GenericContainer<>("redis:latest")
            .withExposedPorts(REDIS_PORT)
            .withNetwork(NETWORK)
            .withNetworkAliases("redis") // 컨테이너 이름 설정
            .waitingFor(Wait.forListeningPort());

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:13")
            .withExposedPorts(5432)
            .withNetwork(NETWORK)
            .withNetworkAliases("db")
            .withDatabaseName("how_about_over_there")
            .withUsername("testuser")
            .withPassword("testpassword");

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(
                redisContainer.getHost(),
                redisContainer.getMappedPort(REDIS_PORT)
        );
    }

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponEventRepository couponEventRepository;

    @Autowired
    private RedisTemplate<String, Integer> countRedisTemplate;

    @MockBean
    private CouponIssueProducer couponIssueProducer; // Kafka 메시지 Mocking

    @MockBean
    private CouponErrorProducer couponErrorProducer;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(REDIS_PORT));

        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @BeforeAll
    static void setup() throws Exception{
        log.info("Redis Host: {}", redisContainer.getHost());
        log.info("Redis Port: {}", redisContainer.getMappedPort(REDIS_PORT)); // 동적으로 6379 port와 연동된 port

        redisContainer.start();
        postgreSQLContainer.start();

        // JDBC URL을 통해 PostgreSQL에 직접 연결하여 SQL 쿼리 실행
        try (Connection connection = DriverManager.getConnection(
                postgreSQLContainer.getJdbcUrl(),
                postgreSQLContainer.getUsername(),
                postgreSQLContainer.getPassword())) {

            // SQL 쿼리 실행
            try (Statement stmt = connection.createStatement()) {
                // coupon 스키마 생성 및 테이블 추가
                String createSchemaSql = "CREATE SCHEMA IF NOT EXISTS coupon;";

                stmt.execute(createSchemaSql);
            }
        }
    }

    private CouponCustomerCreateRequest setData(){

        Coupon coupon = TestEntityFixture.createPriorityCoupon();
        CouponEvent event = TestEntityFixture.createCouponEvent(coupon);

        Coupon savedCoupon = couponRepository.save(coupon);
        CouponEvent savedEvent = couponEventRepository.save(event);

        return CouponCustomerCreateRequest.builder()
                .eventId(savedEvent.getId())
                .couponId(savedCoupon.getId())
                .build();
    }

    @Test
    void testRedisConcurrency() throws InterruptedException {

        doNothing().when(couponIssueProducer).sendIssuePriorityCoupon(any());
        doNothing().when(couponErrorProducer).sendEventClosed(any());

        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        CouponCustomerCreateRequest request = setData();

        String redisKey = "event:" + request.eventId() + "c:" + request.couponId();

        // 초기 Redis 설정
        countRedisTemplate.opsForValue().set(redisKey, 3);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i + 1;
            executorService.submit(() -> {
                try {
                    String userId = UUID.randomUUID().toString();
                    couponService.customerIssueCoupon(request, userId);
                    successCount.incrementAndGet();
                    log.info("쿠폰 발급 {}thread 성공", threadNumber);
                } catch (CustomCouponException exception) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드 종료 대기

        // Redis에서 남은 재고 확인
        Integer remainingStock = countRedisTemplate.opsForValue().get(redisKey);
        log.info("남은 쿠폰 개수: {}", remainingStock);

        // Then: 결과 확인
        log.info("쿠폰 발급 개수 : {}", successCount.get());
        log.info("쿠폰 발급 에러 횟수 : {}", failCount.get());

        assertEquals(3, successCount.get(), "3개의 쓰레드만 성공");
        assertEquals(997, failCount.get(), "997개의 쓰레드 실패");
        assertThat(remainingStock).isGreaterThanOrEqualTo(0); // 수량이 0 이하로 내려가지 않아야 함

        // 스레드 풀이 종료되었을 때 리소스를 해제
        executorService.shutdown();
    }
}
