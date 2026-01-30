# Apache Geode Proof of Concept

A comprehensive PoC project demonstrating Apache Geode's capabilities as a distributed in-memory data grid, integrated with Spring Boot microservices.

## Table of Contents

- [About Apache Geode](#about-apache-geode)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
- [API Reference](#api-reference)
- [Test Cases](#test-cases)
- [Performance Targets](#performance-targets)

---

## About Apache Geode

### What is Apache Geode?

Apache Geode is a distributed, in-memory data management platform that provides:

- **Ultra-low latency**: Microsecond-level read/write operations
- **High throughput**: Millions of operations per second
- **Linear scalability**: Add nodes to increase capacity
- **High availability**: Automatic failover with data redundancy
- **Strong consistency**: ACID transactions across distributed data

### Key Features

| Feature | Description |
|---------|-------------|
| **In-Memory Data Grid** | Store and access data in memory across multiple nodes |
| **Distributed Caching** | Cache-aside, read-through, write-through patterns |
| **ACID Transactions** | Full transaction support across partitioned data |
| **Continuous Queries (CQ)** | Real-time event notifications on data changes |
| **WAN Replication** | Multi-datacenter replication for disaster recovery |
| **PDX Serialization** | Language-independent serialization format |

### Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                     Apache Geode Cluster                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐     Locators discover and coordinate       │
│  │   Locator   │     cluster members. They maintain         │
│  │             │     membership and load balancing.         │
│  └─────────────┘                                            │
│                                                             │
│  ┌─────────────┐     Servers store data in Regions          │
│  │   Server    │     (similar to tables). Data is           │
│  │             │     partitioned and replicated across      │
│  └─────────────┘     servers for high availability.         │
│                                                             │
│  ┌─────────────┐     Regions are named, distributed         │
│  │   Region    │     data structures that hold key-value    │
│  │             │     pairs with configurable policies.      │
│  └─────────────┘                                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Region Types

| Type | Description | Use Case |
|------|-------------|----------|
| **PARTITION** | Data split across nodes | Large datasets, scalability |
| **PARTITION_REDUNDANT** | Partitioned with backup copies | High availability |
| **REPLICATE** | Full copy on every node | Read-heavy, small datasets |
| **LOCAL** | Single node only | Testing, temporary data |

---

## Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          Docker Network                                  │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                     Geode Cluster                                  │  │
│  │                                                                    │  │
│  │   ┌─────────────┐                                                  │  │
│  │   │   Locator   │◄──── Cluster coordination & discovery            │  │
│  │   │  Port:10334 │      JMX Manager: 1099                           │  │
│  │   │  Pulse:7070 │      Web UI: http://localhost:7070/pulse         │  │
│  │   └──────┬──────┘                                                  │  │
│  │          │                                                         │  │
│  │    ┌─────┴─────┐                                                   │  │
│  │    │           │                                                   │  │
│  │    ▼           ▼                                                   │  │
│  │ ┌─────────┐ ┌─────────┐                                            │  │
│  │ │Server-1 │ │Server-2 │◄──── Data nodes with PARTITION_REDUNDANT   │  │
│  │ │Port:404 │ │Port:405 │      regions for high availability         │  │
│  │ │         │ │         │                                            │  │
│  │ │┌───────┐│ │┌───────┐│                                            │  │
│  │ ││Custom-││ ││Custom-││      Customers & Accounts regions          │  │
│  │ ││  ers  ││ ││  ers  ││      replicated across both servers        │  │
│  │ │└───────┘│ │└───────┘│                                            │  │
│  │ │┌───────┐│ │┌───────┐│                                            │  │
│  │ ││Account││ ││Account││                                            │  │
│  │ ││   s   ││ ││   s   ││                                            │  │
│  │ │└───────┘│ │└───────┘│                                            │  │
│  │ └─────────┘ └─────────┘                                            │  │
│  │                                                                    │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                              ▲                                          │
│                              │ Geode Client Protocol                    │
│                              │                                          │
│  ┌───────────────────────────┴───────────────────────────────────────┐  │
│  │                    Spring Boot Application                         │  │
│  │                                                                    │  │
│  │   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐               │  │
│  │   │  REST API   │  │  Services   │  │ Repositories│               │  │
│  │   │ Controllers │─▶│  (Business) │─▶│ (Geode)     │               │  │
│  │   └─────────────┘  └─────────────┘  └─────────────┘               │  │
│  │         ▲                                                          │  │
│  │         │ HTTP :8080                                               │  │
│  └─────────┼──────────────────────────────────────────────────────────┘  │
│            │                                                            │
└────────────┼────────────────────────────────────────────────────────────┘
             │
        ┌────┴────┐
        │ Client  │  curl, Postman, Browser
        └─────────┘
```

### Data Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                    Cache-Aside Pattern                            │
└──────────────────────────────────────────────────────────────────┘

    Client Request
          │
          ▼
    ┌───────────┐
    │ REST API  │
    └─────┬─────┘
          │
          ▼
    ┌───────────┐     ┌─────────────────────────────────────┐
    │  Service  │────▶│  1. Check Geode Cache               │
    └─────┬─────┘     │  2. Cache Hit? Return data          │
          │           │  3. Cache Miss? Query source        │
          │           │  4. Store in cache                  │
          ▼           │  5. Return data                     │
    ┌───────────┐     └─────────────────────────────────────┘
    │Repository │
    └─────┬─────┘
          │
          ▼
    ┌───────────┐
    │  Geode    │
    │  Cluster  │
    └───────────┘
```

### Failover Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│              High Availability with PARTITION_REDUNDANT          │
└─────────────────────────────────────────────────────────────────┘

Normal Operation:
┌─────────────┐     ┌─────────────┐
│  Server-1   │     │  Server-2   │
│             │     │             │
│ ┌─────────┐ │     │ ┌─────────┐ │
│ │ Key A   │ │     │ │ Key A   │ │  ◄── Primary on Server-1
│ │(Primary)│ │     │ │(Backup) │ │      Backup on Server-2
│ └─────────┘ │     │ └─────────┘ │
│ ┌─────────┐ │     │ ┌─────────┐ │
│ │ Key B   │ │     │ │ Key B   │ │  ◄── Primary on Server-2
│ │(Backup) │ │     │ │(Primary)│ │      Backup on Server-1
│ └─────────┘ │     │ └─────────┘ │
└─────────────┘     └─────────────┘

After Server-1 Failure:
┌─────────────┐     ┌─────────────┐
│  Server-1   │     │  Server-2   │
│             │     │             │
│   ╳ DOWN    │     │ ┌─────────┐ │
│             │     │ │ Key A   │ │  ◄── Promoted to Primary
│             │     │ │(Primary)│ │
│             │     │ └─────────┘ │
│             │     │ ┌─────────┐ │
│             │     │ │ Key B   │ │  ◄── Still Primary
│             │     │ │(Primary)│ │
│             │     │ └─────────┘ │
└─────────────┘     └─────────────┘
                    All data accessible!
```

---

## Project Structure

```
geode-poc/
├── Apache_Geode_PoC_Workplan.md    # Detailed workplan
├── docker-compose.yaml              # Geode cluster setup
├── README.md                        # This file
│
├── geode-demo-app/                  # Spring Boot application
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/example/geodedemo/
│       ├── GeodeDemoApplication.java
│       ├── config/
│       │   └── GeodeConfig.java
│       ├── entity/
│       │   ├── Customer.java
│       │   └── Account.java
│       ├── repository/
│       │   ├── CustomerRepository.java
│       │   └── AccountRepository.java
│       ├── service/
│       │   ├── CustomerService.java
│       │   └── AccountService.java
│       ├── controller/
│       │   ├── CustomerController.java
│       │   ├── AccountController.java
│       │   └── HealthController.java
│       └── exception/
│           ├── ResourceNotFoundException.java
│           └── GlobalExceptionHandler.java
│
└── k8s/                             # Kubernetes manifests
    ├── base/
    │   └── kind-config.yaml
    └── geode/
        ├── namespace.yaml
        ├── locator-statefulset.yaml
        ├── locator-service.yaml
        ├── server-statefulset.yaml
        └── server-service.yaml
```

---

## Quick Start

### Prerequisites

- Docker 24.0+
- Java 17+
- Maven 3.9+

### 1. Start Geode Cluster

```bash
# Create network
docker network create geode-network

# Start Locator
docker run -d --name geode-locator --hostname locator \
  --network geode-network \
  -p 10334:10334 -p 1099:1099 -p 7070:7070 \
  apachegeode/geode:1.15.1 \
  sh -c 'gfsh start locator --name=locator1 --hostname-for-clients=locator \
    --J=-Dgemfire.jmx-manager=true --J=-Dgemfire.jmx-manager-start=true \
    --J=-Dgemfire.http-service-port=7070 \
    --J=-Dgemfire.enable-network-partition-detection=false && tail -f /dev/null'

# Wait for locator (30 seconds)
sleep 30

# Start Server 1
docker run -d --name geode-server1 --hostname server1 \
  --network geode-network -p 40404:40404 \
  apachegeode/geode:1.15.1 \
  sh -c 'gfsh start server --name=server1 --locators=locator[10334] \
    --hostname-for-clients=server1 --server-port=40404 \
    --J=-Dgemfire.enable-network-partition-detection=false && tail -f /dev/null'

# Start Server 2
docker run -d --name geode-server2 --hostname server2 \
  --network geode-network -p 40405:40404 \
  apachegeode/geode:1.15.1 \
  sh -c 'gfsh start server --name=server2 --locators=locator[10334] \
    --hostname-for-clients=server2 --server-port=40404 \
    --J=-Dgemfire.enable-network-partition-detection=false && tail -f /dev/null'

# Wait for servers
sleep 20

# Create regions
docker exec geode-locator gfsh -e "connect --locator=locator[10334]" \
  -e "create region --name=Customers --type=PARTITION_REDUNDANT" \
  -e "create region --name=Accounts --type=PARTITION_REDUNDANT"
```

### 2. Build and Run Application

```bash
cd geode-demo-app

# Build
mvn clean package -DskipTests

# Build Docker image
docker build -t geode-demo-app:latest .

# Run in Docker network
docker run -d --name geode-demo-app \
  --network geode-network \
  -p 8080:8080 \
  -e GEODE_LOCATOR=locator \
  geode-demo-app:latest
```

### 3. Verify

```bash
# Health check
curl http://localhost:8080/api/health

# Geode Pulse UI
open http://localhost:7070/pulse
# Login: admin / admin
```

### Cleanup

```bash
docker rm -f geode-locator geode-server1 geode-server2 geode-demo-app
docker network rm geode-network
```

---

## API Reference

### Health & Status

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Application health status |
| GET | `/api/regions` | List Geode regions |

### Customers

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/customers` | Create customer |
| GET | `/api/customers` | List all customers |
| GET | `/api/customers/{id}` | Get customer by ID |
| PUT | `/api/customers/{id}` | Update customer |
| DELETE | `/api/customers/{id}` | Delete customer |
| GET | `/api/customers/email/{email}` | Find by email |
| GET | `/api/customers/status/{status}` | Filter by status |

### Accounts

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/accounts` | Create account |
| GET | `/api/accounts` | List all accounts |
| GET | `/api/accounts/{id}` | Get account by ID |
| GET | `/api/accounts/customer/{customerId}` | Get customer's accounts |
| POST | `/api/accounts/{id}/deposit` | Deposit funds |
| POST | `/api/accounts/{id}/withdraw` | Withdraw funds |
| POST | `/api/accounts/transfer` | Transfer between accounts |

---

## Test Cases

### Test Case 1: Basic CRUD Operations

**Objective**: Verify basic create, read, update, delete operations

```bash
# Create a customer
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "1234567890",
    "address": "123 Main St"
  }'

# Expected: 201 Created with customer object including generated ID

# Read customer
curl http://localhost:8080/api/customers/{customerId}

# Expected: 200 OK with customer data

# Update customer
curl -X PUT http://localhost:8080/api/customers/{customerId} \
  -H "Content-Type: application/json" \
  -d '{"phone": "0987654321"}'

# Expected: 200 OK with updated customer

# Delete customer
curl -X DELETE http://localhost:8080/api/customers/{customerId}

# Expected: 204 No Content
```

**Result**: PASS

---

### Test Case 2: Account Operations

**Objective**: Verify deposit, withdraw, and transfer operations

```bash
# Create account with initial balance
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "{customerId}",
    "accountType": "CHECKING",
    "balance": 1000
  }'

# Deposit
curl -X POST http://localhost:8080/api/accounts/{accountId}/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount": 500}'

# Expected: Balance = 1500

# Withdraw
curl -X POST http://localhost:8080/api/accounts/{accountId}/withdraw \
  -H "Content-Type: application/json" \
  -d '{"amount": 200}'

# Expected: Balance = 1300

# Transfer
curl -X POST http://localhost:8080/api/accounts/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": "{savingsId}",
    "toAccountId": "{checkingId}",
    "amount": 300
  }'

# Expected: Funds moved between accounts
```

**Result**: PASS

---

### Test Case 3: Failover Test

**Objective**: Verify data survives server failure

```bash
# 1. Add test data
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"name": "Test User", "email": "test@example.com"}'

