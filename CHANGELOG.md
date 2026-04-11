# Changelog

## [Unreleased]

## [1.2.0] - 2026-04-11

### Added
- Added `CopyContext.isRoot()` to check whether the current object being copied is the root of the copy operation.

### Changed
- Removed the `isRoot` flag from `CopyCallback`, since this can now be determined via `CopyContext`.

## [1.1.0] - 2026-03-31

### Added
- Added `CopyContext.exclude(Object)` and `CopyContext.include(Object)` to temporarily exclude objects from being
  copied without registering them in the session's clone map.

## [1.0.0] - 2026-03-26
This is the initial release of object-copier.

[Unreleased]: https://github.com/xmlobjects/object-copier/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/xmlobjects/object-copier/releases/tag/v1.2.0
[1.1.0]: https://github.com/xmlobjects/object-copier/releases/tag/v1.1.0
[1.0.0]: https://github.com/xmlobjects/object-copier/releases/tag/v1.0.0