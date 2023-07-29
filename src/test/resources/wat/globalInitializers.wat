(module

  (global $importMe (import "env" "importMe") i32)
  (global $exportMe (mut i32)
    global.get $importMe
  )

  (export "importMe" (global $importMe))
  (export "exportMe" (global $exportMe))
)