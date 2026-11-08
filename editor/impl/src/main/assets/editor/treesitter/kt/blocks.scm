; Code block patterns for sora-editor (Kotlin)
; Those changes reflect the changes of nullij editor fork.
; Capture names with '.marked' suffix set the block end to the start of the last terminal child.

(class_declaration
  (class_body) @scope.marked)

(object_declaration
  (class_body) @scope.marked)

(companion_object
  (class_body) @scope.marked)

(enum_class_body) @scope.marked

(function_declaration
  (function_body) @scope.marked)

(property_declaration
  (getter
    (function_body) @scope.marked))

(property_declaration
  (setter
    (function_body) @scope.marked))

(control_structure_body) @scope.marked

; In Tree-sitter Kotlin, 'statements' is the child node inside function_body and control_structure_body.
; It starts on the first statement line (such as super.onCreate) rather than the function header line.
; Function and block bodies are already captured by function_declaration and control_structure_body.
; Commented out to prevent duplicate fold icons on inner statement lines.
; Those changes reflect the changes of nullij editor fork.
; (statements) @scope.marked

(lambda_literal) @scope.marked

(when_expression) @scope.marked
