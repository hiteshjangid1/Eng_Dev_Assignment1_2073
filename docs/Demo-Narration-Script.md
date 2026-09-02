# Voiceover script — project structure, API contract, then demo

Use this while recording. Times are a guide, not a clock. **Show** = what is on screen. **Say** = speak this.

The app must already be running: from `Eng_Dev_Assignment1_2073` run `mvn spring-boot:run` and wait for port **8080**.

Dataset calendar is **1 Jan 2025 – 12 Sep 2025**. “Yesterday” in the demo is **New Delhi, 2025-01-24**, not today’s date.

---

## Part 1 — Open (about 45 seconds)

**Show:** README or the Word solution doc (architecture diagrams: data join, then AI briefing layer).

**Say:**

Delivery failures and delays are easy to count and hard to explain. Order timestamps, warehouse logs, fleet GPS notes, customer complaints, and weather or traffic all live in different files. This project joins those eight CSVs onto each shipment, tags likely causes with rules, and returns a human-readable briefing with recommendations. Optionally, Cursor Cloud Agents rewrite the narrative from those tags. The model is not allowed to invent a cause that the rules did not tag.

---

## Part 2 — Project structure (about 2 minutes)

**Show:** Explorer, expand `src/main/java/com/logistics/rca`.

**Say:**

This is a Java 17 Spring Boot 3 application. There is no database and no UI. Data is loaded at startup from `src/main/resources/data`.

Walk the packages:

- **`csv`** — `CsvSupport` reads quoted CSVs, including multi-line addresses. `DataLoader` joins everything on `order_id`. `DataStore` holds about 10,000 enriched shipments in memory.
- **`domain`** — records for orders, clients, warehouses, fleet logs, feedback, plus `Cause`, `Outcome`, and `InsightReport`, which is the API response body.
- **`analysis`** — `CauseEngine` tags stockout, traffic, address, weather, warehouse delay, and similar. `InsightService` answers the six assignment questions. `RecommendationCatalog` maps a cause to an action.
- **`ai`** — optional. If `CURSOR_API_KEY` is set, Cursor writes the briefing and routes a plain-English question. If the key is missing, the same JSON still returns, with a rule-based narrative.
- **`api`** — REST controllers on port 8080.
- **`demo`** — `DemoRunner` prints all six use cases at startup and writes `reports/sample-use-case-outputs.txt`.

Also on disk: `postman` for testing, and `docs` for the solution write-up.

**Show:** `src/main/resources/data` file list.

**Say:**

The spine is `orders.csv`. We attach `warehouse_logs`, `fleet_logs`, `external_factors`, and `feedback` by order id, and look up `clients`, `drivers`, and `warehouses`. Warehouse 2 in Pune is treated as Warehouse B.

---

## Part 3 — API contract and response (about 2.5 minutes)

**Show:** browser or Postman, `http://localhost:8080/api/meta` (do not linger on secrets).

**Say:**

Every insight endpoint returns the same JSON shape, an `InsightReport`.

| Field | Meaning |
|---|---|
| `title`, `question`, `scope` | What was asked and which slice of data |
| `metrics` | Counts: orders, problems, problemRate, failed, delayed, returned, openLate |
| `causes` | Ranked tags: `cause`, `count`, `shareOfProblems`, `evidenceNote` |
| `narrative` | The briefing we would show an operations manager |
| `recommendations` | Actions: staffing, windows, address checks, inventory, festival surge |
| `sampleOrderIds` | Example orders you can quote |
| `complaintSamples` | Short customer comments from that slice |
| `aiGenerated`, `aiModel` | Whether Cursor rewrote the narrative |
| `ruleBasedNarrative` | The template text, kept for audit |

**Show:** this table in the script or a collapsed JSON in Postman.

**Say:**

HTTP contract — all GET unless noted. Base URL `http://localhost:8080`.

| Method | Path | Query | Assignment question |
|---|---|---|---|
| GET | `/api/meta` | none | Catalog: cities, warehouses, AI status |
| GET | `/api/insights/demo` | none | All six answers in one call |
| GET | `/api/insights/city-delays` | `city`, `date` (YYYY-MM-DD) | Why delayed in city X yesterday |
| GET | `/api/insights/client-failures` | `clientId`, `from`, `to` | Why Client X failed last week |
| GET | `/api/insights/warehouse-failures` | `warehouseId`, `yearMonth` (YYYY-MM) | Warehouse B in August |
| GET | `/api/insights/city-compare` | `cityA`, `cityB`, `yearMonth` | City A vs City B last month |
| GET | `/api/insights/festival` | `from`, `to` | Festival period causes and prep |
| GET | `/api/insights/capacity-risk` | `similarClientId`, `extraMonthlyOrders` | Onboard Client Y ~20k orders |
| GET | `/api/insights/ask` | `q` | Natural language |
| POST | `/api/insights/ask` | JSON `{"question":"..."}` | Same, POST body |

Ask responses wrap the report: `{ "question", "intent", "report" }`. `intent` is one of `CITY_DELAYS`, `CLIENT_FAILURES`, `WAREHOUSE_FAILURES`, `CITY_COMPARE`, `FESTIVAL`, `CAPACITY_RISK`.

Missing required params return **400**. A date outside the file still returns **200** with `metrics.orders` equal to 0 and a narrative that no rows matched. `yearMonth` must be `2025-08`, not `August`.

---

## Part 4 — Live demo (about 5–6 minutes)

### 4.1 Catalog

**Show:** `http://localhost:8080/api/meta`

**Say:**

