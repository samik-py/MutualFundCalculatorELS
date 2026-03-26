# Code Changes Summary

Summary of backend and frontend changes implemented for the Mutual Fund Calculator ELS application.

**Note:** When adding new changes, append them in the "Subsequent changes" section at the bottom instead of overwriting this file.

---

## Backend

### Entities & Repositories

- **`com.mfcalculator.model.User`** – `id`, `username`, `password`, `displayName`. Table `users`.
- **`com.mfcalculator.model.Portfolio`** – `id`, `user` (ManyToOne), `name`, `createdAt`, `updatedAt`. Table `portfolios`. OneToMany `holdings`.
- **`com.mfcalculator.model.PortfolioHolding`** – `id`, `portfolio` (ManyToOne), `fundId`, `shares`, `purchasePrice`, `purchaseDate` (LocalDate). Table `portfolio_holdings`.
- **`com.mfcalculator.model.SavedChart`** – `id`, `user` (ManyToOne), `title`, `fundIds` (comma-separated), `timeHorizon`, `amount`, `createdAt`. Table `saved_charts`.
- **Repositories**: `UserRepository`, `PortfolioRepository`, `PortfolioHoldingRepository`, `SavedChartRepository`.

### Security & Auth

- **`UserPrincipal`** – Implements `Authentication`; holds `userId` and `username`; used as `SecurityContext` principal.
- **`JwtAuthenticationFilter`** – For `/api/user/**`: reads `Authorization: Bearer <token>`, parses JWT, loads `User` by `sub` (username), sets `UserPrincipal` in `SecurityContext`.
- **`SecurityConfig`** – Stateless session; `/api/user/**` authenticated; `/api/auth/**`, `/api/calculate`, etc. permitted; CORS for frontend origin.

### DTOs

- **Portfolios**: `CreatePortfolioRequest` (name), `AddHoldingRequest` (fundId, shares, purchasePrice, purchaseDate), `PortfolioSummaryResponse`, `PortfolioDetailResponse`, `HoldingDetailResponse`.
- **Charts**: `SaveChartRequest` (title, fundIds, timeHorizon, amount), `SavedChartResponse`.
- **Performance**: `HoldingPerformanceItem` (fundId, fundName, ticker, shares, costBasis, currentValue, gainLoss, returnPct), `PortfolioPerformanceResponse` (holdings + totals), `DashboardResponse` (totalCostBasis, totalCurrentValue, totalGainLoss, totalReturnPct).

### Services

- **`UserPortfolioService`** – Portfolio/holding CRUD scoped to current user; `getPortfolioPerformance(userId, portfolioId)` and `getDashboardPerformance(userId)` using `FinanceService.annualReturnFor(fundId)`, fractional years held, and `currentValue = costBasis * exp(annualReturn * yearsHeld)`; fund display map for fundName/ticker.
- **`SavedChartService`** – Saved chart CRUD scoped to current user.
- **`FinanceService`** – `annualReturnFor(fundId)` (CAPM / 5-fund map); used by performance and compare logic.

### Controllers

- **`UserPortfolioController`** – `/api/user/portfolios`: GET / (list), POST / (create), GET /{id}, PUT /{id}, DELETE /{id}, POST /{id}/holdings, DELETE /{id}/holdings/{holdingId}, **GET /{id}/performance** (portfolio performance with holding breakdown + totals).
- **`SavedChartController`** – `/api/user/charts`: GET / (list), POST / (save), GET /{id}, DELETE /{id}.
- **`UserDashboardController`** – `/api/user`: **GET /dashboard** (aggregate performance totals across all user portfolios).

### Configuration

- **`application.properties`** – `app.jwt.secret`, `spring.jpa.defer-datasource-initialization=true`.
- **`data.sql`** – Optional test user for local dev.

---

## Frontend

### Services

- **`portfolioApi.js`** – `getPortfolios()`, `createPortfolio(name)`, `getPortfolio(id)`, `updatePortfolio(id, name)`, `deletePortfolio(id)`, `addHolding(portfolioId, holding)`, `removeHolding(portfolioId, holdingId)`. All use `Authorization: Bearer` from `localStorage.getItem('jwt')`.
- **`chartApi.js`** – `getSavedCharts()`, `saveChart(config)`, `getSavedChart(id)`, `deleteChart(id)`.
- **`dashboardApi.js`** – `getDashboard()`, `getPortfolioPerformance(portfolioId)`.

### Auth & Routing

- **`AuthContext`** – `token`, `user`, `login`, `register`, `logout`, `isAuthenticated`; JWT and user stored in `localStorage` (`jwt`, `user`).
- **`ProtectedRoute`** – Redirects unauthenticated users to `/login` (with `from` in location state).
- **LoginPage** – On success, redirects to `from?.pathname` or **`/dashboard`** (default).
- **Navbar** – When logged in: **Dashboard** (first), My Portfolios, Compare Funds, Saved Charts, Get Started, user/logout.

