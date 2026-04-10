## Plan

| ID | Name | Category | Description | Based on | Process Requirements |
|----|------|----------|-------------|----------|----------------------|
| A1 | Secure-by-design | Badge | This badge indicates that during planning of the product, risks have been systematically identified, rated, and fitting counter measures have been defined and integrated in the product's architecture. Further, a process was set in place that ensures continuous risk asessment. | Risk Driven DevelopmentSecure Architecture | No explicit requirements |
|   A1.1 | Risk Driven Development | Metric | Risk-driven development, as desired by, e.g., the CRA, entails all activities that are meant to systematically analyze the risks and possible counter measures associated to the developed product. This also includes risks associated to the customers' usage scenarios. | Vulnerabilities in DependenciesVulnerabilities in own codeNot applicable/mitigated Vulnerabilities | No explicit requirements |
|     A1.1.1 | Threat Modeling (0… 100) | Metric | The first step is to create an initial threat model. The second step is to keep the threat model up to date during product development. This is checked by checking the threat models existence and then whether it was updated for each new release. However, this is still no totally reliable check - we don't look at the content of the Threat Model and assess its quality. | Threat Model exists (yes / no)Threat Model is up to date (0 … 100) | No explicit requirements |
|       A1.1.1.1 | Threat Model exists (yes / no) | Metric | TBD | Git / Confluence / … | Threat modeling tool defined, artifact storage decided, artifacts tagged |
|       A1.1.1.2 | Threat Model is up to date (0 … 100) | Metric | TBD | Software Version + Release DateLast change Date for Threat Model | No explicit requirements |
|         A1.1.1.2.1 | Software Version + Release Date | Metric | TBD | Git | Tagged software releases and artifacts |
|         A1.1.1.2.2 | Last change Date for Threat Model | Metric | TBD | Git / Confluence | No explicit requirements |
|     A1.1.2 | Security Controls defined (0 … 100) | Metric | To fulfill the requirement of risk-based/driven development, we first identify threats (Threat Modeling) and then define mitigations for every threat. The mitigations are integrated in the Architecture through security controls. Security Controls defined is calculated by (#mitigations / #threats) * 100. However, this leaves some uncertanty, as you might have threats with multiple and others with no mitigations. | Identified Threats (List of threats)Planned Mitigation (List of mitigations) | No explicit requirements |
|       A1.1.2.1 | Identified Threats (List of threats) | Metric | TBD | Threat Model (Git / Confluence) | Threat management tool selected, process to list and document threats and mitigations across product versions |
|       A1.1.2.2 | Planned Mitigation (List of mitigations) | Metric | TBD | (Evil) User Story / Architecture Document (Confluence) | Threat management tool selected, process to list and document threats and mitigations across product versions |
|     A1.1.3 | Supply chain attack response planning is documented | Metric | TBD | Confluence | Tagged documents |
|     A1.1.4 | Support and maintainence lifecycle is defined | Metric | TBD | Confluence | Tagged documents |
|     A1.1.5 | Risk-driven planning | Metric | New features are systematically analyzed for risks and documented. | Evil user stories or comparable (yes / no) |  |
|       A1.1.5.1 | Evil user stories or comparable (yes / no) | Metric | TBD | Jira | All evil user stories need to be tagged with a predefined tag |
|   A1.2 | Secure Architecture | Metric | Secure architecture indicates that best practices for common security tasks (authentication / authorization / ...) have been used. Further, the domain / product specific risk requirements derived from "risk-driven development" have been addressed and properly documented. | Architecture is documentedArchitecture is reviewed and approvedAttack surface documentedAttack surface minimized | No explicit requirements |
|     A1.2.1 | Architecture is documented | Metric | TBD | Git/Confluence | Versioned and transparent architecture documentation and review process |
|     A1.2.2 | Architecture is reviewed and approved | Metric | TBD | Git/Confluence | Versioned and transparent architecture documentation and review process |
|     A1.2.3 | Attack surface documented | Metric | TBD | Confluence / Threat Model | Documented discussions / reviews based on architecture + threat model |
|     A1.2.4 | Attack surface minimized | Metric | TBD | Confluence | Documented discussions / reviews based on architecture + threat model |
| A2 | Compliance & Security Requirements‑Integrated Design | Badge | All internal and external requirements applicable to the product have been assessed, documented, and integrated in the design. | Compliance Requirements ConsideredSecurity Requirements Defined | No explicit requirements |
|   A2.1 | Compliance Requirements Considered | Metric | This badge indicates that existing security regulatories and laws have been reviewed regarding their applicability for the product. | OSS License ComplianceProduct Requirements based on applicable Laws | No explicit requirements |
|     A2.1.1 | OSS License Compliance | Metric | TBD | Implemented License approval processProduct requirements based on applicable laws | No explicit requirements |
|       A2.1.1.1 | Implemented License approval process | Metric | TBD | Confluence | Tagged documents |
|       A2.1.1.2 | Identified license requirements and created automated policies | Metric | TBD | Confluence / Pipeline / SBOM | Black / White list of forbidden / allowed licenses for the project. Automated checks in the CI, based on SBOM |
|     A2.1.2 | Product requirements based on applicable laws | Metric | TBD | Identified and documented applicable security laws (CRA, NIS-2, …) | No explicit requirements |
|       A2.1.2.1 | Identified and documented applicable security laws (CRA, NIS-2, …) | Metric | TBD | Confluence | Tagged documents for applicable laws and resulting requirements. |
|   A2.2 | Security Requirements Defined | Metric | This badge indicates that existing (internal) security best practices like SLSA have been reviewed and the appropiate target levels selected. | Reviewed, selected and documented applicable security best practicesIdentified target SLSA levelIdentified and documented applicable security laws (CRA, NIS-2, …) | No explicit requirements |
|     A2.2.1 | Reviewed, selected and documented applicable security best practices | Metric | This metric should make teams identify applicable best practices. This list of practices is then checked during the coding phase and can be used as an input there. Here it is an existence check. | Confluence | Tagged documents for security best practices. If possible the practices should be checked in the pipeline (e.g., best practice: use prepared statements, add CI check) |
|     A2.2.2 | Identified target SLSA level | Metric | TBD | Best practices check list exists (yes / no) | No explicit requirements |
|       A2.2.2.1 | Best practices check list exists (yes / no) | Metric | TBD | TBD | Tagged documents |
|     A2.2.3 | Know your security processes (PSIRT, …) | Metric | TBD | Confluence | Tagged documents |

---

## Code

| ID | Name | Category | Description | Based on | Process Requirements |
|----|------|----------|-------------|----------|----------------------|
| B1 | Vulnerability Management | Badge | This badge indicates that a structured vulnerability management process has been defined and is implemented. This contains active vulnerability scanning, documentation, and remediation. | No known(/applicable) VulnerabilitiesVulnerability RemediationVulnerability Reporting Setup | No explicit requirements |
|   B1.1 | No known(/applicable) Vulnerabilities | Metric | All known vulnerabilities, both in code and dependencies, have been either patched or documented why they are not applicable. | Vulnerabilities in DependenciesVulnerabilities in own codeNot applicable/mitigated Vulnerabilities | No explicit requirements |
|     B1.1.1 | Vulnerabilities in Dependencies | Metric | TBD | Vulnerability Scanner | Tag scan results with scanned version and time stamp of scan |
|     B1.1.2 | Vulnerabilities in own code | Metric | TBD | SAST | Tag scan results with scanned version and time stamp of scan |
|     B1.1.3 | Not applicable/mitigated Vulnerabilities | Metric | TBD | Blackduck | No explicit requirements |
|   B.1.2 | Vulnerability Remediation | Metric | TBD | TBD | No explicit requirements |
|     B1.2.1 | Hotfix release process | Metric | TBD | Confluence | Tagged process definition |
|     B1.2.2 | Target time to react | Metric | TBD | Confluence | Tagged process definition |
|   B1.3 | Vulnerability Reporting Setup | Metric | TBD | Confluence | Tagged process definition |
| B2 | Secure Secret Lifecycle | Badge | Secure management, access and withdrawls of secrets are setup. | No known secretsSecret RotationSecret Access Logs | No explicit requirements |
|   B2.1 | No known secrets | Metric | Pre-commit hooks, code, build, and infrastructure scans continuously check for secrets like API-keys and passwords. | Secret Scanner | Tag scan results with scanned version and time stamp of scan |
|   B2.2 | Secret Rotation | Metric | Maximum lifetime for passwords and API-keys are defined and therby enforce a regular rotation | Vault | Tagged configuration / overview of secret acess from the product |
|   B2.3 | Secret Access Logs | Metric | Secret retreval logs of who accessed which secret when | Vault | Tagged configuration / overview of secret acess from the product |
| B3 | Traceable Code Integrity | Badge | A chain of trust for the product's code has been established through signatures, reviews, and protection rules. | Branch protectionPR review enforcedCommits signed | No explicit requirements |
|   B3.1 | Branch protection | Metric | Changes to main (release, deployment, …) branches can only be performed through peer reviewed pull requests | VCS | No explicit requirements |
|   B3.2 | PR review enforced | Metric | Peer reviews of PRs are enforced. | VCS | No explicit requirements |
|   B3.3 | Commits signed | Metric | Every commit must by signed by its author to ensure the integrity and authenticitiy of the changes | VCS | No explicit requirements |
| B4 | Open Source Dependency Software Health | Badge | This badge indicates that the health of the used dependencies is in the target coridor and current versions of the dependencies are used. | Technical lagDependency Vulnerability RateOSS Ecosystem Health (CHAOSS Metrics) | No explicit requirements |
|   B4.1 | Technical lag | Metric | Summarizes the technical lag of the product's dependencies measured in version distance and libyears. | SBOM | No explicit requirements |
|   B4.2 | Dependency Vulnerability Rate | Metric | Metric used to track the rate of vulnerabilities published for each used dependencies. This should indicate whether a given dependency often experiences security problems and how they handle it. This indicator should be used to decide which dependencies and their usage in the product to monitor more closely. | Vulnerability Scanner | No explicit requirements |
|   B4.3 | OSS Ecosystem Health (CHAOSS Metrics) | Metric | TBD | TBD | No explicit requirements |
|     B4.3.1 | Issue reaction rate | Metric | TBD | Github | No explicit requirements |
|     B4.3.2 | Project is maintained | Metric | TBD | Issue reaction rateProject is maintained | No explicit requirements |
|       B4.3.2.1 | Issue reaction rate | Metric | TBD | Github | No explicit requirements |
|       B4.3.2.2 | PR merged rate | Metric | TBD | Github | No explicit requirements |
|     B4.3.3 | Number of maintainers | Metric | TBD | Github | No explicit requirements |
| B5 | Secure Coding Practices Enforced | Badge | Apply the secure coding practices that fit the previously identified requirements for the product (see "Requirements Assessed"). | Track usage of unsafe keywordDefensive coding guidelinesInput Validation | No explicit requirements |
|   B5.1 | Track usage of unsafe keyword | Metric | Rust specific secure coding rule | Code | TBD |
|   B5.2 | Defensive coding guidelines | Metric | Fitting defensive coding guidelines for the product's tech steck have been identified and are applied during development | Confluence | TBD |
|   B5.3 | Input Validation | Metric | All inputs are properly validated and sanitized. Inputs and their domain requirements should've been identified during identification of the attack surface | Fuzzing | TBD |
| B6 | Continuous Patch Management | Badge | Automated third party dependency updates and well defined release / update process of when to include which updates in new releases. | Automated dependency updatesProcess/strategy exists for third party dependencies (who updates when?) | No explicit requirements |
|   B6.1 | Automated dependency updates | Metric | TBD | Pipeline | Dependabot configuration |
|   B6.2 | Process/strategy exists for third party dependencies (who updates when?) | Metric | TBD | Confluence | Tagged document |
| B7 | Crypto Agility | Badge | This describes the capability to switch out the cryptographic primitives without larger disruption | CBOM | No explicit requirements |
|   B7.1 | CBOM created | Metric | TBD | CBOM | No explicit requirements |
|   B7.2 | PQC Readiness | Metric | TBD | Only PQC / hybrid algorithms used | No explicit requirements |
|     B7.2.1 | Only PQC / hybrid algorithms used | Metric | TBD | Crypto RequirementsCBOM | Structured list of allowed algorithms / libraries / configs |
|   B7.3 | Unknown Crypto | Metric | TBD | Crypto RequirementsCBOM | Structured list of allowed algorithms / libraries / configs |
|   B7.4 | Shadow Crypto | Metric | TBD | Crypto RequirementsCBOM | Structured list of allowed algorithms / libraries / configs |
|   B7.5 | CBOM coverage | Metric | TBD | CBOM | No explicit requirements |
|   B7.6 | Crypto settings and algorithms in configs not in code | Metric | TBD | SAST | TBD |

---

## Build

| ID | Name | Category | Description | Based on | Process Requirements |
|----|------|----------|-------------|----------|----------------------|
| C1 | Continuous SBOM Monitoring | Badge | SBOMs are created and continuously monitored regarding dependency updates, vulnerabilities, and licencse compliance. | SBOM exists (yes/no)SBOM is versionedSBOM is continuously scanned for new vulnerabilities | No explicit requirements |
|   C1.1 | SBOM exists (yes/no) | Metric | TBD | Git / Build Artifact | No explicit requirements |
|   C1.2 | SBOM is versioned | Metric | A SBOM is created and stored for each released version of the product | Git / Build Artifact | No explicit requirements |
|   C1.3 | SBOM is continuously scanned for new vulnerabilities | Metric | All SBOMs are continuously scanned for vulnerabilities | Git / Pipeline | No explicit requirements |
| C2 | Secure Containers | Badge | Containers used in build, test, and deployment are continuously scanned for security vulnerabilities. Further, hardned images are used where applicable. | Container vulnerability scanningMinimal container imagesHardned images (if applicable) | No explicit requirements |
|   C2.1 | Container vulnerability scanning | Metric | All deployed containers are continuously scanned for vulnerabilities | Vulnerability Scanner | No explicit requirements |
|   C2.2 | Minimal container images | Metric | TBD | Manual review / container scanning tool? | Tagged reviews in PRs |
|   C2.3 | Hardned images (if applicable) | Metric | TBD | Manual review | Tagged reviews in PRs / Tagged documents |
| C3 | Developer Security Feedback Loop Exists | Badge | This badge indicates that information about the product are automatically made available to the developers such that they can identify areas of improvement. | Vulnerability Dashboards, SBOM Dashboards | No explicit requirements |
|   C3.1 | Dependency Monitoring Dashboard for devs | Metric | TBD | Dependency Track | Product specific API access and data export |
|   C3.2 | Vulnerability Dashboards | Metric | TBD | SonarQube | Product specific API access and data export |
| C4 | Traceable Artifact Integrity | Badge | A chain of trust for the product's build process has been established through signatures and protection rules. | Signed artifactSigned steps in the build chain | No explicit requirements |
|   C4.1 | Signed artifact | Metric | All generated artifacts are cryptographically signed | Automated static check in pipeline | No explicit requirements |
|   C4.2 | Signed steps in the build chain | Metric | Cryptographic proofs are created for each step in the build process to proof that each tool has been executed and that it had the given results. | Automated static check in pipeline | No explicit requirements |
| C5 | Clear Code Ownership and proper Review Process | Badge | Well defined code ownership and review processes for the product. | Git, Confluence | No explicit requirements |
|   C5.1 | Automated determination for review requirements | Metric | Code owners have been defined for critical code areas. Reviews are automatically requested if changes to these areas occur | Analyse code diffs and automatically identify if a change effects a critical area | No explicit requirements |
|     C5.1.1 | Analyse code diffs and automatically identify if a change effects a critical area | Metric | TBD | Pipeline | Code owners for certain areas defined |
|   C5.2 | Security Peer reviews | Metric | TBD | Git | Tagged reviews in PRs |

---

## Test

| ID | Name | Category | Description | Based on | Process Requirements |
|----|------|----------|-------------|----------|----------------------|
| D1 | Proper Security Tests | Badge | TBD | TBD | No explicit requirements |
|   D1.1 | Static Test Coverage | Metric | Explicit security tests for the critical code areas that were identified based on the previously identified risks ("Secure-by-design"). | Define explicit security tests for critical areas | TBD |
|     D1.1.1 | Define explicit security tests for critical areas | Metric | Critical code areas are identified based on the previously created threat model. For the critical areas extra security tests are defined | TBD | TBD |
|   D1.2 | Dynamic Test Coverage | Metric | TBD | TBD | TBD |
|     D1.2.1 | DAST Coverage | Metric | TBD | TBD | TBD |
|     D1.2.2 | API tests (if applicable) | Metric | TBD | TBD | TBD |
|     D1.2.3 | Pen tests (if applicable) | Metric | TBD | TBD | TBD |
|     D1.2.4 | Fuzz Coverage | Metric | TBD | TBD | TBD |
|   D1.3 | Test Failure Rate | Metric | TBD | TBD | TBD |
|   D1.4 | Backups tested | Metric | TBD | TBD | TBD |
|   D1.5 | Developer Experience | Metric | TBD | TBD | TBD |

---

## Release

| ID | Name | Category | Description | Based on | Process Requirements |
|----|------|----------|-------------|----------|----------------------|
| E1 | SLSA | TBD | TBD | TBD | TBD |
|   E1.1 | https://slsa.dev/spec/v1.1/levels | TBD | TBD | Confluence | TBD |
|   E1.2 | SLSA attestation created | TBD | TBD | Automated static check in pipeline | TBD |
| E2 | External / internal audit and documentation | TBD | TBD | TBD | TBD |
| E3 | Continuous Vulnerability Monitoring (daily scans) | TBD | TBD | SBOMReleased Code | TBD |
| E4 | Supply Chain | TBD | TBD | TBD | TBD |
|   E4.1 | Create critical vendor list (most relevant dependency owners) | TBD | TBD | SBOM | TBD |
|   E4.2 | License Compliance | TBD | TBD | SBOM | TBD |
|   E4.3 | Vendor security evaluation | TBD | TBD | Confluence | TBD |

---

## Configure

| ID | Name | Category | Description | Based on | Process Requirements |
|----|------|----------|-------------|----------|----------------------|
| F1 | Secure by default | TBD | TBD | TBD | TBD |
| F2 | Secrets Management | TBD | TBD | TBD | TBD |
| F3 | Security Logging | TBD | TBD | TBD | TBD |
| F4 | Controlled Deployment Envrionemnt (Infrastructure-as-Code) | TBD | TBD | TBD | TBD |
| F5 | Least privilege validation | TBD | TBD | TBD | TBD |
| F6 | Secure (encrypted) Communication | TBD | TBD | TBD | TBD |

---

## Deploy

| ID | Name | Category | Description | Based on | Process Requirements |
|----|------|----------|-------------|----------|----------------------|
| G1 | Staged rollout / canary release | TBD | TBD | TBD | TBD |
| G2 | Mean Time to Remediate (MTTR) | TBD | TBD | TBD | TBD |
| G3 | Backups | TBD | TBD | TBD | TBD |
| G4 | Rollback capability | TBD | TBD | TBD | TBD |

---

## Operate

| ID | Name | Category | Description | Based on | Process Requirements |
|----|------|----------|-------------|----------|----------------------|
| H1 | Change Failure Rate | TBD | TBD | TBD | TBD |
| H2 | Security Patch Rate | TBD | TBD | TBD | TBD |
| H3 | Security Reaction Time | TBD | TBD | TBD | TBD |
| H4 | Rolling credentials / API keys (if applicable) | TBD | TBD | TBD | TBD |
| H5 | Recovery Time | TBD | TBD | TBD | TBD |
| H6 | Runtime Threat Detection | TBD | TBD | TBD | TBD |
|   H6.1 | Authentication/Authorization bypass atempts | TBD | TBD | Intrusion Detection Systems | TBD |
| H7 | Vulnerability Disclosure Program | TBD | TBD | TBD | TBD |
| H8 | Incident monitoring | TBD | TBD | TBD | TBD |
| H9 | Security Performance Measurements (e.g., how long does it take us to authenticate a user) | TBD | TBD | TBD | TBD |
| H10 | Secure Patch Management Process | TBD | TBD | TBD | TBD |
| H11 | Vulnerability Detection Time (Time CVE released and found in the company) | TBD | TBD | TBD | TBD |
| H12 | Time to detect | TBD | TBD | TBD | TBD |
| H13 | Time to react | TBD | TBD | TBD | TBD |
| H14 | False positive rate | TBD | TBD | TBD | TBD |
| H15 | Blameless post-mortems for security incidents | TBD | TBD | TBD | TBD |

---

## Cross Product

| ID | Name | Category | Description | Based on | Process Requirements |
|----|------|----------|-------------|----------|----------------------|
| X1 | PQC Readiness | TBD | TBD | TBD | TBD |
|   X1.1 | #Systems with full PQC Readiness / #Systems | TBD | TBD | TBD | TBD |
| X2 | License Compliance | TBD | TBD | TBD | TBD |
| X3 | Secure Coding Best Practices | TBD | TBD | TBD | TBD |
| X4 | Implemented License approval process | TBD | TBD | TBD | TBD |
| X5 | Secure Coding Best Practices | TBD | TBD | TBD | TBD |
| X6 | Crypto Requirements list | TBD | TBD | TBD | TBD |