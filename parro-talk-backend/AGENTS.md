# Backend Java Standards

## Clean Imports & Annotations
- **No Inline Fully Qualified Names (FQCN)**:
  - Do NOT write `@org.springframework...`, `java.util...`, or any inline fully-qualified package path in annotations, variable types, return types, or method parameters.
  - Always add the proper `import` statement at the top of the Java file.
  - Example: Use `@Primary` with `import org.springframework.context.annotation.Primary;`.
- **Import Hygiene**:
  - Keep imports explicit, organized, and free of unnecessary wildcard (`*`) imports.
  - Remove unused imports.
