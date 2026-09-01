# Delivery Root-Cause Analyzer

Java 17 / Spring Boot 3 demo that joins the eight logistics CSVs, tags delay and failure causes, and writes human-readable recommendations.

## Data files

Loaded from `src/main/resources/data/`:

| File | Role |
|---|---|
| `orders.csv` | Shipment spine (status, SLA dates, failure_reason, city, client) |
| `clients.csv` | Shipper / B2B client |
| `warehouses.csv` | Node master (Warehouse 2 is treated as **Warehouse B**) |
| `warehouse_logs.csv` | Pick / pack / dispatch notes |
| `fleet_logs.csv` | GPS delay notes, driver, vehicle |
| `drivers.csv` | Driver master |
| `external_factors.csv` | Traffic, weather, festival / holiday / strike |
| `feedback.csv` | Customer comments |

The dataset covers **1 Jan 2025 – 12 Sep 2025**. Demo “yesterday / last week / last month” uses that calendar.

## Run

```bash
mvn spring-boot:run
```

On startup the app:

1. Loads and joins all CSVs in memory  
2. Prints the six sample use cases to the console  
3. Writes `reports/sample-use-case-outputs.txt`

Then open:

- All six demos: http://localhost:8080/api/insights/demo  
- Catalog: http://localhost:8080/api/meta  

### Postman

Import `postman/Delivery-Root-Cause.postman_collection.json` and `postman/local.postman_environment.json`.  
Step-by-step demo script: `postman/HOW-TO-TEST.md`.  

### Individual questions

1. City delays: `GET /api/insights/city-delays?city=New%20Delhi&date=2025-01-24`  
2. Client week: `GET /api/insights/client-failures?clientId=409&from=2025-08-10&to=2025-08-16`  
3. Warehouse B / August: `GET /api/insights/warehouse-failures?warehouseId=2&yearMonth=2025-08`  
4. City compare: `GET /api/insights/city-compare?cityA=New%20Delhi&cityB=Ahmedabad&yearMonth=2025-08`  
5. Festival: `GET /api/insights/festival?from=2025-01-01&to=2025-09-12`  
6. Client Y onboard: `GET /api/insights/capacity-risk?similarClientId=118&extraMonthlyOrders=20000`

## Demo mapping

| Assignment question | Default in this repo |
|---|---|
| City X yesterday | New Delhi, **2025-01-24** (busiest problem city-day in the file) |
| Client X past week | Client **409** Bath, Bhatt and Gulati, 10–16 Aug 2025 (a week with failures in the file) |
| Warehouse B in August | `warehouse_id=2` (Warehouse 2, Pune), Aug 2025 |
| City A vs City B last month | New Delhi vs Ahmedabad, Aug 2025 |
| Festival period | Orders with `event_type=Festival` |
| Client Y + 20k orders | Volume/failure mix of client **118** Atwal-Dhawan scaled to 20,000/month |

## Documents

Word files in `docs/`:

- `How-This-Problem-Can-Be-Solved.docx` — write-up of how the delivery-failure problem can be solved (includes architecture diagram). Same content is also saved as `Delivery-Failure-Root-Cause-Solution.docx`  
- `Sample-Use-Case-Outputs.docx` — recorded answers for the six assignment questions
