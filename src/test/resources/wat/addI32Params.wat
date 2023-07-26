(module
  (export "add" (func $add))
  (func $add (param $l i32) (param $r i32) (result i32)
    local.get $l
    local.get $r

    i32.add
  )
)