(module
  (table 1 funcref)
  (type $indirectCall (func (result i32)))
  (export "callMe" (func $callMe))

  (func $callMe (result i32)
    i32.const 0
    call_indirect (type $indirectCall)
  )
)