# OCR Module Agent Guide

This document describes the intended design for the `ocr` module. Read the root `README.md` and root `AGENTS.md` before editing this module.

## Module Positioning

The `ocr` module provides OCR business orchestration for the Smart Worksite Java backend.

It must support ID card recognition, license plate recognition, invoice recognition, and custom field extraction from uploaded contracts or other documents. The Java backend owns authentication, authorization, project isolation, file binding, OCR record persistence, async task status, result revision, and audit tracing.

The OCR recognition capability is implemented by Qwen VL, but Qwen VL must be called through `python-ai-service`. Java OCR code must not call `QWEN_VL_ENDPOINT` directly, must not hold Qwen API keys, and must not implement prompt or model-specific multimodal request logic in controllers or application services.

Recommended integration path:

```text
Frontend
  -> Java /api/ocr/**
    -> file module stores OCR_INPUT file in MinIO
    -> ocr module creates ocr_record and async task
    -> Java calls python-ai-service /v1/ocr/recognize
      -> python-ai-service downloads image URLs and converts them to base64 data URLs
      -> python-ai-service calls QWEN_VL_ENDPOINT
      -> python-ai-service normalizes OCR JSON
    -> Java persists fields_json, status, error_message, and external call log
```

## Responsibility Boundaries

Java `ocr` module responsibilities:

- Expose frontend-facing REST APIs under `/api/ocr/**`.
- Validate OCR type, project ID, file presence, invoice type, and custom field definitions.
- Use the `file` module for upload, metadata, and signed access URLs.
- Create and update OCR records in MySQL.
- Create or bind async task records and stage logs.
- Call Python OCR adapter with timeout, retry policy, error mapping, and call logging.
- Persist normalized structured fields, raw provider result summary, confidence, location, and error messages.
- Support manual field revision and optional revision history in a later phase.
- Enforce project-level data isolation and sensitive-field masking.

Python `python-ai-service` OCR responsibilities:

- Read Qwen VL configuration from its own `.env` or environment variables, such as `QWEN_VL_ENDPOINT`, `QWEN_VL_API_KEY`, `QWEN_VL_MODEL`, `QWEN_VL_MAX_TOKENS`, and timeout values.
- Build multimodal Qwen VL requests from image/document URLs or file content. For image inputs received as Java-generated MinIO temporary URLs, Python should download the URL first and send Qwen VL a `data:image/...;base64,...` image URL instead of forwarding the signed MinIO URL to the cloud provider.
- Maintain OCR prompts, field schemas, output JSON constraints, retry around model responses, and provider-specific parsing.
- Extract ID card, license plate, invoice, and custom contract fields.
- Normalize model responses into a stable JSON structure for Java.
- Return provider trace ID, usage, elapsed time, raw result summary, and error information.

Frontend responsibilities:

- Call only Java `/api/ocr/**` APIs.
- Display upload, status, progress, structured fields, confidence, location, raw JSON, and manual revision controls.
- Never call Python, Qwen VL, MinIO, database, or OCR engines directly.

## Recommended Package Structure

When implementing this module, use these layers:

```text
com.xd.smartworksite.ocr
├── controller
│   └── OcrController
├── application
│   ├── OcrApplicationService
│   └── OcrRecognitionWorker
├── domain
│   ├── OcrRecord
│   ├── OcrType
│   ├── OcrStatus
│   ├── OcrField
│   ├── OcrFieldDefinition
│   ├── InvoiceType
│   ├── PlateColor
│   └── WatermarkResult
├── dto
│   ├── OcrSubmitRequest
│   ├── OcrRecordQueryRequest
│   ├── OcrSubmitResponse
│   ├── OcrRecordResponse
│   ├── OcrFieldResponse
│   ├── OcrFieldUpdateRequest
│   └── OcrTypeResponse
├── repository
│   ├── OcrRepository
│   └── MyBatisOcrRepository
├── mapper
│   └── OcrMapper
└── infra
    ├── OcrPythonServiceClient
    ├── OcrProviderRequest
    ├── OcrProviderResponse
    └── OcrProviderProperties
```

Follow the project layering rules:

- Controllers handle HTTP input, Bean Validation, request context, and `ApiResponse` wrapping only.
- Controllers call only `OcrApplicationService` or a future OCR facade.
- Application services own orchestration, transaction boundaries, state transitions, and permission-sensitive business decisions.
- Domain objects express OCR types, status transitions, field normalization, and validation rules.
- Repositories expose business-facing persistence methods.
- Mappers only execute SQL and must filter `deleted = 0` by default.
- Infra code encapsulates calls to `python-ai-service`, not calls to Qwen VL.

