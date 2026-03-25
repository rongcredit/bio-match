# bio-match

bio-match is a small multi-module Java project (Maven) providing utilities and a console Spring Boot application for biological sequence matching (protein/RNA translation and matching utilities).

This repository contains two modules:

- bio-match-console
  - A Spring Boot console application that starts the application context. Main class: `com.rongcredit.bio.match.console.BioMatchApp`.
  - It depends on `bio-match-utils` and excludes datasource autoconfiguration.
- bio-match-utils
  - A library module providing sequence translation and matching utilities (classes such as `ProteinMatcher`, `ProteinTranslator`, `RNAProvider`, `MatchResult`, `TranslateResult`, and supporting circ package).

Project structure

- pom.xml (parent POM, packaging=pom)
- bio-match-console/
  - Spring Boot app and console commands under `src/main/java/com/rongcredit/bio/match/console`
- bio-match-utils/
  - Reusable utilities under `src/main/java/com/rongcredit/bio/match/utils`

Requirements

- Java 8+ (project uses Maven and Spring Boot; confirm with your local JDK version)
- Maven 3.x

Build

From the `bio-match` project root, run:

```bash
mvn -T 1C clean install
```

This will build both modules and install artifacts into your local Maven repository.

Run

To run the console application (from project root):

```bash
cd bio-match-console
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

Or run the packaged jar after building:

```bash
cd bio-match-console
mvn package
java -jar target/bio-match-console-<version>.jar
```

(Replace `<version>` with the artifact version shown in the `pom.xml` — `1.0.1-SNAPSHOT` per the parent POM.)

Development notes

- The console application scans base package `com.rongcredit` and excludes `DataSourceAutoConfiguration`.
- `bio-match-utils` depends on `flowx-utils` (property `flowx.version` referenced in the POM). Ensure the `flowx` parent or dependencies are resolvable in your environment.
- Unit tests: run `mvn test` from module directories.

Useful commands

- Build all modules: `mvn clean install`
- Build a single module: `mvn -pl bio-match-utils clean install`
- Run console app in IDE: run `BioMatchApp` as a Java application with Spring profile as needed.

Contributing

- Fork/branch, make changes in the appropriate module, add/update unit tests, and submit a pull request.

License

- This project is licensed under the MIT License - see the `LICENSE` file for details.

Contact

- Internal to the `com.rongcredit` organization. For questions, find the module owners in your organization or check module `doc/` folders for additional details.