# 2. Verify data exists
curl http://localhost:8080/api/customers

# 3. Stop one server
docker stop geode-server1

# 4. Wait for cluster to stabilize (10 seconds)
sleep 10

# 5. Verify data is still accessible
curl http://localhost:8080/api/customers

# Expected: All data still accessible via server2

# 6. Restart server
docker start geode-server1
```

**Result**: PASS - Zero data loss during failover

---

### Test Case 4: Cluster Health Check

**Objective**: Verify cluster status via gfsh

```bash
# Connect and list members
docker exec geode-locator gfsh \
  -e "connect --locator=locator[10334]" \
  -e "list members"

# Expected:
# Member Count : 3
# locator1 [Coordinator]
# server1
# server2

# Check region status
docker exec geode-locator gfsh \
  -e "connect --locator=locator[10334]" \
  -e "describe region --name=Customers"

# Expected:
# Data Policy: partition
# Hosting Members: server1, server2
# redundant-copies: 1
```

**Result**: PASS

---

### Test Case 5: Concurrent Operations

**Objective**: Verify system handles concurrent requests

```bash
# Run multiple requests in parallel
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/customers \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"User $i\", \"email\": \"user$i@example.com\"}" &
done
wait

# Verify all customers created
curl http://localhost:8080/api/customers | jq length

