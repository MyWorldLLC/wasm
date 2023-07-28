(module
  (import "env" "importMe" (func $importMe (result i32)) )
  (export "callMe" (func $callMe))

  (func $callMe (result i32)
    call $importMe
  )
)