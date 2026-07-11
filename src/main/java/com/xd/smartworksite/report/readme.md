# Report Module Current Contract

This file records the current implementation contract of the `report` module. Historical phase notes were removed because they conflicted with the current asynchronous Worker flow.

## Current Boundary

- Java owns report orchestration, state records, file persistence, project isolation, and observable failures.
- CryptoAgentV3 owns the external report generation capability.
- `POST /api/reports` and `POST /api/reports/{reportId}/regenerate` create the report, config, task, and `task_outbox` event.
- HTTP creation endpoints do not call CryptoAgentV3 directly. The Worker claims `REPORT_GENERATION` tasks and calls CryptoAgentV3.
- If CryptoAgentV3 is unavailable, production code must mark the report as `FAILED` and record the error. It must not return fake success or fallback content.
- Unit tests may use fake clients to verify the contract without a real CryptoAgentV3 service.

## Current APIs

| Method | Path | Current behavior |
| --- | --- | --- |
| POST | `/api/reports` | Creates a report generation task and returns report status `PENDING` plus `taskId`. |
| GET | `/api/reports` | Lists reports in projects accessible to the current user. |
| GET | `/api/reports/{reportId}` | Gets report detail after project access validation. |
| POST | `/api/reports/{reportId}/regenerate` | Creates a new report and task from the original report config. |
| GET | `/api/reports/{reportId}/download?format=WORD` | Returns a MinIO access URL for the saved Word file, or downloads from `previewUrl` and saves it first. |

## State Flow

- After creation, report status is `PENDING`.
- After `task_outbox` is written, task status is `QUEUED`.
- After Worker claim, task status is `RUNNING` and report status is `PROCESSING`.
- If CryptoAgentV3 succeeds and returns a DOCX download reference, report status becomes `COMPLETED`.
- If CryptoAgentV3 fails, returns no DOCX, returns no download reference, or the downloaded file is empty, the error must be recorded and the report must become `FAILED`.

## CryptoAgentV3 Contract

- The request does not send a user-uploaded template file; CryptoAgentV3 uses its default report template.
- `templateVariables` comes from generation params, with `referenceDocuments` removed.
- At least one reference document is required.
- `generationParams.referenceDocuments` has priority. If it is absent, Java reads text files from `referenceFileIds`.
- Reference document file name and content must not be blank. Blank content fails fast.
- Only DOCX output is accepted. Missing DOCX output fails fast.

## Download Behavior

- Only `WORD` is supported.
- PDF requests fail explicitly. Word output must not be returned as fake PDF.
- If the current report version has `word_file_id`, the API returns a MinIO signed URL.
- If `word_file_id` is absent, the API downloads the DOCX from report `previewUrl`, stores it in MinIO and `file_object`, then returns the signed URL.
- Download failure, non-2xx HTTP response, empty file, or interruption must return an error. No fallback file is generated.

## Verification

Run after backend changes:

```powershell
mvn clean test
```

Current report module tests cover:

- Creating a `PENDING` report, `QUEUED` task, and `task_outbox` event.
- Worker success and failure paths for CryptoAgentV3 execution.
- Regeneration from original report config.
- Real Word-file persistence through the download path.
