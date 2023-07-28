(module
  (export "callMe" (func $callMe))
  (func $add (result i32)
    i32.const 1
    i32.const 2

    i32.add
  )

  (func $callMe (result i32)
    call $add
  )
)