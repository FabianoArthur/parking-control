# Contributing

Thanks for taking the time to contribute!

1. Open an issue first for anything bigger than a small fix, so we can agree on the approach.
2. Fork, create a branch (`feat/...` or `fix/...`) and keep the change focused.
3. Run `./mvnw verify` before pushing. It checks formatting (google-java-format via Spotless)
   and runs the unit and integration tests. `./mvnw spotless:apply` fixes formatting.
4. Add or update tests for any behaviour you change. Pricing rules live in
   `PriceCalculatorTest`; HTTP behaviour lives in `ParkingFlowIT`.
5. Use [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`...).

Never commit credentials: configuration comes from environment variables (see `.env.example`).
