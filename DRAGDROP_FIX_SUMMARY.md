# Drag-and-Drop Fix Summary

## Issue
The drag-and-drop functionality was broken with error: `Move rejected: HEADING_SELECTION` preventing tasks from being moved between hierarchy levels.

## Root Cause
In `TaskMovePlanner.java` line 145-146, the validation logic checked if a HEADING's parent was in the selected tasks, but did not account for cases where the parent ID could be null:

```java
if (value.type() == TaskType.HEADING && !selected.contains(value.parentId()))
    return Result.reject(Rejection.HEADING_SELECTION);
```

When `value.parentId()` was null, `!selected.contains(null)` would return true, causing false rejection.

## Fix Applied
Added null-safety check before calling contains():

```java
if (value.type() == TaskType.HEADING && value.parentId() != null && !selected.contains(value.parentId()))
    return Result.reject(Rejection.HEADING_SELECTION);
```

This ensures we only check parent containment when a parent actually exists.

## Test Coverage
Created comprehensive JUnit tests to verify all drag-drop scenarios:

### TaskMovePlannerTest (25 tests)
- Core move validation logic
- HEADING handling with parents
- Multi-level nesting constraints
- Task type preservation
- Children moved with parents

### TaskDragDropIntegrationTest (10 tests)
- End-to-end move operations
- Top-level to child conversion
- Child to top-level conversion
- Child reparenting
- Multiple task moves
- Cross-type moves (MORNING/EVENING)
- Cache invalidation

### TaskDropControllerIntentBuildingTest (10 tests)
- UI layer intent construction
- Drop position detection
- Selection validation
- Task existence validation
- Multiple task selection order preservation
- Custom checklist handling

## Test Results
✓ 45 drag-drop specific tests: **PASSING**
✓ 51 total with task manager integration: **PASSING**

## Scenarios Validated
1. ✓ Top-level task → child of another top-level task
2. ✓ Child task → top-level  
3. ✓ Child task → different parent
4. ✓ Multiple tasks moved together
5. ✓ Tasks reordered at same level
6. ✓ Cross-daily-type moves (MORNING ↔ EVENING)
7. ✓ Tasks with HEADING metadata preserved

## Files Modified
- `src/main/java/TaskMovePlanner.java` - Added null-safety check (1 line)
- `test/java/TaskMovePlannerTest.java` - Enhanced with 8 new tests
- `test/java/TaskDragDropIntegrationTest.java` - Created (10 tests)
- `test/java/TaskDropControllerIntentBuildingTest.java` - Created (10 tests)

## Backward Compatibility
✓ All existing tests continue to pass
✓ Fix is defensive - improves robustness without changing valid behavior
✓ No API changes or breaking changes
