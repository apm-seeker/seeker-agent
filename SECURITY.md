# Security Policy

## Supported Versions

Seeker Agent is early-stage. Security fixes target the latest `main` branch unless a release maintenance policy is introduced.

## Reporting A Vulnerability

Do not open a public issue for sensitive security reports.

Use GitHub Security Advisories if they are enabled for this repository. If advisories are not enabled yet, open a minimal public issue asking for a private security contact without disclosing exploit details.

Include:

- affected version or commit
- affected module or plugin
- impact summary
- reproduction steps or proof of concept
- expected and actual behavior
- suggested mitigation, if available

## Expected Response

Until a formal SLA is published, maintainers should aim to:

- acknowledge a valid private report within 7 days
- provide an initial impact assessment within 14 days
- coordinate disclosure timing with the reporter when the issue is confirmed

## Security Scope

Relevant areas include:

- bytecode instrumentation safety
- agent exceptions affecting user application logic
- sensitive data exposure in traces, logs, SQL, headers, MDC, or debug output
- collector transport security
- dependency vulnerabilities
- unsafe protocol or protobuf handling
- classloader and dependency isolation issues

## Current Limitations

- gRPC transport currently uses plaintext channel creation.
- SQL and log masking policies are incomplete.
- Log MDC collection is allowlist-based but must still be configured carefully.
- Debug mode may print sensitive telemetry to stdout.
- Dependency relocation for the final agent jar should be reviewed before production distribution.

## Handling Sensitive Data

Assume telemetry can contain sensitive values if the application places them in:

- URLs
- SQL statements
- log messages
- MDC
- headers
- exception messages

Before production use, configure collection scope carefully and review masking requirements for your environment.