## OCR Types

Use these public OCR types for the first implementation:

```text
ID_CARD        ID card fields and watermark detection
LICENSE_PLATE  License plate number, background color, font color, and plate type
INVOICE        VAT special invoice and VAT normal invoice fields
CUSTOM         User-defined fields, such as contract party A, party B, amount, and payment terms
```

`CONTRACT` may be kept as a backward-compatible alias only if existing frontend or mock data already depends on it. New contract extraction should use `CUSTOM` with field definitions.

Invoice subtype:

```text
VAT_SPECIAL    Value-added tax special invoice
VAT_NORMAL     Value-added tax normal invoice
```

License plate color values:

```text
BLUE
YELLOW
GREEN
GRADIENT_GREEN
WHITE
BLACK
OTHER
UNKNOWN
```

## Status Model

Use a domain enum for OCR record status:

```text
PENDING      Record created, waiting for execution
PROCESSING   Recognition is running
SUCCESS      Recognition completed
FAILED       Recognition failed
CANCELED     Recognition canceled
```

Allowed state transitions:

```text
PENDING -> PROCESSING -> SUCCESS
PENDING -> PROCESSING -> FAILED
PENDING -> CANCELED
PROCESSING -> CANCELED
FAILED -> PENDING       retry creates a new execution attempt
```

OCR recognition is a long-running operation. Do not block the HTTP submit request until model recognition finishes. Return `recordId`, `taskId`, and initial status, then let the frontend poll OCR detail and task stage logs.

## Storage Model

Current table:

```text
ocr_record
  id
  project_id
  ocr_type
  file_id
  task_id
  status
  fields_json
  custom_fields_json
  error_message
  created_at
  updated_at
  created_by
  updated_by
  deleted
```

Short-term implementation should reuse this table.

Recommended `fields_json` structure:

```json
{
  "summary": {
    "ocrType": "ID_CARD",
    "invoiceType": null,
    "confidence": 0.96,
    "provider": "QWEN_VL",
    "providerTraceId": "trace-id-from-python",
    "elapsedMs": 1420,
    "model": "qwen-vl-plus"
  },
  "fields": [
    {
      "fieldKey": "name",
      "fieldName": "姓名",
      "fieldValue": "张三",
      "confidence": 0.98,
      "location": "front.name",
      "pageNo": 1,
      "evidence": "姓名 张三",
      "revised": false
    }
  ],
  "extras": {},
  "raw": {}
}
```

Recommended `custom_fields_json` structure:

```json
[
  {
    "fieldKey": "partyA",
    "fieldName": "甲方",
    "description": "合同中的甲方名称",
    "required": true,
    "valueType": "TEXT"
  }
]
```

If field-level audit is required later, add a new Flyway migration instead of changing used migrations:

```text
ocr_field_revision
  id
  project_id
  record_id
  field_key
  field_name
  old_value
  new_value
  revised_by
  revised_at
  created_at
  updated_at
  created_by
  updated_by
  deleted
```

## Frontend-Facing REST APIs

Recommended endpoints:

```text
POST   /api/ocr/records
GET    /api/ocr/records
GET    /api/ocr/records/{recordId}
PUT    /api/ocr/records/{recordId}/fields
POST   /api/ocr/records/{recordId}/retry
DELETE /api/ocr/records/{recordId}
GET    /api/ocr/records/{recordId}/download
GET    /api/ocr/types
```

Submit request uses `multipart/form-data`:

```text
projectId     Project ID, required
ocrType       ID_CARD / LICENSE_PLATE / INVOICE / CUSTOM, required
file          Source image or document, required
invoiceType   VAT_SPECIAL / VAT_NORMAL, required when ocrType = INVOICE
customFields  JSON array of field definitions, required when ocrType = CUSTOM
```

Submit response:

```json
{
  "recordId": 11001,
  "taskId": 9601,
  "status": "PENDING"
}
```

Detail response should include at least:

```json
{
  "recordId": 11001,
  "projectId": 1001,
  "taskId": 9601,
  "fileId": 4401,
  "ocrType": "ID_CARD",
  "status": "SUCCESS",
  "progress": 100,
  "fields": [],
  "rawResult": {},
  "createdAt": "2026-07-10T10:00:00+08:00",
  "updatedAt": "2026-07-10T10:01:00+08:00"
}
```

Manual field revision request:

```json
{
  "fields": [
    {
      "fieldKey": "name",
      "fieldName": "姓名",
      "fieldValue": "张三",
      "confidence": 0.98,
      "location": "front.name"
    }
  ]
}
```

