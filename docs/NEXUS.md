# Nexus — common artifact (snapshot/release shared)

> **Trạng thái hiện tại:** Chưa có Nexus trên local, `common` + `common-security` + `common-web` là **modules Maven local** trong cùng reactor. Build chung: `mvn -B install`.

## Có 3 tầng common

| Module | Artifact | Chứa gì |
|---|---|---|
| `common` | `common:0.0.1-SNAPSHOT` | `ApiResponse`, `PageResponse`, `ErrorCode`, `BusinessException`, `ResourceNotFoundException`, `GlobalExceptionHandler` |
| `common-security` | `common-security:0.0.1-SNAPSHOT` | `JwtRoleConverter` (Keycloak realm_access/resource_access/scope), `CommonSecurityAutoConfiguration` (auto `JwtAuthenticationConverter`), `SecuritySupport` helper |
| `common-web` | `common-web:0.0.1-SNAPSHOT` | `CorrelationIdFilter` (`X-Request-Id` + MDC), `CommonWebAutoConfiguration` |

Service kéo theo nhu cầu:
- Service thường: `common` + `common-security` + `common-web`.
- `api-gateway` (WebFlux) **không** kéo `common-web`/`common-security` Servlet.
- `discovery-service` chỉ cần `common` nếu muốn.

## Chạy local không Nexus

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21"
mvn -B clean install -pl common,common-security,common-web -am -DskipTests
# rồi build service kèm -am: mvn -B -pl services/<svc> -am package -DskipTests
```

* `Dockerfile` đã `COPY common` + `COPY common-security` + `COPY common-web` nên build image vẫn đủ.

## Khi chạy Nexus (VPS)

### 1. Triển Nexus

`docker-compose.yml` đã có service `nexus` (`sonatype/nexus3:3.78.0`) public `0.0.0.0:18081:8081`, volume `nexus-data`, healthcheck `/service/rest/v1/status`.

```bash
docker compose up -d nexus
# đợi healthy (~90s): docker inspect --format '{{.State.Health.Status}}' eccommerce-nexus-1
# UI: http://162.4.177.91:18081  (admin / xem /opt/sonatype-work/nexus3/admin.password lần đầu)
```

### 2. distributionManagement

`pom.xml` root đã đặt:

```xml
<nexus.url>http://162.4.177.91:18081</nexus.url>
<distributionManagement>
  <repository><id>nexus-releases</id><url>${nexus.url}/repository/maven-releases/</url></repository>
  <snapshotRepository><id>nexus-snapshots</id><url>${nexus.url}/repository/maven-snapshots/</url></snapshotRepository>
</distributionManagement>
```

Các module `common*` kế thừa nên không cần ghi lại.

### 3. settings.xml

Tạo `~/.m2/settings.xml` (không commit), thay `NEXUS_PASS`:

```xml
<settings>
  <servers>
    <server><id>nexus-releases</id><username>admin</username><password>NEXUS_PASS</password></server>
    <server><id>nexus-snapshots</id><username>admin</username><password>NEXUS_PASS</password></server>
  </servers>
  <mirrors>
    <!-- optional: proxy central qua Nexus group nếu đã tạo maven-public -->
  </mirrors>
</settings>
```

### 4. Deploy

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21"
# snapshot (0.0.1-SNAPSHOT -> nexus-snapshots)
mvn -B -pl common,common-security,common-web clean deploy -DskipTests
# release (đổi 0.0.1 -> nexus-releases)
mvn -B -pl common,common-security,common-web clean deploy -DskipTests -P release
```

Kiểm tra: `http://162.4.177.91:18081/#browse/browse:maven-snapshots:com%2Fdtthuan3%2Fecommerce`

### 5. Service build độc lập sau khi có Nexus

```xml
<!-- service/pom.xml -->
<dependency><groupId>com.dtthuan3.ecommerce</groupId><artifactId>common</artifactId></dependency>
<dependency><groupId>com.dtthuan3.ecommerce</groupId><artifactId>common-security</artifactId></dependency>
<dependency><groupId>com.dtthuan3.ecommerce</groupId><artifactId>common-web</artifactId></dependency>
```

Có thể bỏ `-am` (không cần build `common*` local nữa) vì đã resolve từ Nexus.

## common cung cấp

- `ApiResponse<T>` — envelope chuẩn `{success, code, message, data, timestamp}` + factory `ok/created/error`.
- `PageResponse<T>` — wrapper phân trang.
- `ErrorCode`, `BusinessException`, `ResourceNotFoundException`.
- `GlobalExceptionHandler` — `@RestControllerAdvice` tự động qua `AutoConfiguration.imports`, chỉ kích hoạt với Servlet stack (`@ConditionalOnWebApplication(SERVLET)`), không ảnh hưởng `api-gateway` (WebFlux) hay `discovery-service`.
- `JwtRoleConverter` / `SecuritySupport` — chuẩn Keycloak RBAC toàn hệ.

### Ví dụ chuẩn

```java
@GetMapping("/{id}")
public ApiResponse<ProductDto> getById(@PathVariable Long id) {
    ProductDto dto = service.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    return ApiResponse.ok(dto);
}

@PostMapping
public ResponseEntity<ApiResponse<ProductDto>> create(@Valid @RequestBody CreateProductRequest req) {
    ProductDto created = service.create(req);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
}

// service
if (stock < qty) throw new BusinessException(ErrorCode.CONFLICT, "Hết hàng");

// SecurityConfig dùng common-security
@Bean
SecurityFilterChain chain(HttpSecurity http, JwtAuthenticationConverter conv) throws Exception {
    return SecuritySupport.defaultChain(http, conv).build();
    // hoặc: SecuritySupport.withDeleteAdminOnly(http, conv, "/files", "/files/**")
}
```

Validation (`@Valid`) sẽ được `GlobalExceptionHandler` map thành `VALIDATION_ERROR` 400 với message `field: reason; ...`.

## Phiên bản
- Hiện tại: `0.0.1-SNAPSHOT` cho cả 3 module.
- Khi phát hành release: đổi `pom.xml` parent `0.0.1` và `mvn ... deploy` lên `maven-releases`.