### Pages & Routes (all under `App.jsx`)

| Route            | Component           | Protected | Description |
|------------------|---------------------|-----------|-------------|
| `/`              | LandingPage         | No        | Landing.    |
| `/predictor`     | PredictorPage       | No        | Calculator. |
| `/login`         | LoginPage           | No        | Sign in → /dashboard. |
| `/register`      | RegisterPage        | No        | Sign up.    |
| **`/dashboard`** | **DashboardPage**   | Yes       | Summary cards, pie (allocation by fund), sortable holdings table; portfolio filter dropdown; empty state CTA to /portfolios. |
| `/portfolios`    | PortfoliosPage      | Yes       | Grid of portfolio cards; create modal; delete with confirm; card click → /portfolios/:id. |
| `/portfolios/:id`| PortfolioDetailPage | Yes       | Editable name; holdings table (fund name from id); Add Holding form; Navbar. |
| `/compare`       | CompareChartsPage   | Yes       | Fund checkboxes (2–5), amount, 1–30y slider, Generate Chart; Recharts LineChart; Save Chart modal; supports `?chartId=` to load saved chart and auto-generate. |
| `/charts`        | SavedChartsPage     | Yes       | Grid of saved chart cards; delete with confirm; card click → /compare?chartId={id}. |

### Dashboard Page (detail)

- **Top**: Three glass-style summary cards – Total Value (white), Total Gain/Loss (green/red $), Overall Return (green/red %).
- **Filter**: If user has multiple portfolios, dropdown “All Portfolios” (default) or one portfolio; refetches data accordingly.
- **Middle**: Recharts PieChart – allocation by fund (current value); same 5 colors as compare; Legend + Tooltip.
- **Bottom**: Sortable table – Fund Name, Ticker, Shares, Cost Basis, Current Value, Gain/Loss ($), Return (%); column header click toggles sort; green/red for gains and return.
- **Empty state**: No holdings → message + “Go to Portfolios” link to `/portfolios`.

### Styling

- **Theme**: Dark background (`--bg-deep`), glass cards (`--glass-bg`, `backdrop-filter: blur`), gold accents (`--gold`), white/serif titles.
- **Shared**: PredictorForm.css for inputs/sliders; PortfoliosPage / SavedChartsPage / DashboardPage / CompareChartsPage / PortfolioDetailPage each have page-specific CSS matching this theme.

### Compare & Saved Charts Flow

- **CompareChartsPage**: If URL has `?chartId=`, fetches saved chart via `getSavedChart(id)`, prefills funds/amount/years, then runs generation so chart appears without clicking Generate.
- **SavedChartsPage**: Cards show title, date saved, fund names (from ids), time horizon, amount; Delete with Yes/No; click card → `/compare?chartId={id}`.

---

## File List (key files added or modified)

### Backend (Java)

- `model/User.java`, `model/Portfolio.java`, `model/PortfolioHolding.java`, `model/SavedChart.java`
- `repository/UserRepository.java`, `PortfolioRepository.java`, `PortfolioHoldingRepository.java`, `SavedChartRepository.java`
- `dto/CreatePortfolioRequest.java`, `AddHoldingRequest.java`, `PortfolioSummaryResponse.java`, `PortfolioDetailResponse.java`, `HoldingDetailResponse.java`, `SaveChartRequest.java`, `SavedChartResponse.java`, `HoldingPerformanceItem.java`, `PortfolioPerformanceResponse.java`, `DashboardResponse.java`
- `security/UserPrincipal.java`, `JwtAuthenticationFilter.java`
- `config/SecurityConfig.java`
- `service/UserPortfolioService.java`, `SavedChartService.java`
- `controller/UserPortfolioController.java`, `SavedChartController.java`, `UserDashboardController.java`
- `resources/application.properties`, `resources/data.sql`
- `pom.xml` (Spring Security, jjwt)

### Frontend

- `services/portfolioApi.js`, `services/chartApi.js`, `services/dashboardApi.js`
- `context/AuthContext.jsx`
- `components/ProtectedRoute.jsx`
- `pages/PortfoliosPage.jsx`, `pages/PortfoliosPage.css`
- `pages/PortfolioDetailPage.jsx`, `pages/PortfolioDetailPage.css`
- `pages/CompareChartsPage.jsx`, `pages/CompareChartsPage.css`
- `pages/SavedChartsPage.jsx`, `pages/SavedChartsPage.css`
- `pages/DashboardPage.jsx`, `pages/DashboardPage.css`
- `pages/LoginPage.jsx` (redirect to `/dashboard`)
- `components/Navbar.jsx` (Dashboard first when logged in)
- `App.jsx` (routes for `/dashboard`, `/portfolios`, `/portfolios/:id`, `/compare`, `/charts`)

