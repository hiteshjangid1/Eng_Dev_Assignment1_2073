# How to test the Delivery Root-Cause API

This guide walks through Postman using the six assignment questions. The dataset is **1 Jan 2025 – 12 Sep 2025**, so “yesterday / last week / last month” use that calendar, not today’s date.

## 1. Start the app

From the project root:

```bash
mvn spring-boot:run
```

Wait for: `Tomcat started on port 8080`.  
Startup also prints the six demos in the console and writes `reports/sample-use-case-outputs.txt`.

To turn on the LLM for HTTP insights (optional):

```bash
set CURSOR_API_KEY=crsr_your-key
mvn spring-boot:run
```

Then use Postman folder **0b. AI natural language**, or `GET /api/insights/ask?q=...`. Reports include `aiGenerated` and `ruleBasedNarrative`. Without a key, questions still run using keyword routing and template text.

## 2. Import Postman

1. Open Postman → **Import**.
2. Import both files:
   - `postman/Delivery-Root-Cause.postman_collection.json`
   - `postman/local.postman_environment.json`
3. Top-right environment dropdown: select **Delivery RCA — local**.
4. Collection variable `baseUrl` is `http://localhost:8080` if you skip the environment.

Optional: open the collection → **Run** → **Run collection** to execute all requests and their tests.

## 3. What a successful insight looks like

Every insight endpoint returns JSON like:

| Field | Meaning |
|---|---|
| `title` / `question` / `scope` | Which question was answered |
| `metrics` | Counts and rates |
| `causes` | Ranked tags (stockout, traffic, warehouse delay, …) with `%` of the problem set |
| `narrative` | Human-readable explanation |
| `recommendations` | Operational next steps |
| `sampleOrderIds` | Example orders you can quote in a demo |

Causes can overlap (one order can have weather **and** stockout). Percentages may sum to more than 100%.

## 4. Suggested demo order

### Step A — Prove data loaded

**GET** `http://localhost:8080/api/meta`

Check:

- `orders` = 10000  
- `asOfDate` = `2025-09-12`  
- `demoHints` lists City X, Client X, Warehouse B, Client Y  

If this fails, the app is not running or the port is not 8080.

### Step B — All six answers in one call

**GET** `http://localhost:8080/api/insights/demo`

Expand keys `1_city_yesterday` … `6_onboard_client_y`. Each must have a non-empty `narrative`. This is the fastest video clip.

### Step C — Assignment questions (one request each)

#### 1. Why were deliveries delayed in city X yesterday?

Collection: **1 — Why were deliveries delayed in City X yesterday?**

```
GET {{baseUrl}}/api/insights/city-delays?city=New Delhi&date=2025-01-24
```

| Expect | Typical in this CSV |
|---|---|
| `metrics.orders` | ~27 |
| `problemRate` | high (many Failed / Returned / open-late) |
| Top causes | weather, stockout, warehouse, address, traffic |
| Narrative | mentions New Delhi and the mix of failed vs late |

Try next: `city=Ahmedabad&date=2025-07-15`.

#### 2. Why did Client X’s orders fail in the past week?

```
GET {{baseUrl}}/api/insights/client-failures?clientId=409&from=2025-08-10&to=2025-08-16
```

Client **409** = Bath, Bhatt and Gulati. Expect a **small** week (about 3 orders) with named causes (weather, warehouse delay, breakdown). That is enough to show client-level filtering.

Wider example: `clientId=390&from=2025-05-01&to=2025-05-31`.

#### 3. Top reasons for delivery failures linked to Warehouse B in August

```
GET {{baseUrl}}/api/insights/warehouse-failures?warehouseId=2&yearMonth=2025-08
```

`warehouseId=2` is **Warehouse B** (Warehouse 2, Pune). `yearMonth` must be `2025-08`, not `August` or `08-2025`.

Expect warehouse-side tags: processing delay, system issue, slow packing, stockout.

#### 4. Compare City A and City B last month

```
GET {{baseUrl}}/api/insights/city-compare?cityA=New Delhi&cityB=Ahmedabad&yearMonth=2025-08
```

Expect both cities in `metrics`, different **top cause** names, and a narrative that says a single national playbook would miss local bottlenecks.

#### 5. Festival period — causes and how to prepare

```
GET {{baseUrl}}/api/insights/festival?from=2025-01-01&to=2025-09-12
```

Festival = `event_type=Festival` on external factors, not Diwali dates.  
`festivalProblemRate` and `baselineProblemRate` are **close** in this file (the flag is spread evenly). Still check ranked festival causes (weather, traffic, warehouse) and surge recommendations.

#### 6. Onboard Client Y with ~20,000 extra monthly orders

```
GET {{baseUrl}}/api/insights/capacity-risk?similarClientId=118&extraMonthlyOrders=20000
```

Client **118** = Atwal-Dhawan (volume/failure proxy). Expect ~**18x** current monthly volume and a large `expectedExtraProblemShipmentsPerMonth`. Recommendations should say **do not onboard in one wave**.

Then rerun with `extraMonthlyOrders=5000` and compare the multiplier.

## 5. Collection runner (automated)

1. Collection → **Run**.
2. Keep folders **0. Catalog** and **1. Sample use cases** selected.
3. Run. All of those requests have Postman tests (`HTTP 200`, narrative/metrics checks).
4. Folder **3. Edge cases** includes a **400** test (missing `date`) — include it if you want negative tests.

## 6. curl equivalents (if Postman is unavailable)

```bash
curl "http://localhost:8080/api/meta"
curl "http://localhost:8080/api/insights/demo"
curl "http://localhost:8080/api/insights/city-delays?city=New%20Delhi&date=2025-01-24"
curl "http://localhost:8080/api/insights/client-failures?clientId=409&from=2025-08-10&to=2025-08-16"
curl "http://localhost:8080/api/insights/warehouse-failures?warehouseId=2&yearMonth=2025-08"
curl "http://localhost:8080/api/insights/city-compare?cityA=New%20Delhi&cityB=Ahmedabad&yearMonth=2025-08"
curl "http://localhost:8080/api/insights/festival?from=2025-01-01&to=2025-09-12"
curl "http://localhost:8080/api/insights/capacity-risk?similarClientId=118&extraMonthlyOrders=20000"
```

## 7. Common mistakes

| Symptom | Cause |
|---|---|
| Connection refused | App not running, or not on 8080 |
| `orders: 0` and “No orders matched” | Date outside 2025-01-01 … 2025-09-12, or city spelling (use `New Delhi` not `Delhi`) |
| HTTP 400 | Missing required query param (`date`, `clientId`, `from`, `to`, …) |
| HTTP 500 on warehouse/compare | `yearMonth` not `YYYY-MM` |
| Empty client week | That client had no promised dates in the range — widen `from`/`to` |
