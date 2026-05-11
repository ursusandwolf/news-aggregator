# MoneyNews Aggregator

Standalone financial news and regulatory filings aggregator built with Java 21 and Spring Boot 3.

## Features
- **Multi-source Ingestion**: Parallel fetching from Yahoo Finance, SEC EDGAR, and RSS (CNBC, Reuters, WSJ).
- **Virtual Threads**: Powered by Java 21 Project Loom for high-performance I/O.
- **Smart Deduplication**: Multi-level filtering using Redis (URL canonicalization and Title hashing).
- **SEC Integration**: Direct access to Archives EDGAR submissions.

## Tech Stack
- Java 21
- Spring Boot 3.2.5
- Redis
- MySQL
- Jsoup & Rome

## API Endpoints
- `GET /api/v1/news/{ticker}`: Consolidated news feed.
- `GET /api/v1/news/{ticker}/filings`: SEC regulatory filings.

## Setup
1. Ensure Redis and MySQL are running.
2. Configure `application.properties`.
3. Run with `mvn spring-boot:run`.