---

## Subsequent changes

### Session: Database + JWT Auth + Frontend Auth Pages

#### Backend — new/modified files

**`pom.xml`** — added `spring-boot-starter-data-jpa`, `h2` (runtime), `spring-boot-starter-security`, `jjwt-api/impl/jackson` 0.12.5, `spring-security-test`.

**`application.properties`** — added H2 datasource (`jdbc:h2:mem:mfcalculator`), JPA (`ddl-auto=create-drop`, `show-sql=true`, `defer-datasource-initialization=true`), H2 console at `/h2-console`, JWT properties (`jwt.secret`, `jwt.expirationMs=86400000`).

**`model/User.java`** (modified) — added `displayName` field + getter/setter. Entity fields: `id`, `username` (unique, not null), `password` (not null), `displayName`.

**`repository/UserRepository.java`** — `JpaRepository<User, Long>` + `findByUsername(String)`.

**`security/JwtUtil.java`** (new) — `@Component`; reads `jwt.secret` + `jwt.expirationMs`; `generateToken(username)`, `extractUsername(token)`, `isTokenValid(token)` using jjwt 0.12.x fluent API.

**`security/UserDetailsServiceImpl.java`** (new) — `UserDetailsService` backed by `UserRepository`; builds Spring Security `User` with role `USER`.

**`security/JwtAuthenticationFilter.java`** (modified) — rewrote to use `JwtUtil` + `UserDetailsServiceImpl`; reads `Authorization: Bearer`; sets `UsernamePasswordAuthenticationToken` in `SecurityContext`; removed the previous `/api/user/**`-only `shouldNotFilter`.

**`security/UserPrincipal.java`** — implements `Authentication`; holds `userId` + `username`; used as principal when needed.

**`config/SecurityConfig.java`** (modified) — rewrote: added `CorsConfigurationSource` bean (origin `localhost:5173`, methods GET/POST/PUT/DELETE/OPTIONS), `PasswordEncoder` (`BCrypt`), `AuthenticationManager`; auth rules: `/api/auth/**` + `/api/funds` + `/api/calculate` + `/api/ai/portfolio` + `/h2-console/**` → permitAll, `/api/**` → authenticated; stateless sessions; `frameOptions.sameOrigin` for H2 console.

**`dto/RegisterRequest.java`** (new) — `email` (`@Email @NotBlank`), `password` (`@Size(min=8)`), `displayName`.

**`dto/LoginRequest.java`** (new) — `email` (`@NotBlank`), `password` (`@NotBlank`).

**`dto/AuthResponse.java`** (new) — `token`, `email`, `displayName`.

**`controller/AuthController.java`** (new) — `POST /api/auth/register`: BCrypt-hashes password, saves user, returns JWT + user info (409 if email taken). `POST /api/auth/login`: validates credentials, returns JWT + user info (401 on bad creds).

#### Frontend — new/modified files

**`context/AuthContext.jsx`** (modified) — full rewrite; stores JWT in `localStorage('jwt')` and user object in `localStorage('user')`; evicts expired tokens on mount via `exp` claim check; exposes `user`, `token`, `login(email, password)`, `register(email, password, displayName)`, `logout()`, `isAuthenticated`.

**`components/ProtectedRoute.jsx`** (modified) — changed redirect from `/` to `/login` (preserves `from` in location state).

**`components/Navbar.jsx`** (modified) — removed dead links (Markets, Insights, Funds, About); logged-out: shows Login + Get Started; logged-in: shows Dashboard, My Portfolios, Compare Funds, Get Started, user display name, Logout button.

**`components/Navbar.css`** (modified) — added `.navbar__display-name` (gold tint) and `.navbar__link--logout` (unstyled button reset).

**`pages/LoginPage.jsx`** (new) — glassmorphic card; email + password form; error state; redirects to `from?.pathname` or `/predictor` on success.

**`pages/RegisterPage.jsx`** (new) — glassmorphic card; display name + email + password form; error state; redirects to `/predictor` on success.

**`pages/AuthPage.css`** (new) — shared styles for login/register: `.auth-page`, `.auth-card` (glass), `.auth-eyebrow`, `.auth-title`, `.auth-subtitle`, `.auth-error` (red tint), `.auth-form`, `.auth-field`, `.auth-label`, `.auth-input` (gold focus border), `.auth-btn` (gold outline), `.auth-switch`.

**`App.jsx`** (modified) — added `import LoginPage` + `import RegisterPage`; added `<Route path="/login">` and `<Route path="/register">`.

---

*Generated as a summary of implemented changes.*
