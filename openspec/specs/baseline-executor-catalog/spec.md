# baseline-executor-catalog

## ADDED Requirements

### Requirement: Catalog SHALL register and manage common executor presets

The system MUST provide a `BaselineExecutorCatalog` class that registers, stores, and retrieves `CommonExecutorPreset` records. The catalog MUST be immutable after construction.

#### Scenario: Catalog with defaults registers 6 presets
- **WHEN** `BaselineExecutorCatalog.withDefaults()` is called
- **THEN** a catalog is returned with exactly 6 presets: `fixed-2`, `fixed-4`, `fixed-8`, `cached`, `single`, `fixed-2-bounded`

#### Scenario: Get preset by ID returns correct configuration
- **WHEN** `catalog.get("fixed-4")` is called on a default catalog
- **THEN** a `CommonExecutorPreset` is returned with `corePoolSize=4`, `maxPoolSize=4`, `executorType="FIXED_THREAD_POOL"`, `queueCapacity=-1`

#### Scenario: Get nonexistent preset throws NoSuchElementException
- **WHEN** `catalog.get("nonexistent")` is called
- **THEN** `NoSuchElementException` is thrown

#### Scenario: Duplicate registration throws IllegalArgumentException
- **WHEN** `builder.register(preset)` is called twice with the same `presetId`
- **THEN** `IllegalArgumentException` is thrown

### Requirement: CommonExecutorPreset SHALL validate construction parameters

The `CommonExecutorPreset` record MUST validate all fields at construction time and reject invalid values.

#### Scenario: Valid construction accepts all fields
- **WHEN** `new CommonExecutorPreset("fixed-4", "FIXED_THREAD_POOL", 4, 4, -1, "description")` is called
- **THEN** the record is created successfully

#### Scenario: Blank presetId throws IllegalArgumentException
- **WHEN** `new CommonExecutorPreset("", ...)` is called with a blank `presetId`
- **THEN** `IllegalArgumentException` is thrown

#### Scenario: Invalid executorType throws IllegalArgumentException
- **WHEN** `new CommonExecutorPreset("test", "INVALID_TYPE", 1, 1, -1, null)` is called
- **THEN** `IllegalArgumentException` is thrown

#### Scenario: maxPoolSize less than corePoolSize throws IllegalArgumentException
- **WHEN** `new CommonExecutorPreset("test", "FIXED_THREAD_POOL", 4, 2, -1, null)` is called
- **THEN** `IllegalArgumentException` is thrown

#### Scenario: queueCapacity less than -1 throws IllegalArgumentException
- **WHEN** `new CommonExecutorPreset("test", "FIXED_THREAD_POOL", 1, 2, -2, null)` is called
- **THEN** `IllegalArgumentException` is thrown

### Requirement: CommonExecutorPreset SHALL convert to BaselineExecutorPreset

The `CommonExecutorPreset` MUST provide a `toBaselinePreset()` method that converts to `BaselineExecutorPreset` with the correct queueCapacity mapping.

#### Scenario: Unbounded queue maps to Integer.MAX_VALUE
- **WHEN** `toBaselinePreset()` is called on a preset with `queueCapacity=-1`
- **THEN** the resulting `BaselineExecutorPreset` has `queueCapacity=Integer.MAX_VALUE`

#### Scenario: SynchronousQueue maps to 0
- **WHEN** `toBaselinePreset()` is called on a preset with `queueCapacity=0`
- **THEN** the resulting `BaselineExecutorPreset` has `queueCapacity=0`

#### Scenario: Bounded queue maps directly
- **WHEN** `toBaselinePreset()` is called on a preset with `queueCapacity=10`
- **THEN** the resulting `BaselineExecutorPreset` has `queueCapacity=10`

### Requirement: CommonExecutorPreset SHALL support toMap and fromMap serialization

The `CommonExecutorPreset` record MUST provide `toMap()` returning a Map with keys presetId, executorType, corePoolSize, maxPoolSize, queueCapacity, and description (if non-null), and `fromMap(Map)` constructing an equivalent record.

#### Scenario: toMap includes description when non-null
- **WHEN** `toMap()` is called on a preset with `description="test"`
- **THEN** the returned map contains key `"description"` with value `"test"`

#### Scenario: toMap omits description when null
- **WHEN** `toMap()` is called on a preset with `description=null`
- **THEN** the returned map does not contain key `"description"`
