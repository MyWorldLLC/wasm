(module

  (global $importMe (import "env" "importMe") (mut i32))
  (global $exportMe (mut i32) (i32.const 3))

  (export "callMe" (func $callMe))
  (export "exportMe" (global $exportMe))

  (func $callMe (result i32)
    global.get $exportMe
    global.get $importMe
    i32.add
  )
)