# Expected: 10 customers
```

**Result**: PASS

---

### Test Case 6: Invalid Operations

**Objective**: Verify proper error handling

```bash
# Get non-existent customer
curl http://localhost:8080/api/customers/invalid-id

# Expected: 404 Not Found

# Withdraw more than balance
curl -X POST http://localhost:8080/api/accounts/{accountId}/withdraw \
  -H "Content-Type: application/json" \
  -d '{"amount": 999999}'

# Expected: 400 Bad Request - Insufficient balance

# Invalid transfer
curl -X POST http://localhost:8080/api/accounts/transfer \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId": "invalid", "toAccountId": "invalid", "amount": 100}'

# Expected: 404 Not Found
```

**Result**: PASS

---

## Performance Targets

| Metric | Target | Notes |
|--------|--------|-------|
| Read Latency (P99) | < 1ms | Single key lookup |
| Write Latency (P99) | < 5ms | Single key insert/update |
| Batch Read (100 keys) | < 10ms | getAll operation |
| Throughput | > 10,000 TPS | Per server node |
| Failover Time | < 10s | Automatic recovery |
| Data Loss | 0 | With redundant copies |

---

## References

- [Apache Geode Documentation](https://geode.apache.org/docs/)
- [Spring Data Geode](https://spring.io/projects/spring-data-geode)
- [Geode GitHub Repository](https://github.com/apache/geode)

---

## License

This project is for demonstration purposes only.
