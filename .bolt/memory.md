# Project memory

## Product intent & audience

- Asset management application with maintenance tracking and notification features
- Server-side Java project using Maven (builds handled via Maven/Maven Wrapper, not available in current AI environment)
- Versions numbered (e.g., 1.3.117+); releases pushed to users who open screens directly

## Brand personality & voice

- User communicates in Russian; responses should be in Russian
- Technical, diagnostic tone — explain root cause and fix concisely

## Visual design & UX direction

- Asset list screen shows error state: «Не удалось загрузить данные» when server request fails
- Page-load reliability is critical; UI must not block on background maintenance operations

## Decisions, constraints & rationale

- **Asset list loading decoupled from maintenance processing**: reading assets no longer triggers automatic maintenance + email notifications inline. Rationale: a corrupted maintenance record caused the entire list request to fail, making the screen unusable.
- Maintenance still runs as a separate process but doesn't block page opening.
- Build verification via Maven not possible in current environment (Maven and Maven Wrapper absent).

## Open questions & assumptions

- No `.bolt/prompt` authoritative instructions exist yet
- Exact codebase structure, framework, and project name not yet identified
- Whether maintenance processing needs its own error handling/isolation to prevent future blocks
- Whether the corrupted maintenance record should be fixed at data level
- Build/CI pipeline details unknown