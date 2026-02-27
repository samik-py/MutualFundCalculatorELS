# MutualFundCalculatorELS

## Backend (Spring Boot)

From the `backend/` directory:

- Run tests: `./mvnw test`
- Run app: `./mvnw spring-boot:run`

## CAPM Data Configuration

- Set FRED key:
  - macOS/Linux: `export FRED_API_KEY=your_key_here`
  - The backend reads `fred.apiKey`, which defaults to `${FRED_API_KEY}` when unset.
- Risk-free rate (`Rf`):
  - Source: FRED DGS10 via `fred.baseUrl` (default `https://api.stlouisfed.org/fred`).
  - Fallback: `capm.riskFreeFallback` (default `0.04`) when key/network/parse fails.
- Market return (`Rm`):
  - Source: public Yahoo Finance S&P 500 chart endpoint (`^GSPC`) using the latest 5-year monthly close data.
  - Computation: 5-year CAGR, `Rm = (end / start)^(1/5) - 1`.
  - Fallback: `capm.marketReturn5y` (default `0.10`) if fetch or parsing fails.
- Caching:
  - Both `Rf` and `Rm` are cached for 24 hours to reduce external calls.
