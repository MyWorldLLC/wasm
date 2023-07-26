(module
  (export "add" (func $add))
  (func $add (result i32)
    i32.const 1
    i32.const 2

    i32.add
  )
)