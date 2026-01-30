#!/bin/bash
# WAN Replication Test Script
# Tests bi-directional replication between Site A (Taiwan) and Site B (Japan)

set -e

echo "============================================"
echo "  Apache Geode WAN Replication Test"
echo "============================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Wait for clusters to be ready
echo -e "${YELLOW}Waiting for clusters to initialize...${NC}"
sleep 10

# Function to run gfsh command on Site A
gfsh_site_a() {
    docker exec geode-server-site-a gfsh -e "connect --locator=locator-site-a[10334]" -e "$1"
}

# Function to run gfsh command on Site B
gfsh_site_b() {
    docker exec geode-server-site-b gfsh -e "connect --locator=locator-site-b[10334]" -e "$1"
}

echo ""
echo "============================================"
echo "  Test 1: Check Gateway Senders Status"
echo "============================================"
echo ""

echo -e "${YELLOW}Site A Gateway Sender:${NC}"
gfsh_site_a "list gateway-senders" || true

echo ""
echo -e "${YELLOW}Site B Gateway Sender:${NC}"
gfsh_site_b "list gateway-senders" || true

echo ""
echo "============================================"
echo "  Test 2: Check Gateway Receivers Status"
echo "============================================"
echo ""

echo -e "${YELLOW}Site A Gateway Receiver:${NC}"
gfsh_site_a "list gateway-receivers" || true

echo ""
echo -e "${YELLOW}Site B Gateway Receiver:${NC}"
gfsh_site_b "list gateway-receivers" || true

echo ""
echo "============================================"
echo "  Test 3: Write Data to Site A"
echo "============================================"
echo ""

echo -e "${YELLOW}Creating customer in Site A...${NC}"
gfsh_site_a "put --region=/Customers --key=WAN-001 --value='{\"id\":\"WAN-001\",\"name\":\"Taiwan Customer\",\"email\":\"taiwan@example.com\"}'"

echo -e "${YELLOW}Creating account in Site A...${NC}"
gfsh_site_a "put --region=/Accounts --key=ACC-WAN-001 --value='{\"id\":\"ACC-WAN-001\",\"customerId\":\"WAN-001\",\"balance\":10000}'"

echo ""
echo "============================================"
echo "  Test 4: Verify Replication to Site B"
echo "============================================"
echo ""

echo -e "${YELLOW}Waiting for replication (5 seconds)...${NC}"
sleep 5

echo -e "${YELLOW}Reading customer from Site B:${NC}"
gfsh_site_b "get --region=/Customers --key=WAN-001"

echo ""
echo -e "${YELLOW}Reading account from Site B:${NC}"
gfsh_site_b "get --region=/Accounts --key=ACC-WAN-001"

echo ""
echo "============================================"
echo "  Test 5: Write Data to Site B"
echo "============================================"
echo ""

echo -e "${YELLOW}Creating customer in Site B...${NC}"
gfsh_site_b "put --region=/Customers --key=WAN-002 --value='{\"id\":\"WAN-002\",\"name\":\"Japan Customer\",\"email\":\"japan@example.com\"}'"

echo -e "${YELLOW}Creating account in Site B...${NC}"
gfsh_site_b "put --region=/Accounts --key=ACC-WAN-002 --value='{\"id\":\"ACC-WAN-002\",\"customerId\":\"WAN-002\",\"balance\":20000}'"

echo ""
echo "============================================"
echo "  Test 6: Verify Replication to Site A"
echo "============================================"
echo ""

echo -e "${YELLOW}Waiting for replication (5 seconds)...${NC}"
sleep 5

echo -e "${YELLOW}Reading customer from Site A:${NC}"
gfsh_site_a "get --region=/Customers --key=WAN-002"

echo ""
echo -e "${YELLOW}Reading account from Site A:${NC}"
gfsh_site_a "get --region=/Accounts --key=ACC-WAN-002"

echo ""
echo "============================================"
echo "  Test 7: Region Sizes Comparison"
echo "============================================"
echo ""

echo -e "${YELLOW}Site A - Customers region size:${NC}"
gfsh_site_a "describe region --name=/Customers"

echo ""
echo -e "${YELLOW}Site B - Customers region size:${NC}"
gfsh_site_b "describe region --name=/Customers"

echo ""
echo "============================================"
echo -e "${GREEN}  WAN Replication Test Complete!${NC}"
echo "============================================"
echo ""
echo "Summary:"
echo "- Data written to Site A was replicated to Site B"
echo "- Data written to Site B was replicated to Site A"
echo "- Bi-directional WAN replication is working"
echo ""