Meta confirms load: 10,000 orders, 500 clients, 50 warehouses, as-of date 12 September 2025. `demoHints` lists the defaults we use on camera. `ai` shows whether a Cursor key is configured.

### 4.2 All six at once

**Show:** `http://localhost:8080/api/insights/demo`

**Say:**

This is the fastest clip. Six keys: city yesterday, client week, warehouse B, city compare, festival, and onboard Client Y. Expand `1_city_yesterday`. Point at `metrics`, then `causes`, then `narrative`, then `recommendations`. Causes can overlap, so percentages can add up to more than 100 percent — one shipment can have weather and a stockout together.

### 4.3 Use case 1 — city delays

**Show:**  
`http://localhost:8080/api/insights/city-delays?city=New%20Delhi&date=2025-01-24`

**Say:**

Question one: why were deliveries delayed in city X yesterday. We use New Delhi on 24 January 2025, the busiest problem city-day in the file. You should see tens of orders, a high problem rate, and weather, stockout, warehouse, or address near the top. Sample order IDs are in the payload if we need to cite one.

### 4.4 Use case 2 — client week

**Show:**  
`http://localhost:8080/api/insights/client-failures?clientId=409&from=2025-08-10&to=2025-08-16`

**Say:**

Question two: Client X, Bath, Bhatt and Gulati, id 409, week 10 to 16 August 2025. The slice is small — a few orders — but the same contract: metrics, causes, narrative, recommendations. That shows the engine filters by client, not only by city.

### 4.5 Use case 3 — Warehouse B

**Show:**  
`http://localhost:8080/api/insights/warehouse-failures?warehouseId=2&yearMonth=2025-08`

**Say:**

Question three: Warehouse B is warehouse id 2, Pune, August 2025. Expect warehouse-side tags: processing delay, system issue, slow packing, stockout, plus whatever happened downstream.

### 4.6 Use case 4 — compare cities

**Show:**  
`http://localhost:8080/api/insights/city-compare?cityA=New%20Delhi&cityB=Ahmedabad&yearMonth=2025-08`

**Say:**

Question four compares New Delhi and Ahmedabad in August. Metrics include both cities’ order counts, problem rates, and top causes. If the number-one cause differs, the narrative says a single national playbook would miss local bottlenecks.

### 4.7 Use case 5 — festival

**Show:**  
`http://localhost:8080/api/insights/festival?from=2025-01-01&to=2025-09-12`

**Say:**

Question five. In this extract, festival is a flag on `external_factors`, not a Diwali calendar. Compare `festivalProblemRate` and `baselineProblemRate`. In this file they are close; we report that honestly. Recommendations still cover surge roster, inventory, and SLA freeze.

### 4.8 Use case 6 — Client Y volume

**Show:**  
`http://localhost:8080/api/insights/capacity-risk?similarClientId=118&extraMonthlyOrders=20000`

**Say:**

Question six is a what-if. Client 118 Atwal-Dhawan is the failure-mix proxy. Adding 20,000 monthly orders is about 18 times current volume. `expectedExtraProblemShipmentsPerMonth` is large if we do not change process. The recommendation is to phase the onboard, not dump the volume onto today’s SLA.

### 4.9 Natural language (optional)

**Show:**  
`http://localhost:8080/api/insights/ask?q=Why%20were%20deliveries%20delayed%20in%20New%20Delhi%20yesterday`

**Say:**

Same engine, English question. The router sets `intent` to `CITY_DELAYS` and returns the same `report` object. If Cursor is on, `aiGenerated` is true and `narrative` is the rewritten briefing. If not, you still get the rule-based story.

---

## Part 5 — Close (about 20 seconds)

**Show:** architecture diagram or folder `docs`.

**Say:**

To recap: we aggregate eight domains onto the order, correlate with a cause taxonomy, and return one JSON briefing per operations question. The API is the product. Postman collection and this script are in the repo so the demo is repeatable.

---

## Backup if something fails on camera

| Problem | What to say |
|---|---|
| Connection refused | App is not on 8080; restart `mvn spring-boot:run` from `Eng_Dev_Assignment1_2073`. |
| `orders: 0` | Date is outside Jan–Sep 2025, or city is `Delhi` instead of `New Delhi`. |
| HTTP 400 | A required query param is missing (`date`, `from`/`to`, `yearMonth`). |
| HTTP 500 on warehouse/compare | `yearMonth` is not `YYYY-MM`. |
| Ask is slow | Cursor Cloud Agent can take up to a few minutes; switch to a GET insight URL. |
| `aiGenerated: false` | No `CURSOR_API_KEY`; the structured answer is still valid. |

## URLs to paste into the browser bar (in order)

```
http://localhost:8080/api/meta
http://localhost:8080/api/insights/demo
http://localhost:8080/api/insights/city-delays?city=New%20Delhi&date=2025-01-24
http://localhost:8080/api/insights/client-failures?clientId=409&from=2025-08-10&to=2025-08-16
http://localhost:8080/api/insights/warehouse-failures?warehouseId=2&yearMonth=2025-08
http://localhost:8080/api/insights/city-compare?cityA=New%20Delhi&cityB=Ahmedabad&yearMonth=2025-08
http://localhost:8080/api/insights/festival?from=2025-01-01&to=2025-09-12
http://localhost:8080/api/insights/capacity-risk?similarClientId=118&extraMonthlyOrders=20000
http://localhost:8080/api/insights/ask?q=Why%20were%20deliveries%20delayed%20in%20New%20Delhi%20yesterday
```