## Java To Python OCR API

The Java OCR module should call `python-ai-service`, not Qwen VL.

Recommended internal API:

```text
POST /v1/ocr/recognize
Header: X-AI-Service-Key
Content-Type: application/json
```

Request:

```json
{
  "projectId": 1001,
  "recordId": 11001,
  "ocrType": "CUSTOM",
  "file": {
    "fileId": 4401,
    "fileName": "contract.pdf",
    "contentType": "application/pdf",
    "downloadUrl": "temporary-internal-signed-url"
  },
  "options": {
    "invoiceType": null,
    "customFields": [
      {
        "fieldKey": "partyA",
        "fieldName": "甲方",
        "description": "合同中的甲方名称",
        "required": true,
        "valueType": "TEXT"
      }
    ]
  }
}
```

Response:

```json
{
  "success": true,
  "traceId": "python-trace-id",
  "data": {
    "ocrType": "CUSTOM",
    "confidence": 0.91,
    "fields": [],
    "extras": {},
    "raw": {}
  },
  "usage": {
    "provider": "QWEN_VL",
    "model": "qwen-vl-plus",
    "elapsedMs": 1420
  },
  "errorCode": null,
  "errorMessage": null
}
```

Java must persist `traceId` and useful `usage` values inside `fields_json.summary` or the external call log.

## Qwen VL OCR Design In Python

`python-ai-service` should hide provider-specific request details from Java.

Qwen VL call inputs:

- Uploaded file content, temporary signed URL, or base64 image. For image OCR, prefer converting temporary signed URLs to base64 data URLs inside Python before calling Qwen VL.
- OCR type.
- Invoice subtype when applicable.
- Custom field definitions when applicable.
- Strict JSON output schema.

Qwen VL prompt requirements:

- Require JSON-only output.
- Require every field to include `fieldKey`, `fieldName`, `fieldValue`, `confidence`, `location`, and optional `evidence`.
- Require unknown or unreadable values to be returned as empty strings with low confidence, not hallucinated values.
- Require sensitive identifiers to be extracted for business use but masked in logs.
- Require model to include `extras` for type-specific structured information.

Python should validate model output before returning to Java:

- JSON must parse successfully.
- `fields` must be an array.
- `confidence` must be numeric and between 0 and 1.
- Required standard fields should exist, even when empty.
- Provider raw response should be summarized before returning or logging.

## ID Card Recognition

`ocrType = ID_CARD`

Required fields:

```text
name               姓名
gender             性别
nation             民族
birthDate          出生日期
address            住址
idNumber           身份证号
issuingAuthority   签发机关
validPeriod        有效期限
hasWatermark       是否有水印
```

Recommended extras:

```json
{
  "watermark": {
    "detected": true,
    "type": "TEXT",
    "text": "仅用于办理入职",
    "confidence": 0.88,
    "regions": []
  },
  "sideDetected": ["FRONT", "BACK"]
}
```

Sensitive fields such as `idNumber` and `address` must be masked in logs and response summaries. If product requirements need frontend masking, provide both masked display value and controlled raw value only after authorization.

## License Plate Recognition

`ocrType = LICENSE_PLATE`

Required fields:

```text
plateNumber       车牌号
backgroundColor   底色
fontColor         字号颜色
plateType         车牌类型
```

Recommended extras:

```json
{
  "plate": {
    "number": "浙A12345",
    "backgroundColor": "BLUE",
    "fontColor": "WHITE",
    "plateType": "SMALL_VEHICLE",
    "bbox": [100, 80, 320, 150]
  }
}
```

Qwen VL is acceptable for the first implementation, especially for clear uploaded images. For high-volume gate camera scenarios or low-quality night images, consider a specialized plate recognition model behind the same Python OCR API without changing Java contracts.

## Invoice Recognition

`ocrType = INVOICE`

First phase supports:

```text
VAT_SPECIAL
VAT_NORMAL
```

Required fields:

```text
invoiceType        发票类型
invoiceCode        发票代码
invoiceNumber      发票号码
issueDate          开票日期
buyerName          购买方名称
buyerTaxNumber     购买方纳税人识别号
sellerName         销售方名称
sellerTaxNumber    销售方纳税人识别号
amountWithoutTax   不含税金额
taxAmount          税额
totalAmount        价税合计
```

Recommended extras:

