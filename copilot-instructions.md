# Copilot Instructions

All new Java source files added to this repository MUST include the project's standard GPL license header at the top of the file.

Required header (copy exactly, updating year/author as appropriate):

/*
 * Daily Checklist
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

Placement: the header must appear as the first non-empty content in the file (before any `package` or `import` statements).

Notes for contributors and tools:
- Editors, CI checks, and code generation tools (including Copilot/assistants) should enforce or insert this header when creating new `.java` files.
- When updating the year or author, prefer the repository owner or the actual contributor's name.

If you want, I can add a CI check or a pre-commit hook to enforce presence of this header automatically.
