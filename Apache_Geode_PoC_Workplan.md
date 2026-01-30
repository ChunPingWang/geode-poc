# Apache Geode Proof of Concept

## 工作清單與架構設計

**部署環境：** Kind Kubernetes Cluster  
**版本：** 1.0  
**日期：** 2026年1月

---

## 目錄

1. [專案概述](#1-專案概述)
2. [系統架構設計](#2-系統架構設計)
3. [基礎環境建置](#3-基礎環境建置)
4. [PoC 情境一：高速快取與資料網格](#4-poc-情境一高速快取與資料網格)
5. [PoC 情境二：即時交易處理系統](#5-poc-情境二即時交易處理系統)
6. [PoC 情境三：分散式 Session 管理](#6-poc-情境三分散式-session-管理)
7. [PoC 情境四：事件驅動架構 (CQ)](#7-poc-情境四事件驅動架構-cq)
8. [PoC 情境五：地理分散式部署](#8-poc-情境五地理分散式部署-wan-replication)
9. [監控與運維](#9-監控與運維)
10. [工作清單總覽](#10-工作清單總覽)

---

## 1. 專案概述

### 1.1 專案目標

本 PoC 專案旨在驗證 Apache Geode 在企業級應用場景中的可行性，包含：

- 評估 Geode 作為分散式記憶體資料網格的效能表現
- 驗證與 Spring Boot 微服務架構的整合能力
- 測試高可用性與故障轉移機制
- 評估 Kubernetes 環境下的部署與運維可行性

### 1.2 適用情境

| 情境 | 應用場景 | 預期效益 |
|------|----------|----------|
| 高速快取 | 客戶資料、帳戶餘額、產品定價 | 微秒級延遲，減少資料庫負載 |
| 交易處理 | 即時支付、信用卡授權、風險計算 | 高吞吐量，ACID 交易保證 |
| Session 管理 | 微服務 Session 共享 | 無狀態部署，水平擴展 |
| 事件驅動 | 信用額度變動通知、觸發處理 | 即時反應，降低輪詢成本 |
| 地理分散 | 跨資料中心同步、災難復原 | 業務連續性，資料一致性 |

---

## 2. 系統架構設計

### 2.1 整體架構圖

本 PoC 採用 Kind (Kubernetes in Docker) 作為本地開發環境，模擬生產級 Kubernetes 叢集。

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Kind Kubernetes Cluster                          │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                    Ingress Controller (NGINX)                      │  │
│  │                         Port: 80, 443                              │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                    │                                     │
│          ┌─────────────────────────┼─────────────────────────┐          │
│          │                         │                         │          │
│          ▼                         ▼                         ▼          │
│  ┌───────────────┐        ┌───────────────┐        ┌───────────────┐   │
│  │  Namespace:   │        │  Namespace:   │        │  Namespace:   │   │
│  │    geode      │        │     apps      │        │  monitoring   │   │
│  ├───────────────┤        ├───────────────┤        ├───────────────┤   │
│  │               │        │               │        │               │   │
│  │ ┌───────────┐ │        │ ┌───────────┐ │        │ ┌───────────┐ │   │
│  │ │ Locator-0 │ │        │ │  App-1    │ │        │ │Prometheus │ │   │
│  │ └───────────┘ │        │ └───────────┘ │        │ └───────────┘ │   │
│  │ ┌───────────┐ │        │ ┌───────────┐ │        │ ┌───────────┐ │   │
│  │ │ Locator-1 │ │◄──────►│ │  App-2    │ │        │ │  Grafana  │ │   │
│  │ └───────────┘ │        │ └───────────┘ │        │ └───────────┘ │   │
│  │               │        │ ┌───────────┐ │        │               │   │
│  │ ┌───────────┐ │        │ │  App-3    │ │        │               │   │
│  │ │ Server-0  │ │        │ └───────────┘ │        │               │   │
│  │ └───────────┘ │        │               │        │               │   │
│  │ ┌───────────┐ │        │               │        │               │   │
│  │ │ Server-1  │ │        │               │        │               │   │
│  │ └───────────┘ │        │               │        │               │   │
│  │ ┌───────────┐ │        │               │        │               │   │
│  │ │ Server-2  │ │        │               │        │               │   │
│  │ └───────────┘ │        │               │        │               │   │
│  │ ┌───────────┐ │        │               │        │               │   │
│  │ │ Server-3  │ │        │               │        │               │   │
│  │ └───────────┘ │        │               │        │               │   │
│  │               │        │               │        │               │   │
│  │ ┌───────────┐ │        │               │        │               │   │
│  │ │  Pulse    │ │        │               │        │               │   │
│  │ │  Web UI   │ │        │               │        │               │   │
│  │ └───────────┘ │        │               │        │               │   │
│  └───────────────┘        └───────────────┘        └───────────────┘   │
│                                                                          │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                    Persistent Volumes (hostPath)                   │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 元件說明

| 元件 | 說明 |
|------|------|
| Kind Cluster | 本地 Kubernetes 叢集，3 個 worker nodes 模擬多節點環境 |
| NGINX Ingress | 統一入口，處理外部請求路由與負載平衡 |
| Geode Locator | 叢集協調者，負責成員發現與負載平衡 (2 replicas for HA) |
| Geode Server | 資料節點，儲存與處理資料 (4 replicas，支援 partition 與 replication) |
| Spring Boot Apps | 範例應用程式，展示各種整合情境 |
| Prometheus | 監控指標收集與告警 |
| Grafana | 監控儀表板視覺化 |

### 2.3 網路架構

Kubernetes 服務設計：

- **geode-locator-headless**：Headless Service，用於 Locator 間通訊
- **geode-locator**：ClusterIP Service，應用程式連線入口
- **geode-server-headless**：Headless Service，用於 Server 資料同步
- **geode-pulse**：NodePort Service，Pulse Web UI 存取

### 2.4 Geode 叢集拓樸

```
                    ┌─────────────────────────────────────┐
                    │           Client Applications        │
                    └─────────────────┬───────────────────┘
                                      │
                    ┌─────────────────▼───────────────────┐
                    │            Locators (HA)             │
                    │  ┌─────────────┐ ┌─────────────┐    │
                    │  │  Locator-0  │ │  Locator-1  │    │
                    │  │  Port:10334 │ │  Port:10334 │    │
                    │  └─────────────┘ └─────────────┘    │
                    └─────────────────┬───────────────────┘
                                      │
        ┌─────────────┬───────────────┼───────────────┬─────────────┐
        │             │               │               │             │
        ▼             ▼               ▼               ▼             ▼
┌─────────────┐┌─────────────┐┌─────────────┐┌─────────────┐
│  Server-0   ││  Server-1   ││  Server-2   ││  Server-3   │
│  Port:40404 ││  Port:40404 ││  Port:40404 ││  Port:40404 │
│             ││             ││             ││             │
│ ┌─────────┐ ││ ┌─────────┐ ││ ┌─────────┐ ││ ┌─────────┐ │
│ │Region-A │ ││ │Region-A │ ││ │Region-A │ ││ │Region-A │ │
│ │(Primary)│ ││ │(Backup) │ ││ │(Primary)│ ││ │(Backup) │ │
│ └─────────┘ ││ └─────────┘ ││ └─────────┘ ││ └─────────┘ │
│ ┌─────────┐ ││ ┌─────────┐ ││ ┌─────────┐ ││ ┌─────────┐ │
│ │Region-B │ ││ │Region-B │ ││ │Region-B │ ││ │Region-B │ │
│ │(Backup) │ ││ │(Primary)│ ││ │(Backup) │ ││ │(Primary)│ │
│ └─────────┘ ││ └─────────┘ ││ └─────────┘ ││ └─────────┘ │
└─────────────┘└─────────────┘└─────────────┘└─────────────┘
```

---

## 3. 基礎環境建置

### 3.1 前置需求

| 工具 | 版本 | 用途 |
|------|------|------|
| Docker Desktop | 24.0+ | 容器執行環境 |
| Kind | 0.20+ | 本地 Kubernetes 叢集 |
| kubectl | 1.28+ | Kubernetes CLI |
| Helm | 3.12+ | Kubernetes 套件管理 |
| Java JDK | 17+ | Spring Boot 應用開發 |
| Maven | 3.9+ | 專案建置 |

### 3.2 工作項目

| # | 任務 | 預估時間 | 狀態 |
|---|------|----------|------|
| 1 | 安裝並設定 Docker Desktop | 30 分鐘 | ⬜ 待執行 |
| 2 | 安裝 Kind CLI | 10 分鐘 | ⬜ 待執行 |
| 3 | 建立 Kind 叢集設定檔 (3 workers, port mappings) | 20 分鐘 | ⬜ 待執行 |
| 4 | 啟動 Kind 叢集 | 5 分鐘 | ⬜ 待執行 |
| 5 | 安裝 NGINX Ingress Controller | 15 分鐘 | ⬜ 待執行 |
| 6 | 驗證 Ingress 設定 | 10 分鐘 | ⬜ 待執行 |
| 7 | 建立 Geode namespace 與 RBAC 設定 | 15 分鐘 | ⬜ 待執行 |
| 8 | 部署 Geode Locator StatefulSet | 30 分鐘 | ⬜ 待執行 |
| 9 | 部署 Geode Server StatefulSet | 30 分鐘 | ⬜ 待執行 |
| 10 | 設定 Geode Pulse Web UI | 15 分鐘 | ⬜ 待執行 |
| 11 | 驗證叢集健康狀態 | 20 分鐘 | ⬜ 待執行 |

### 3.3 Kind 叢集設定檔

```yaml
# kind-config.yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: geode-poc
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP
      - containerPort: 30080
        hostPort: 30080
        protocol: TCP
  - role: worker
  - role: worker
  - role: worker
```

### 3.4 Geode Locator StatefulSet

```yaml
# geode-locator.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: geode-locator
  namespace: geode
spec:
  serviceName: geode-locator-headless
  replicas: 2
  selector:
    matchLabels:
      app: geode-locator
  template:
    metadata:
      labels:
        app: geode-locator
    spec:
      containers:
        - name: locator
          image: apachegeode/geode:1.15.1
          command:
            - /bin/bash
            - -c
            - |
              gfsh start locator \
                --name=${HOSTNAME} \
                --hostname-for-clients=${HOSTNAME}.geode-locator-headless.geode.svc.cluster.local \
                --port=10334 \
                --jmx-manager-start=true \
                --jmx-manager-port=1099 \
                --J=-Dgemfire.http-service-port=7070
          ports:
            - containerPort: 10334
              name: locator
            - containerPort: 1099
              name: jmx
            - containerPort: 7070
              name: pulse
          resources:
            requests:
              memory: "1Gi"
              cpu: "500m"
            limits:
              memory: "2Gi"
              cpu: "1000m"
```

### 3.5 Geode Server StatefulSet

```yaml
# geode-server.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: geode-server
  namespace: geode
spec:
  serviceName: geode-server-headless
  replicas: 4
  selector:
    matchLabels:
      app: geode-server
  template:
    metadata:
      labels:
        app: geode-server
    spec:
      containers:
        - name: server
          image: apachegeode/geode:1.15.1
          command:
            - /bin/bash
            - -c
            - |
              gfsh start server \
                --name=${HOSTNAME} \
                --locators=geode-locator-0.geode-locator-headless.geode.svc.cluster.local[10334],geode-locator-1.geode-locator-headless.geode.svc.cluster.local[10334] \
                --hostname-for-clients=${HOSTNAME}.geode-server-headless.geode.svc.cluster.local \
                --server-port=40404 \
                --max-heap=2g
          ports:
            - containerPort: 40404
              name: server
          resources:
            requests:
              memory: "2Gi"
              cpu: "1000m"
            limits:
              memory: "4Gi"
              cpu: "2000m"
```

---

## 4. PoC 情境一：高速快取與資料網格

### 4.1 情境說明

模擬銀行核心系統的客戶資料快取場景，包含客戶基本資料、帳戶餘額、產品定價等頻繁存取的資料。目標是實現微秒級讀取延遲，並降低後端資料庫的查詢負載。

### 4.2 架構設計

- **Region 類型**：PARTITION_REDUNDANT（分散式儲存 + 備援）
- **資料過期策略**：TTL-based expiration（可設定）
- **序列化**：PDX Serialization（跨語言相容）
- **整合方式**：Spring Data Geode Repository

```
┌─────────────────────────────────────────────────────────────────┐
│                     Cache-Aside Pattern                          │
└─────────────────────────────────────────────────────────────────┘

    ┌──────────┐         ┌──────────────┐         ┌──────────┐
    │  Client  │────1───►│ Spring Boot  │────2───►│  Geode   │
    │          │◄───6────│   Service    │◄───3────│  Cache   │
    └──────────┘         └──────────────┘         └──────────┘
                                │                       │
                                │ 4 (cache miss)        │
                                ▼                       │
                         ┌──────────────┐               │
                         │   Database   │               │
                         │  (PostgreSQL)│               │
                         └──────────────┘               │
                                │                       │
                                └───────5 (put)─────────┘

Flow:
1. Client requests data
2. Service checks Geode cache
3. Cache hit → return data (go to 6)
4. Cache miss → query database
5. Put result into cache
6. Return data to client
```

### 4.3 工作項目

| # | 任務 | 預估時間 | 狀態 |
|---|------|----------|------|
| 1 | 建立 Customer 實體類別與 PDX 序列化設定 | 1 小時 | ⬜ 待執行 |
| 2 | 建立 Account 實體類別 | 45 分鐘 | ⬜ 待執行 |
| 3 | 建立 Product 定價實體類別 | 45 分鐘 | ⬜ 待執行 |
| 4 | 設定 PARTITION_REDUNDANT Region | 30 分鐘 | ⬜ 待執行 |
| 5 | 實作 Spring Data Geode Repository | 1 小時 | ⬜ 待執行 |
| 6 | 實作 Cache-Aside Pattern Service Layer | 1.5 小時 | ⬜ 待執行 |
| 7 | 建立 REST API 端點 | 1 小時 | ⬜ 待執行 |
| 8 | 設定 TTL 過期策略 | 30 分鐘 | ⬜ 待執行 |
| 9 | 效能測試：讀取延遲驗證 | 2 小時 | ⬜ 待執行 |
| 10 | 效能測試：吞吐量驗證 | 2 小時 | ⬜ 待執行 |
| 11 | 撰寫測試報告 | 1 小時 | ⬜ 待執行 |

### 4.4 範例程式碼

#### Customer 實體

```java
@Region("Customers")
public class Customer implements PdxSerializable {
    
    @Id
    private String customerId;
    private String name;
    private String email;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Override
    public void toData(PdxWriter writer) {
        writer.writeString("customerId", customerId)
              .writeString("name", name)
              .writeString("email", email)
              .writeString("phone", phone)
              .writeDate("createdAt", Date.from(createdAt.atZone(ZoneId.systemDefault()).toInstant()))
              .writeDate("updatedAt", Date.from(updatedAt.atZone(ZoneId.systemDefault()).toInstant()));
    }
    
    @Override
    public void fromData(PdxReader reader) {
        this.customerId = reader.readString("customerId");
        this.name = reader.readString("name");
        this.email = reader.readString("email");
        this.phone = reader.readString("phone");
        this.createdAt = reader.readDate("createdAt").toInstant()
                              .atZone(ZoneId.systemDefault()).toLocalDateTime();
        this.updatedAt = reader.readDate("updatedAt").toInstant()
                              .atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
```

#### Spring Data Geode Repository

```java
@Repository
public interface CustomerRepository extends CrudRepository<Customer, String> {
    
    @Query("SELECT * FROM /Customers c WHERE c.email = $1")
    Optional<Customer> findByEmail(String email);
    
    @Query("SELECT * FROM /Customers c WHERE c.name LIKE $1")
    List<Customer> findByNameContaining(String name);
}
```

#### Cache-Aside Service

```java
@Service
@RequiredArgsConstructor
public class CustomerService {
    
    private final CustomerRepository cacheRepository;
    private final CustomerJpaRepository dbRepository;
    
    public Customer getCustomer(String customerId) {
        // 1. Check cache first
        return cacheRepository.findById(customerId)
            .orElseGet(() -> {
                // 2. Cache miss - query database
                Customer customer = dbRepository.findById(customerId)
                    .orElseThrow(() -> new CustomerNotFoundException(customerId));
                
                // 3. Put into cache
                cacheRepository.save(customer);
                
                return customer;
            });
    }
    
    @Transactional
    public Customer updateCustomer(String customerId, CustomerUpdateRequest request) {
        // 1. Update database
        Customer customer = dbRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
        
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setUpdatedAt(LocalDateTime.now());
        
        Customer saved = dbRepository.save(customer);
        
        // 2. Update cache (write-through)
        cacheRepository.save(saved);
        
        return saved;
    }
}
```

### 4.5 Region 設定

```java
@Configuration
@EnableGemfireRepositories(basePackages = "com.example.repository")
public class GeodeConfig {
    
    @Bean("Customers")
    public PartitionedRegionFactoryBean<String, Customer> customersRegion(
            GemFireCache cache) {
        
        PartitionedRegionFactoryBean<String, Customer> region = 
            new PartitionedRegionFactoryBean<>();
        
        region.setCache(cache);
        region.setName("Customers");
        region.setPersistent(false);
        
        PartitionAttributesFactoryBean partitionAttributes = 
            new PartitionAttributesFactoryBean();
        partitionAttributes.setRedundantCopies(1);
        partitionAttributes.setTotalNumBuckets(113);
        
        region.setPartitionAttributes(partitionAttributes.getObject());
        
        // TTL expiration
        ExpirationAttributesFactoryBean expiration = 
            new ExpirationAttributesFactoryBean();
        expiration.setTimeout(3600); // 1 hour
        expiration.setAction(ExpirationAction.INVALIDATE);
        
        region.setEntryTimeToLive(expiration.getObject());
        
        return region;
    }
}
```

### 4.6 驗收標準

| 指標 | 目標值 |
|------|--------|
| 單筆讀取延遲 | < 1ms (P99) |
| 批次讀取 (100筆) | < 10ms |
| 寫入延遲 | < 5ms (P99) |
| 節點故障 | 自動 failover，資料不遺失 |

---

## 5. PoC 情境二：即時交易處理系統

### 5.1 情境說明

模擬即時支付或信用卡授權場景，要求高吞吐量、低延遲，並確保 ACID 交易特性。包含帳戶餘額扣款、交易記錄寫入等操作，需要在分散式環境下保證資料一致性。

### 5.2 架構設計

- **交易管理**：Geode Transaction API
- **一致性層級**：REPEATABLE_READ Isolation
- **分散式鎖**：Pessimistic Locking Pattern
- **資料模型**：帳戶 + 交易記錄（雙寫）

```
┌─────────────────────────────────────────────────────────────────┐
│                   Transaction Processing Flow                    │
└─────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │   Payment    │
    │   Request    │
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐     ┌─────────────────────────────────────┐
    │   Validate   │────►│  Validation Failed → Reject         │
    │   Request    │     └─────────────────────────────────────┘
    └──────┬───────┘
           │ Valid
           ▼
    ┌──────────────┐
    │ Begin Geode  │
    │ Transaction  │
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐     ┌─────────────────────────────────────┐
    │   Acquire    │────►│  Lock Timeout → Retry/Reject        │
    │ Account Lock │     └─────────────────────────────────────┘
    └──────┬───────┘
           │ Lock Acquired
           ▼
    ┌──────────────┐     ┌─────────────────────────────────────┐
    │    Check     │────►│  Insufficient → Rollback + Reject   │
    │   Balance    │     └─────────────────────────────────────┘
    └──────┬───────┘
           │ Sufficient
           ▼
    ┌──────────────┐
    │    Debit     │
    │   Account    │
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │    Write     │
    │ Transaction  │
    │    Record    │
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │   Commit     │
    │ Transaction  │
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │   Release    │
    │    Lock      │
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │   Return     │
    │   Success    │
    └──────────────┘
```

### 5.3 工作項目

| # | 任務 | 預估時間 | 狀態 |
|---|------|----------|------|
| 1 | 設計交易處理領域模型 (DDD) | 2 小時 | ⬜ 待執行 |
| 2 | 建立 TransactionRequest / TransactionResult 實體 | 1 小時 | ⬜ 待執行 |
| 3 | 實作 AccountService 扣款邏輯 | 2 小時 | ⬜ 待執行 |
| 4 | 設定 Geode Transaction Manager | 1 小時 | ⬜ 待執行 |
| 5 | 實作分散式鎖機制 (Pessimistic Locking) | 2 小時 | ⬜ 待執行 |
| 6 | 實作交易補償 (Rollback) 邏輯 | 1.5 小時 | ⬜ 待執行 |
| 7 | 建立 REST API 交易端點 | 1 小時 | ⬜ 待執行 |
| 8 | 並發測試：Race Condition 驗證 | 3 小時 | ⬜ 待執行 |
| 9 | 效能測試：TPS 吞吐量驗證 | 2 小時 | ⬜ 待執行 |
| 10 | 撰寫測試報告 | 1 小時 | ⬜ 待執行 |

### 5.4 範例程式碼

#### 領域模型

```java
@Region("Accounts")
@Data
@Builder
public class Account implements PdxSerializable {
    @Id
    private String accountId;
    private String customerId;
    private BigDecimal balance;
    private BigDecimal creditLimit;
    private AccountStatus status;
    private long version;
    
    public void debit(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(accountId, balance, amount);
        }
        this.balance = this.balance.subtract(amount);
        this.version++;
    }
    
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.version++;
    }
}

@Region("Transactions")
@Data
@Builder
public class PaymentTransaction implements PdxSerializable {
    @Id
    private String transactionId;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private TransactionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String failureReason;
}
```

#### 交易服務

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final Region<String, Account> accountRegion;
    private final Region<String, PaymentTransaction> transactionRegion;
    private final CacheTransactionManager txManager;
    
    public PaymentResult processPayment(PaymentRequest request) {
        String txId = UUID.randomUUID().toString();
        
        PaymentTransaction transaction = PaymentTransaction.builder()
            .transactionId(txId)
            .fromAccountId(request.getFromAccountId())
            .toAccountId(request.getToAccountId())
            .amount(request.getAmount())
            .status(TransactionStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();
        
        try {
            // Begin Geode transaction
            txManager.begin();
            
            // Pessimistic lock - get with lock
            Account fromAccount = accountRegion.get(request.getFromAccountId());
            Account toAccount = accountRegion.get(request.getToAccountId());
            
            if (fromAccount == null || toAccount == null) {
                throw new AccountNotFoundException("Account not found");
            }
            
            // Business logic
            fromAccount.debit(request.getAmount());
            toAccount.credit(request.getAmount());
            
            // Update accounts
            accountRegion.put(fromAccount.getAccountId(), fromAccount);
            accountRegion.put(toAccount.getAccountId(), toAccount);
            
            // Record transaction
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRegion.put(txId, transaction);
            
            // Commit
            txManager.commit();
            
            log.info("Payment {} completed successfully", txId);
            return PaymentResult.success(txId);
            
        } catch (Exception e) {
            // Rollback on any error
            if (txManager.exists()) {
                txManager.rollback();
            }
            
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRegion.put(txId, transaction);
            
            log.error("Payment {} failed: {}", txId, e.getMessage());
            return PaymentResult.failure(txId, e.getMessage());
        }
    }
}
```

### 5.5 驗收標準

| 指標 | 目標值 |
|------|--------|
| 交易處理 TPS | > 10,000 |
| 交易延遲 | < 10ms (P99) |
| 並發衝突 | 正確處理，無資料不一致 |
| 交易失敗 | 正確 Rollback |

---

## 6. PoC 情境三：分散式 Session 管理

### 6.1 情境說明

在微服務架構下，實現分散式 Session 儲存，使多個服務實例可共享使用者 Session，支援無狀態部署與水平擴展。整合 Spring Session 框架，對應用程式透明化。

### 6.2 架構設計

- **整合框架**：Spring Session + Geode
- **Session 儲存**：REPLICATE Region（全節點複製）
- **過期策略**：Idle Timeout + Absolute Timeout
- **序列化**：Java Serialization / JSON

```
┌─────────────────────────────────────────────────────────────────┐
│                 Distributed Session Architecture                 │
└─────────────────────────────────────────────────────────────────┘

                         Load Balancer
                              │
           ┌──────────────────┼──────────────────┐
           │                  │                  │
           ▼                  ▼                  ▼
    ┌────────────┐     ┌────────────┐     ┌────────────┐
    │   App-1    │     │   App-2    │     │   App-3    │
    │            │     │            │     │            │
    │ ┌────────┐ │     │ ┌────────┐ │     │ ┌────────┐ │
    │ │ Spring │ │     │ │ Spring │ │     │ │ Spring │ │
    │ │Session │ │     │ │Session │ │     │ │Session │ │
    │ └────┬───┘ │     │ └────┬───┘ │     │ └────┬───┘ │
    └──────┼─────┘     └──────┼─────┘     └──────┼─────┘
           │                  │                  │
           └──────────────────┼──────────────────┘
                              │
                              ▼
    ┌─────────────────────────────────────────────────────────────┐
    │                    Geode Cluster                             │
    │  ┌──────────────────────────────────────────────────────┐   │
    │  │              Session Region (REPLICATE)               │   │
    │  │                                                       │   │
    │  │   Server-0      Server-1      Server-2      Server-3  │   │
    │  │  ┌────────┐    ┌────────┐    ┌────────┐    ┌────────┐│   │
    │  │  │Session │    │Session │    │Session │    │Session ││   │
    │  │  │ Data   │◄──►│ Data   │◄──►│ Data   │◄──►│ Data   ││   │
    │  │  │(Full)  │    │(Full)  │    │(Full)  │    │(Full)  ││   │
    │  │  └────────┘    └────────┘    └────────┘    └────────┘│   │
    │  └──────────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────────┘
```

### 6.3 工作項目

| # | 任務 | 預估時間 | 狀態 |
|---|------|----------|------|
| 1 | 設定 Spring Session Geode 依賴 | 30 分鐘 | ⬜ 待執行 |
| 2 | 設定 Session Region (REPLICATE) | 30 分鐘 | ⬜ 待執行 |
| 3 | 實作 Session 儲存/讀取 Service | 1 小時 | ⬜ 待執行 |
| 4 | 設定 Session 過期策略 | 30 分鐘 | ⬜ 待執行 |
| 5 | 建立範例登入/登出 API | 1 小時 | ⬜ 待執行 |
| 6 | 部署多個應用實例 (3 replicas) | 30 分鐘 | ⬜ 待執行 |
| 7 | 驗證跨實例 Session 共享 | 1 小時 | ⬜ 待執行 |
| 8 | 測試應用實例故障時 Session 不中斷 | 1 小時 | ⬜ 待執行 |
| 9 | 撰寫測試報告 | 1 小時 | ⬜ 待執行 |

### 6.4 範例程式碼

#### Maven 依賴

```xml
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-geode</artifactId>
    <version>3.1.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.geode</groupId>
    <artifactId>spring-geode-starter</artifactId>
    <version>1.7.0</version>
</dependency>
```

#### Session 設定

```java
@Configuration
@EnableGemFireHttpSession(
    regionName = "Sessions",
    maxInactiveIntervalInSeconds = 1800,  // 30 minutes idle timeout
    poolName = "DEFAULT"
)
public class SessionConfig {
    
    @Bean
    public ReplicatedRegionFactoryBean<String, Session> sessionsRegion(
            GemFireCache cache) {
        
        ReplicatedRegionFactoryBean<String, Session> region = 
            new ReplicatedRegionFactoryBean<>();
        
        region.setCache(cache);
        region.setName("Sessions");
        region.setScope(Scope.DISTRIBUTED_ACK);
        
        return region;
    }
}
```

#### 登入控制器

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpSession session) {
        
        // Authenticate user (simplified)
        User user = authenticateUser(request);
        
        // Store user info in session
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("roles", user.getRoles());
        session.setAttribute("loginTime", LocalDateTime.now());
        
        return ResponseEntity.ok(LoginResponse.builder()
            .sessionId(session.getId())
            .username(user.getUsername())
            .build());
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserInfo> getCurrentUser(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        return ResponseEntity.ok(UserInfo.builder()
            .userId(userId)
            .username((String) session.getAttribute("username"))
            .roles((Set<String>) session.getAttribute("roles"))
            .build());
    }
}
```

### 6.5 驗收標準

| 指標 | 目標值 |
|------|--------|
| Session 共享 | 可在任意應用實例存取 |
| 實例重啟 | Session 不遺失 |
| Session 過期 | 正確過期並清理 |
| 複雜物件 | 支援 Session 內儲存複雜物件 |

---

## 7. PoC 情境四：事件驅動架構 (CQ)

### 7.1 情境說明

利用 Geode 的 Continuous Query (CQ) 功能，實現事件驅動架構。當資料滿足特定條件時（如信用額度變動），自動觸發通知或後續處理流程，降低輪詢成本，實現即時反應。

### 7.2 架構設計

- **事件機制**：Continuous Query (CQ)
- **查詢語言**：OQL (Object Query Language)
- **事件處理**：CqListener 回呼
- **整合**：與 Kafka 結合實現事件發布

```
┌─────────────────────────────────────────────────────────────────┐
│                  Event-Driven Architecture with CQ               │
└─────────────────────────────────────────────────────────────────┘

┌─────────────┐
│  Producer   │
│  Service    │
└──────┬──────┘
       │ PUT/UPDATE
       ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Geode Cluster                             │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                   CreditLimit Region                        │ │
│  │                                                             │ │
│  │   Entry Updated: creditLimit > threshold                    │ │
│  │                         │                                   │ │
│  │                         ▼                                   │ │
│  │   ┌─────────────────────────────────────────────────────┐  │ │
│  │   │  Continuous Query Engine                             │  │ │
│  │   │                                                      │  │ │
│  │   │  CQ: "SELECT * FROM /CreditLimits c                 │  │ │
│  │   │       WHERE c.newLimit > c.oldLimit * 1.5"          │  │ │
│  │   └─────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              │                                   │
│                              │ CQ Event                          │
│                              ▼                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                      CqListener                             │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   Kafka Producer    │
                    └──────────┬──────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Kafka Cluster                            │
│                                                                  │
│  Topic: credit-limit-changes                                    │
│  ┌────────┬────────┬────────┬────────┬────────┐                │
│  │ Part-0 │ Part-1 │ Part-2 │ Part-3 │ Part-4 │                │
│  └────────┴────────┴────────┴────────┴────────┘                │
└──────────────────────────────┬──────────────────────────────────┘
                               │
           ┌───────────────────┼───────────────────┐
           │                   │                   │
           ▼                   ▼                   ▼
    ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
    │ Notification│     │    Risk     │     │   Audit     │
    │   Service   │     │   Service   │     │   Service   │
    └─────────────┘     └─────────────┘     └─────────────┘
```

### 7.3 工作項目

| # | 任務 | 預估時間 | 狀態 |
|---|------|----------|------|
| 1 | 設計 CreditLimit 領域模型 | 1 小時 | ⬜ 待執行 |
| 2 | 建立 CreditLimit Region | 30 分鐘 | ⬜ 待執行 |
| 3 | 撰寫 OQL 查詢條件 (額度變動偵測) | 1 小時 | ⬜ 待執行 |
| 4 | 實作 CqListener 事件處理器 | 1.5 小時 | ⬜ 待執行 |
| 5 | 整合 Kafka Producer 發送事件 | 2 小時 | ⬜ 待執行 |
| 6 | 實作下游 Kafka Consumer | 1.5 小時 | ⬜ 待執行 |
| 7 | 建立 REST API 模擬額度變更 | 1 小時 | ⬜ 待執行 |
| 8 | 驗證事件即時觸發 | 1 小時 | ⬜ 待執行 |
| 9 | 測試高頻變更下的事件處理 | 2 小時 | ⬜ 待執行 |
| 10 | 撰寫測試報告 | 1 小時 | ⬜ 待執行 |

### 7.4 範例程式碼

#### CreditLimit 實體

```java
@Region("CreditLimits")
@Data
@Builder
public class CreditLimit implements PdxSerializable {
    @Id
    private String accountId;
    private String customerId;
    private BigDecimal currentLimit;
    private BigDecimal previousLimit;
    private BigDecimal utilizationAmount;
    private LocalDateTime lastUpdated;
    private String updatedBy;
    
    public BigDecimal getUtilizationRate() {
        if (currentLimit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return utilizationAmount.divide(currentLimit, 4, RoundingMode.HALF_UP);
    }
    
    public boolean isSignificantIncrease() {
        if (previousLimit.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }
        BigDecimal increaseRate = currentLimit.subtract(previousLimit)
            .divide(previousLimit, 4, RoundingMode.HALF_UP);
        return increaseRate.compareTo(new BigDecimal("0.5")) > 0;
    }
}
```

#### CQ 設定與 Listener

```java
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ContinuousQueryConfig {
    
    private final KafkaTemplate<String, CreditLimitEvent> kafkaTemplate;
    
    @Bean
    public CqQuery creditLimitCq(QueryService queryService) throws CqException {
        String queryString = 
            "SELECT * FROM /CreditLimits c " +
            "WHERE c.currentLimit > c.previousLimit * 1.5";
        
        CqAttributesFactory cqf = new CqAttributesFactory();
        cqf.addCqListener(new CreditLimitCqListener(kafkaTemplate));
        
        CqQuery cq = queryService.newCq(
            "CreditLimitSignificantIncrease",
            queryString,
            cqf.create()
        );
        
        cq.execute();
        log.info("CQ registered: {}", queryString);
        
        return cq;
    }
}

@RequiredArgsConstructor
@Slf4j
public class CreditLimitCqListener implements CqListener {
    
    private final KafkaTemplate<String, CreditLimitEvent> kafkaTemplate;
    
    @Override
    public void onEvent(CqEvent event) {
        Operation operation = event.getQueryOperation();
        CreditLimit creditLimit = (CreditLimit) event.getNewValue();
        
        log.info("CQ Event: {} for account {}", 
            operation.name(), creditLimit.getAccountId());
        
        CreditLimitEvent kafkaEvent = CreditLimitEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(mapOperationType(operation))
            .accountId(creditLimit.getAccountId())
            .customerId(creditLimit.getCustomerId())
            .previousLimit(creditLimit.getPreviousLimit())
            .newLimit(creditLimit.getCurrentLimit())
            .changePercentage(calculateChangePercentage(creditLimit))
            .timestamp(LocalDateTime.now())
            .build();
        
        kafkaTemplate.send("credit-limit-changes", 
            creditLimit.getAccountId(), kafkaEvent);
    }
    
    @Override
    public void onError(CqEvent event) {
        log.error("CQ Error: {}", event.getThrowable().getMessage());
    }
    
    private String mapOperationType(Operation op) {
        if (op.isCreate()) return "CREATED";
        if (op.isUpdate()) return "UPDATED";
        if (op.isDestroy()) return "DELETED";
        return "UNKNOWN";
    }
    
    private BigDecimal calculateChangePercentage(CreditLimit cl) {
        if (cl.getPreviousLimit().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }
        return cl.getCurrentLimit().subtract(cl.getPreviousLimit())
            .divide(cl.getPreviousLimit(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
}
```

#### Kafka Consumer

```java
@Service
@Slf4j
public class CreditLimitEventConsumer {
    
    @KafkaListener(topics = "credit-limit-changes", groupId = "notification-service")
    public void handleCreditLimitChange(CreditLimitEvent event) {
        log.info("Received credit limit change event: {}", event);
        
        if ("UPDATED".equals(event.getEventType()) && 
            event.getChangePercentage().compareTo(BigDecimal.valueOf(50)) > 0) {
            
            // Send notification
            sendNotification(event);
            
            // Trigger risk assessment
            triggerRiskAssessment(event);
        }
    }
    
    private void sendNotification(CreditLimitEvent event) {
        // Implementation
    }
    
    private void triggerRiskAssessment(CreditLimitEvent event) {
        // Implementation
    }
}
```

### 7.5 驗收標準

| 指標 | 目標值 |
|------|--------|
| 事件觸發延遲 | 資料變更後 < 100ms |
| Kafka 傳遞 | 事件正確傳遞至 Kafka |
| 高頻變更 | 無事件遺漏 |
| CQ 管理 | 註冊/取消註冊正常運作 |

---

## 8. PoC 情境五：地理分散式部署 (WAN Replication)

### 8.1 情境說明

模擬跨資料中心的資料同步場景，利用 Geode 的 WAN Gateway 功能實現雙向或單向複製，確保災難復原能力與多區域資料一致性。在 Kind 環境中使用兩個獨立叢集模擬。

### 8.2 架構設計

- **複製模式**：Active-Active (雙向複製)
- **衝突解決**：Last-Write-Wins / Custom Resolver
- **Gateway**：Sender + Receiver 配對
- **拓樸**：2 個 Kind 叢集模擬 Site A / Site B

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        WAN Replication Architecture                              │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────┐       ┌─────────────────────────────────┐
│           Site A (Primary)       │       │          Site B (Secondary)      │
│         Kind Cluster #1          │       │         Kind Cluster #2          │
│                                  │       │                                  │
│  ┌────────────────────────────┐ │       │ ┌────────────────────────────┐  │
│  │       Geode Cluster        │ │       │ │       Geode Cluster        │  │
│  │                            │ │       │ │                            │  │
│  │  ┌──────────┐ ┌──────────┐│ │       │ │┌──────────┐ ┌──────────┐  │  │
│  │  │ Locator  │ │ Locator  ││ │       │ ││ Locator  │ │ Locator  │  │  │
│  │  └──────────┘ └──────────┘│ │       │ │└──────────┘ └──────────┘  │  │
│  │                            │ │       │ │                            │  │
│  │  ┌──────────┐ ┌──────────┐│ │       │ │┌──────────┐ ┌──────────┐  │  │
│  │  │ Server-0 │ │ Server-1 ││ │       │ ││ Server-0 │ │ Server-1 │  │  │
│  │  │          │ │          ││ │       │ ││          │ │          │  │  │
│  │  │┌────────┐│ │┌────────┐││ │       │ ││┌────────┐│ │┌────────┐│  │  │
│  │  ││Gateway ││ ││Gateway ││││       │ │││Gateway ││ ││Gateway ││  │  │
│  │  ││Sender  │├─┼┼────────┼┼┼───────┼─┼┼│Receiver││ ││Receiver││  │  │
│  │  │└────────┘│ │└────────┘│││       │ ││└────────┘│ │└────────┘│  │  │
│  │  │┌────────┐│ │┌────────┐│││       │ ││┌────────┐│ │┌────────┐│  │  │
│  │  ││Gateway ││ ││Gateway ││││       │ │││Gateway ││ ││Gateway ││  │  │
│  │  ││Receiver│◄┼┼┼────────┼┼┼───────┼─┼┼│Sender  ││ ││Sender  ││  │  │
│  │  │└────────┘│ │└────────┘│││       │ ││└────────┘│ │└────────┘│  │  │
│  │  └──────────┘ └──────────┘││       │ │└──────────┘ └──────────┘  │  │
│  │                            │ │       │ │                            │  │
│  │  ┌──────────┐ ┌──────────┐│ │       │ │┌──────────┐ ┌──────────┐  │  │
│  │  │ Server-2 │ │ Server-3 ││ │       │ ││ Server-2 │ │ Server-3 │  │  │
│  │  └──────────┘ └──────────┘│ │       │ │└──────────┘ └──────────┘  │  │
│  └────────────────────────────┘ │       │ └────────────────────────────┘  │
│                                  │       │                                  │
│  ┌────────────────────────────┐ │       │ ┌────────────────────────────┐  │
│  │      App Instances         │ │       │ │      App Instances         │  │
│  └────────────────────────────┘ │       │ └────────────────────────────┘  │
└─────────────────────────────────┘       └─────────────────────────────────┘
                  │                                         │
                  │         Docker Network Bridge           │
                  └─────────────────────────────────────────┘
```

### 8.3 工作項目

| # | 任務 | 預估時間 | 狀態 |
|---|------|----------|------|
| 1 | 建立第二個 Kind 叢集 (Site B) | 30 分鐘 | ⬜ 待執行 |
| 2 | 設定叢集間網路連通 (docker network) | 1 小時 | ⬜ 待執行 |
| 3 | 部署 Site B Geode 叢集 | 1 小時 | ⬜ 待執行 |
| 4 | 設定 Site A Gateway Sender | 1.5 小時 | ⬜ 待執行 |
| 5 | 設定 Site B Gateway Receiver | 1 小時 | ⬜ 待執行 |
| 6 | 設定反向 Gateway (B -> A) | 1.5 小時 | ⬜ 待執行 |
| 7 | 設定 Region 啟用 WAN Replication | 1 小時 | ⬜ 待執行 |
| 8 | 實作衝突解決策略 | 2 小時 | ⬜ 待執行 |
| 9 | 驗證雙向資料同步 | 1.5 小時 | ⬜ 待執行 |
| 10 | 模擬 Site A 故障，驗證 Site B 接手 | 2 小時 | ⬜ 待執行 |
| 11 | 撰寫測試報告 | 1 小時 | ⬜ 待執行 |

### 8.4 設定範例

#### Gateway Sender 設定

```xml
<!-- cache.xml for Site A -->
<cache>
    <gateway-sender id="sender-to-site-b" 
                    remote-distributed-system-id="2"
                    parallel="true"
                    enable-persistence="true"
                    disk-store-name="gateway-disk-store"
                    maximum-queue-memory="100"
                    batch-size="100"
                    batch-time-interval="10">
    </gateway-sender>
    
    <gateway-receiver start-port="5000" end-port="5500"/>
    
    <region name="Customers">
        <region-attributes refid="PARTITION_REDUNDANT">
            <partition-attributes redundant-copies="1"/>
            <gateway-sender-ids>sender-to-site-b</gateway-sender-ids>
        </region-attributes>
    </region>
</cache>
```

#### 衝突解決器

```java
public class LastWriteWinsResolver implements GatewayConflictResolver {
    
    @Override
    public void onEvent(TimestampedEntryEvent event, 
                        GatewayConflictHelper helper) {
        
        if (event.getOldTimestamp() < event.getNewTimestamp()) {
            // New event is newer - apply it
            helper.changeEventValue(event.getNewValue());
        } else {
            // Existing value is newer - discard incoming
            helper.disallowEvent();
        }
    }
}
```

### 8.5 驗收標準

| 指標 | 目標值 |
|------|--------|
| 同步延遲 | Site A 寫入後 < 5秒 同步至 Site B |
| 衝突解決 | 雙向寫入時衝突正確解決 |
| 故障轉移 | Site 故障時另一 Site 可正常運作 |
| 自動恢復 | 恢復後資料自動同步 |

---

## 9. 監控與運維

### 9.1 監控架構

建置完整的監控體系，包含指標收集、視覺化儀表板、告警通知等，確保 PoC 期間可觀察系統運行狀態。

```
┌─────────────────────────────────────────────────────────────────┐
│                     Monitoring Architecture                      │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        Geode Cluster                             │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Server-0   │  │   Server-1   │  │   Server-2   │          │
│  │              │  │              │  │              │          │
│  │ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │          │
│  │ │   JMX    │ │  │ │   JMX    │ │  │ │   JMX    │ │          │
│  │ │ Exporter │ │  │ │ Exporter │ │  │ │ Exporter │ │          │
│  │ └────┬─────┘ │  │ └────┬─────┘ │  │ └────┬─────┘ │          │
│  └──────┼───────┘  └──────┼───────┘  └──────┼───────┘          │
│         │                 │                 │                   │
└─────────┼─────────────────┼─────────────────┼───────────────────┘
          │                 │                 │
          └─────────────────┼─────────────────┘
                            │ Metrics Scrape
                            ▼
          ┌─────────────────────────────────────┐
          │            Prometheus               │
          │                                     │
          │  ┌─────────────────────────────┐   │
          │  │    ServiceMonitor Config    │   │
          │  └─────────────────────────────┘   │
          │  ┌─────────────────────────────┐   │
          │  │      Alert Rules            │   │
          │  └─────────────────────────────┘   │
          └──────────────────┬──────────────────┘
                             │
                             ▼
          ┌─────────────────────────────────────┐
          │             Grafana                 │
          │                                     │
          │  ┌─────────────────────────────┐   │
          │  │    Geode Overview Dashboard │   │
          │  │    - Cluster Health         │   │
          │  │    - Memory Usage           │   │
          │  │    - Operation Latency      │   │
          │  │    - Region Statistics      │   │
          │  └─────────────────────────────┘   │
          └─────────────────────────────────────┘
```

### 9.2 工作項目

| # | 任務 | 預估時間 | 狀態 |
|---|------|----------|------|
| 1 | 部署 Prometheus Operator | 1 小時 | ⬜ 待執行 |
| 2 | 設定 Geode JMX Exporter | 1.5 小時 | ⬜ 待執行 |
| 3 | 設定 ServiceMonitor 抓取 Geode 指標 | 1 小時 | ⬜ 待執行 |
| 4 | 部署 Grafana | 30 分鐘 | ⬜ 待執行 |
| 5 | 匯入/建立 Geode Dashboard | 2 小時 | ⬜ 待執行 |
| 6 | 設定告警規則 (Alertmanager) | 1.5 小時 | ⬜ 待執行 |
| 7 | 設定 Geode Pulse 存取 | 30 分鐘 | ⬜ 待執行 |
| 8 | 建立 Log 收集 (Loki/EFK 選配) | 2 小時 | ⬜ 待執行 |

### 9.3 關鍵監控指標

| 類別 | 指標 | 說明 |
|------|------|------|
| Cluster Health | Member Count | 目前存活的成員數量 |
| | Member Status | 各成員的健康狀態 |
| Memory | Heap Used | JVM Heap 使用量 |
| | Off-Heap Used | Off-heap 記憶體使用量 |
| | Eviction Count | 因記憶體壓力而驅逐的 Entry 數 |
| Operations | GET Latency | 讀取操作延遲 (P50/P99) |
| | PUT Latency | 寫入操作延遲 (P50/P99) |
| | Operations/sec | 每秒操作數 |
| Regions | Entry Count | 各 Region 的 Entry 數量 |
| | Hit Ratio | 快取命中率 |
| | Miss Count | 快取未命中次數 |
| WAN | Queue Size | Gateway 佇列深度 |
| | Replication Lag | 複製延遲時間 |

### 9.4 告警規則

```yaml
# prometheus-rules.yaml
groups:
  - name: geode-alerts
    rules:
      - alert: GeodeMemberDown
        expr: geode_cluster_member_count < 6
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Geode cluster member down"
          description: "Expected 6 members, but only {{ $value }} are up"
      
      - alert: GeodeHighMemoryUsage
        expr: geode_member_heap_used_bytes / geode_member_heap_max_bytes > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Geode member high memory usage"
          description: "Member {{ $labels.member }} heap usage is above 90%"
      
      - alert: GeodeHighLatency
        expr: geode_region_get_latency_p99 > 10
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Geode high read latency"
          description: "P99 read latency is {{ $value }}ms"
```

---

## 10. 工作清單總覽

### 10.1 階段時程

| 階段 | 預估工時 | 依賴 | 優先級 |
|------|----------|------|--------|
| 基礎環境建置 | 4 小時 | 無 | P0 |
| 情境一：高速快取 | 12 小時 | 基礎環境 | P0 |
| 情境二：交易處理 | 16 小時 | 情境一 | P0 |
| 情境三：Session 管理 | 7 小時 | 基礎環境 | P1 |
| 情境四：事件驅動 (CQ) | 13 小時 | 情境一 | P1 |
| 情境五：WAN Replication | 14 小時 | 情境一 | P2 |
| 監控與運維 | 10 小時 | 基礎環境 | P1 |

**總計預估工時：約 76 小時** (不含文件撰寫與會議時間)

### 10.2 建議執行順序

1. **基礎環境建置** - 必要前置作業
2. **監控與運維** - 及早建立可觀察性
3. **情境一：高速快取** - 最基本的 Geode 功能驗證
4. **情境二：交易處理** - 驗證 ACID 特性
5. **情境三：Session 管理** - 快速整合驗證
6. **情境四：事件驅動** - 進階功能驗證
7. **情境五：WAN Replication** - 複雜度最高，最後執行

### 10.3 Gantt Chart

```
Week 1          Week 2          Week 3          Week 4
├───┬───┬───┬───┼───┬───┬───┬───┼───┬───┬───┬───┼───┬───┬───┬───┤
│ M │ T │ W │ T │ F │ M │ T │ W │ T │ F │ M │ T │ W │ T │ F │ M │
├───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┤
│ [基礎環境]                                                      │
│ ████                                                            │
│     [監控]                                                      │
│     ████████                                                    │
│             [情境一：高速快取]                                  │
│             ████████████████                                    │
│                             [情境二：交易處理]                  │
│                             ████████████████████████            │
│                                                 [情境三：Session]│
│                                                 ████████        │
│                                                         [情境四]│
│                                                         ████████│
│                                                                 │
│                                                     [情境五：WAN]│
│                                                     ████████████│
└─────────────────────────────────────────────────────────────────┘
```

### 10.4 風險評估

| 風險 | 影響 | 機率 | 緩解措施 |
|------|------|------|----------|
| Kind 資源限制 | 無法完整測試效能 | 中 | 使用較少節點數量，或考慮雲端環境 |
| Geode 版本相容性 | Spring Data 整合問題 | 中 | 確認版本矩陣，提前測試 |
| WAN 網路模擬限制 | 無法真實測試延遲 | 高 | 使用 tc (traffic control) 模擬延遲 |
| 社群支援度降低 | 問題解決困難 | 中 | 評估 GemFire 商業版，準備替代方案 |
| 學習曲線 | 時程延誤 | 中 | 預留緩衝時間，安排訓練 |

### 10.5 替代方案評估

若 PoC 過程中發現 Apache Geode 不符合需求，建議評估以下替代方案：

| 產品 | 優勢 | 劣勢 |
|------|------|------|
| VMware Tanzu GemFire | Geode 商業版，企業支援 | 授權成本 |
| Redis Enterprise | 成熟生態系，高效能 | 不支援複雜交易 |
| Hazelcast | 輕量級，易部署 | WAN 功能需企業版 |
| Apache Ignite | 完整 SQL 支援 | 學習曲線較陡 |

---

## 附錄

### A. 參考資源

- [Apache Geode 官方文件](https://geode.apache.org/docs/)
- [Spring Data Geode 文件](https://spring.io/projects/spring-data-geode)
- [Kind 官方文件](https://kind.sigs.k8s.io/)
- [Kubernetes 文件](https://kubernetes.io/docs/)

### B. 版本矩陣

| 元件 | 建議版本 |
|------|----------|
| Apache Geode | 1.15.x |
| Spring Boot | 3.2.x |
| Spring Data Geode | 3.1.x |
| Java | 17+ |
| Kubernetes | 1.28+ |

### C. 聯絡資訊

**專案負責人：** [待填寫]  
**技術窗口：** [待填寫]  
**更新日期：** 2026年1月

---

*文件結束*