```json
{
  "items": [
    {
      "name": "钢筋",
      "spec": "HRB400",
      "unit": "吨",
      "quantity": "10",
      "unitPrice": "5000.00",
      "amount": "50000.00",
      "taxRate": "13%",
      "taxAmount": "6500.00"
    }
  ],
  "validation": {
    "amountCheckPassed": true,
    "warnings": []
  }
}
```

Java may perform lightweight validation after Python returns results, such as checking whether `totalAmount = amountWithoutTax + taxAmount` when all three values are numeric.

## Custom OCR And Contract Field Extraction

`ocrType = CUSTOM`

Use custom OCR for contracts and other non-fixed-format documents. The user supplies field definitions, and Python uses document parsing, OCR, and Qwen VL understanding to extract values.

Example custom fields:

```json
[
  {
    "fieldKey": "partyA",
    "fieldName": "甲方",
    "description": "合同中的甲方名称",
    "required": true,
    "valueType": "TEXT"
  },
  {
    "fieldKey": "partyB",
    "fieldName": "乙方",
    "description": "合同中的乙方名称",
    "required": true,
    "valueType": "TEXT"
  },
  {
    "fieldKey": "contractAmount",
    "fieldName": "合同金额",
    "description": "合同总金额，保留币种和大小写金额",
    "required": true,
    "valueType": "AMOUNT"
  },
  {
    "fieldKey": "paymentTerms",
    "fieldName": "付款条件",
    "description": "付款节点、比例、触发条件和期限",
    "required": false,
    "valueType": "TEXT"
  }
]
```

Expected fields include evidence:

```json
{
  "fieldKey": "partyA",
  "fieldName": "甲方",
  "fieldValue": "杭州某某建设有限公司",
  "confidence": 0.91,
  "location": "第1页 第3段",
  "evidence": "甲方：杭州某某建设有限公司"
}
```

Custom OCR should not be implemented as plain text OCR only. For PDF, Word, or scanned contracts, Python should combine document parsing, page image OCR, layout understanding, and Qwen VL field extraction as needed.

## Upload And Recognition Flow

Submit flow:

```text
OcrController
  -> OcrApplicationService.submit()
    -> validate projectId, ocrType, file, invoiceType, customFields
    -> upload source file through file module with bizType = OCR_INPUT
    -> create ocr_record with status = PENDING
    -> create or bind async task and initial stage log
    -> dispatch OcrRecognitionWorker
    -> return recordId, taskId, status
```

Worker flow:

```text
OcrRecognitionWorker
  -> set ocr_record status = PROCESSING
  -> update task stage OCR_RECOGNITION = RUNNING
  -> request signed internal file URL from file module
  -> call python-ai-service /v1/ocr/recognize
  -> normalize and persist fields_json
  -> set ocr_record status = SUCCESS
  -> update task progress and stage logs
```

Failure flow:

```text
Any validation or provider error
  -> map to BusinessException or provider error
  -> set ocr_record status = FAILED
  -> persist error_message with a user-safe summary
  -> update task stage log
  -> record external call log
```

## External Call Logging

Every Python OCR call must record:

```text
serviceName       PYTHON_AI_SERVICE
callType          OCR_RECOGNIZE
projectId
requestId
requestSummary
responseSummary
status            SUCCESS / FAILED
costMs
errorMessage
```

Request and response summaries must mask:

- API keys, tokens, passwords, and service keys.
- ID card numbers.
- Phone numbers.
- Detailed addresses when not required for diagnosis.
- Long contract content and raw OCR text.

Do not log file binary content, base64 images, MinIO credentials, permanent URLs, or production endpoints.

## Security And Data Isolation

- Every OCR record must include `project_id`.
- Every query must filter by project permission and `deleted = 0`.
- File access must be checked before generating signed URLs.
- OCR result download must check project permission before returning content.
- Frontend must receive only fields the current user is allowed to view.
- Sensitive fields should be masked where product requirements demand display masking.
- Qwen VL API keys must stay in `python-ai-service/.env` or runtime environment variables.

## Testing Guidance

After adding runnable OCR functionality, run `mvn test`.

Recommended Java tests:

- Submit validation for missing file, invalid OCR type, missing invoice type, and invalid custom fields.
- Status transitions for success and failure.
- Field JSON persistence and manual revision.
- Python client timeout, failed provider response, and malformed provider data.
- Project isolation for detail, list, update, retry, delete, and download.

Recommended Python tests:

- `/v1/ocr/recognize` request validation.
- Qwen VL response parsing and JSON repair behavior.
- Standard schemas for ID card, license plate, invoice, and custom OCR.
- Error mapping when Qwen VL times out or returns non-JSON content.
- Sensitive data masking in logs.
